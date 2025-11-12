package io.toolsplus.atlassian.connect.play.auth.jwt.symmetric

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.DefaultAtlassianHostUser
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.auth.jwt._
import io.toolsplus.atlassian.jwt.generators.core.CanonicalHttpRequestGen
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import org.scalacheck.Gen._
import org.scalacheck.Shrink
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration

import scala.concurrent.Future

class SymmetricJwtAuthenticationProviderSpec
    extends TestSpec
    with GuiceOneAppPerSuite
    with CanonicalHttpRequestGen {

  val config: Configuration = app.configuration

  val hostRepository: AtlassianHostRepository = mock[AtlassianHostRepository]

  val jwtAuthenticationProvider =
    new SymmetricJwtAuthenticationProvider(hostRepository)

  "A SymmetricJwtAuthenticationProvider" when {

    "asked to authenticate any invalid credentials" should {

      "fail if credentials cannot be parsed" in {
        forAll(symmetricJwtCredentialsGen()) { credentials =>
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

    }

    "asked to authenticate any valid credentials" should {

      "fail if host has never been installed and could not be found" in {
        forAll(atlassianHostGen) { aHost =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          val customClaims = Seq("iss" -> host.clientKey)
          forAll(
            symmetricJwtCredentialsGen(secret = host.sharedSecret, customClaims)
          ) { credentials =>
            (hostRepository
              .findByClientKey(
                _: ClientKey
              )) expects host.clientKey returning Future
              .successful(None)

            val result = await {
              jwtAuthenticationProvider
                .authenticate(credentials, "fake-qsh")
                .value
            }
            result mustBe Left(UnknownJwtIssuerError(host.clientKey))
          }
        }
      }

    }

    "asked to authenticate Atlassian authenticated credentials" should {

      "fail if authenticated credentials' issuer does not exist" in {
        forAll(atlassianHostGen) { aHost =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          val customClaims = Seq("iss" -> null)
          forAll(
            symmetricJwtCredentialsGen(secret = host.sharedSecret, customClaims)
          ) { credentials =>
            val result = await {
              jwtAuthenticationProvider
                .authenticate(credentials, "fake-qsh")
                .value
            }
            val expectedMessage =
              "Failed to extract client key due to missing issuer claim"
            result mustBe Left(JwtBadCredentialsError(expectedMessage))
          }
        }
      }

      "fail if credentials' signature is not valid" in {
        forAll(atlassianHostGen) { aHost =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          val customClaims = Seq("iss" -> host.clientKey)
          forAll(
            symmetricJwtCredentialsGen(secret = host.sharedSecret, customClaims)
          ) { credentials =>
            val nCharsFromSignature = 4
            val invalidSignatureJwt =
              credentials.rawJwt.dropRight(nCharsFromSignature)
            val invalidSignatureCredentials =
              credentials.copy(rawJwt = invalidSignatureJwt)

            (hostRepository
              .findByClientKey(
                _: ClientKey
              )) expects host.clientKey returning Future
              .successful(Some(host))

            val result = await {
              jwtAuthenticationProvider
                .authenticate(invalidSignatureCredentials, "fake-qsh")
                .value
            }
            result mustBe Left(
              InvalidJwtError(invalidSignatureCredentials.rawJwt)
            )
          }
        }
      }

      "successfully authenticate valid credentials" in {
        implicit val stringNoShrink: Shrink[String] =
          Shrink.shrinkAny
        forAll(atlassianHostGen, alphaStr) { (aHost, subject) =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          val customClaims = Seq("iss" -> host.clientKey, "sub" -> subject)
          forAll(
            symmetricJwtCredentialsGen(secret = host.sharedSecret, customClaims)
          ) { credentials =>
            (hostRepository
              .findByClientKey(
                _: ClientKey
              )) expects host.clientKey returning Future
              .successful(Some(host))

            val result = await {
              jwtAuthenticationProvider
                .authenticate(credentials, "fake-qsh")
                .value
            }
            result mustBe Right(DefaultAtlassianHostUser(host, Option(subject)))
          }
        }
      }

      "successfully authenticate credentials without context claim (as after GDPR migration)" in {
        implicit val stringNoShrink: Shrink[String] =
          Shrink.shrinkAny
        forAll(atlassianHostGen, alphaStr) { (aHost, userAccountId) =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          val customClaims =
            Seq("iss" -> host.clientKey, "sub" -> userAccountId)
          forAll(
            symmetricJwtCredentialsGen(secret = host.sharedSecret, customClaims)
          ) { credentials =>
            (hostRepository
              .findByClientKey(
                _: ClientKey
              )) expects host.clientKey returning Future
              .successful(Some(host))

            val result = await {
              jwtAuthenticationProvider
                .authenticate(credentials, "fake-qsh")
                .value
            }
            result mustBe Right(
              DefaultAtlassianHostUser(host, Some(userAccountId))
            )
          }
        }
      }

      "successfully authenticate credentials with a with HTTP request QSH claim" in {
        implicit val stringNoShrink: Shrink[String] =
          Shrink.shrinkAny
        forAll(atlassianHostGen, alphaStr, canonicalHttpRequestGen) {
          (aHost, userAccountId, canonicalHttpRequest) =>
            val host = {
              aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
            }
            val qsh = CanonicalHttpRequestQshProvider.qsh(canonicalHttpRequest)
            val customClaims =
              Seq("iss" -> host.clientKey, "sub" -> userAccountId, "qsh" -> qsh)
            forAll(
              symmetricJwtCredentialsGen(
                secret = host.sharedSecret,
                customClaims
              )
            ) { credentials =>
              (hostRepository
                .findByClientKey(
                  _: ClientKey
                )) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                jwtAuthenticationProvider.authenticate(credentials, qsh).value
              }
              result mustBe Right(
                DefaultAtlassianHostUser(host, Some(userAccountId))
              )
            }
        }
      }

      "successfully authenticate credentials with a context QSH claim" in {
        implicit val stringNoShrink: Shrink[String] =
          Shrink.shrinkAny
        forAll(atlassianHostGen, alphaStr) { (aHost, userAccountId) =>
          val host = {
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          }
          val qsh = ContextQshProvider.qsh
          val customClaims =
            Seq("iss" -> host.clientKey, "sub" -> userAccountId, "qsh" -> qsh)
          forAll(
            symmetricJwtCredentialsGen(secret = host.sharedSecret, customClaims)
          ) { credentials =>
            (hostRepository
              .findByClientKey(
                _: ClientKey
              )) expects host.clientKey returning Future
              .successful(Some(host))

            val result = await {
              jwtAuthenticationProvider.authenticate(credentials, qsh).value
            }
            result mustBe Right(
              DefaultAtlassianHostUser(host, Some(userAccountId))
            )
          }
        }
      }
    }
  }
}
