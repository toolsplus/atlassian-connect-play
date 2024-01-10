package io.toolsplus.atlassian.connect.play.auth.jwt.asymmetric

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.DefaultAtlassianHostUser
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.auth.jwt.{InvalidJwtError, JwtBadCredentialsError, UnknownJwtIssuerError}
import io.toolsplus.atlassian.connect.play.models.PlayAddonProperties
import io.toolsplus.atlassian.jwt.generators.core.CanonicalHttpRequestGen
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import org.scalacheck.Gen._
import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration

import java.security.interfaces.RSAPublicKey
import scala.concurrent.Future

class AsymmetricJwtAuthenticationProviderSpec
    extends TestSpec
    with EitherValues
    with GuiceOneAppPerSuite
    with CanonicalHttpRequestGen {

  val config: Configuration = app.configuration

  private val appBaseUrl: String = "https://intercom-for-jira.toolsplus.app"

  val appProperties = new PlayAddonProperties(
    Configuration("addon.baseUrl" -> appBaseUrl).withFallback(config))

  val publicKeyProvider: PublicKeyProvider = mock[PublicKeyProvider]
  val hostRepository: AtlassianHostRepository = mock[AtlassianHostRepository]

  val jwtAuthenticationProvider =
    new AsymmetricJwtAuthenticationProvider(appProperties,
                                            publicKeyProvider,
                                            hostRepository)

  private val keyId: String = "0e50fccb-239d-4991-a5db-dc850ba3f236"
  private val keyPair = JwtTestHelper.generateKeyPair()
  private val publicKey = keyPair.getPublic.asInstanceOf[RSAPublicKey]
  private val privateKey = keyPair.getPrivate

  "A AsymmetricJwtAuthenticationProvider" when {

    "authenticating JWT credentials" should {

      "fail if credentials cannot be parsed" in {
        forAll(asymmetricJwtCredentialsGen(keyId, privateKey, Seq.empty)) { credentials =>
          val result = await {
            jwtAuthenticationProvider
              .authenticate(credentials.copy(rawJwt = "bogus"), "fake-qsh")
              .value
          }
          val expectedParseExceptionMessage =
            "Invalid serialized unsecured/JWS/JWE object: Missing part delimiters"
          result mustBe Left(InvalidJwtError(expectedParseExceptionMessage))
        }
      }

      "fail if JWT does not contain a key id ('kid') in the token header" in {
        forAll(symmetricJwtCredentialsGen()) { credentials =>
          val result = await {
            jwtAuthenticationProvider
              .authenticate(credentials, "fake-qsh")
              .value
          }
          val expectedParseExceptionMessage =
            "Missing key id (kid) in token header"
          result mustBe Left(
            JwtBadCredentialsError(expectedParseExceptionMessage))
        }
      }

      "fail if public key provider fails to fetch the public key" in {
        forAll(asymmetricJwtCredentialsGen(keyId, privateKey, Seq.empty)) {
          credentials =>
            (publicKeyProvider
              .fetchPublicKey(_: String)) expects keyId returning Future
              .successful(Left(InvalidJwtError("")))

            val result = await {
              jwtAuthenticationProvider
                .authenticate(credentials, "fake-qsh")
                .value
            }
            result.left.value mustBe a[InvalidJwtError]
        }
      }

      "fail if public key cannot to be parsed" in {
        forAll(asymmetricJwtCredentialsGen(keyId, privateKey, Seq.empty)) {
          credentials =>
            (publicKeyProvider
              .fetchPublicKey(_: String)) expects keyId returning Future
              .successful(Right("fake-public-key"))

            val result = await {
              jwtAuthenticationProvider
                .authenticate(credentials, "fake-qsh")
                .value
            }
            result.left.value mustBe JwtBadCredentialsError(
              "Failed to read PEM encoded public key")
        }
      }

      "fail if JWT verification fails" in {
        forAll(asymmetricJwtCredentialsGen(keyId, privateKey, Seq.empty)) {
          credentials =>
            (publicKeyProvider
              .fetchPublicKey(_: String)) expects keyId returning Future
              .successful(Right(JwtTestHelper.toPemString(publicKey)))

            val result = await {
              jwtAuthenticationProvider
                .authenticate(credentials, "fake-qsh")
                .value
            }
            result.left.value mustBe a[InvalidJwtError]
        }
      }

      "fail if JWT has no issuer claim" in {
        forAll(
          asymmetricJwtCredentialsGen(
            keyId,
            privateKey,
            Seq("aud" -> appBaseUrl, "iss" -> null))) { credentials =>
          (publicKeyProvider
            .fetchPublicKey(_: String)) expects keyId returning Future
            .successful(Right(JwtTestHelper.toPemString(publicKey)))

          val result = await {
            jwtAuthenticationProvider
              .authenticate(credentials, "fake-qsh")
              .value
          }

          result.left.value mustBe JwtBadCredentialsError(
            "Failed to extract client key due to missing issuer claim")
        }
      }

      "succeed if there is no Atlassian host for the JWT issuer claim value (client key)" in {
        val issuerClaim = "fake-issuer-claim"
        forAll(
          asymmetricJwtCredentialsGen(keyId,
                                      privateKey,
                                      Seq("aud" -> appBaseUrl,
                                          "iss" -> issuerClaim))) {
          credentials =>
            (publicKeyProvider
              .fetchPublicKey(_: String)) expects keyId returning Future
              .successful(Right(JwtTestHelper.toPemString(publicKey)))

            (hostRepository
              .findByClientKey(_: ClientKey)) expects issuerClaim returning Future
              .successful(None)

            val result = await {
              jwtAuthenticationProvider
                .authenticate(credentials, "fake-qsh")
                .value
            }

            result.value mustBe None
        }
      }

      "successfully authenticate valid credentials" in {
        forAll(connectAtlassianHostGen, alphaStr) { (aHost, subject) =>
          forAll(
            asymmetricJwtCredentialsGen(keyId,
                                        privateKey,
                                        Seq("aud" -> appBaseUrl,
                                            "iss" -> aHost.clientKey,
                                            "sub" -> subject))) { credentials =>
            (publicKeyProvider
              .fetchPublicKey(_: String)) expects keyId returning Future
              .successful(Right(JwtTestHelper.toPemString(publicKey)))

            (hostRepository
              .findByClientKey(_: ClientKey)) expects aHost.clientKey returning Future
              .successful(Some(aHost))

            val result = await {
              jwtAuthenticationProvider
                .authenticate(credentials, "fake-qsh")
                .value
            }

            result.value mustBe Some(
              DefaultAtlassianHostUser(aHost, Option(subject)))
          }
        }
      }
    }
  }
}
