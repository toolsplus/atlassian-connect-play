package io.toolsplus.atlassian.connect.play.auth.jwt

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHostUser
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.models.AddonProperties
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import org.scalacheck.Gen._
import org.scalacheck.Shrink
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.collection.JavaConverters._
import scala.concurrent.Future

class JwtAuthenticationProviderSpec extends TestSpec with GuiceOneAppPerSuite {

  val config = app.configuration

  val hostRepository = mock[AtlassianHostRepository]
  val addonProperties = new AddonProperties(config)

  val jwtAuthenticationProvider =
    new JwtAuthenticationProvider(hostRepository, addonProperties)

  "A JwtAuthenticationProvider" when {

    "asked to authenticate any invalid credentials" should {

      "fail if credentials cannot be parsed" in {
        forAll(jwtCredentialsGen()) { credentials =>
          val result = await {
            jwtAuthenticationProvider
              .authenticate(credentials.copy(rawJwt = "bogus"))
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
          implicit val stringNoShrink = Shrink[String](_ => Stream.empty)
          val customClaims = Seq("iss" -> host.clientKey)
          forAll(jwtCredentialsGen(secret = host.sharedSecret, customClaims)) {
            credentials =>
              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(None)

              val result = await {
                jwtAuthenticationProvider.authenticate(credentials).value
              }
              result mustBe Left(UnknownJwtIssuerError(host.clientKey))
          }
        }
      }

    }

    "asked to authenticate Atlassian authenticated credentials" should {

      "fail if authenticated credentials' issuer does not exist" in {
        implicit val stringNoShrink = Shrink[String](_ => Stream.empty)
        forAll(atlassianHostGen) { aHost =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          val customClaims = Seq("iss" -> null)
          forAll(jwtCredentialsGen(secret = host.sharedSecret, customClaims)) {
            credentials =>
              val result = await {
                jwtAuthenticationProvider.authenticate(credentials).value
              }
              val expectedMessage =
                "Missing client key claim for Atlassian token"
              result mustBe Left(JwtBadCredentialsError(expectedMessage))
          }
        }
      }

      "fail if credentials' signature is not valid" in {
        implicit val stringNoShrink = Shrink[String](_ => Stream.empty)
        forAll(atlassianHostGen) { aHost =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          val customClaims = Seq("iss" -> host.clientKey)
          forAll(jwtCredentialsGen(secret = host.sharedSecret, customClaims)) {
            credentials =>
              val nCharsFromSignature = 4
              val invalidSignatureJwt =
                credentials.rawJwt.dropRight(nCharsFromSignature)
              val invalidSignatureCredentials =
                credentials.copy(rawJwt = invalidSignatureJwt)

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                jwtAuthenticationProvider
                  .authenticate(invalidSignatureCredentials)
                  .value
              }
              result mustBe Left(
                InvalidJwtError(invalidSignatureCredentials.rawJwt))
          }
        }
      }

      "successfully authenticate valid credentials" in {
        implicit val stringNoShrink = Shrink[String](_ => Stream.empty)
        forAll(atlassianHostGen, alphaStr) { (aHost, subject) =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          val customClaims = Seq("iss" -> host.clientKey, "sub" -> subject)
          forAll(jwtCredentialsGen(secret = host.sharedSecret, customClaims)) {
            credentials =>
              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                jwtAuthenticationProvider.authenticate(credentials).value
              }
              result mustBe Right(AtlassianHostUser(host, Option(subject)))
          }
        }
      }

    }

    "asked to authenticate self-authenticated credentials" should {

      "fail if credentials' audience is not present" in {
        forAll(atlassianHostGen) { aHost =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          implicit val stringNoShrink = Shrink[String](_ => Stream.empty)
          val customClaims = Seq("aud" -> null)
          forAll(
            selfAuthenticatedJwtCredentialsGen(addonProperties.key,
                                               host,
                                               customClaims)) { credentials =>
            val result = await {
              jwtAuthenticationProvider.authenticate(credentials).value
            }
            val expectedMessage =
              "Missing audience for self-authentication token"
            result mustBe Left(JwtBadCredentialsError(expectedMessage))
          }
        }
      }

      "fail if credentials' audience does not match add-on key" in {
        forAll(atlassianHostGen, alphaStr) { (aHost, someAudience) =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          implicit val stringNoShrink = Shrink[String](_ => Stream.empty)
          val customClaims = Seq("aud" -> Seq(someAudience).asJava)
          forAll(
            selfAuthenticatedJwtCredentialsGen(addonProperties.key,
                                               host,
                                               customClaims)) { credentials =>
            val result = await {
              jwtAuthenticationProvider.authenticate(credentials).value
            }
            val expectedMessage =
              s"Invalid audience ($someAudience) for self-authentication token"
            result mustBe Left(JwtBadCredentialsError(expectedMessage))
          }
        }
      }

      "fail if credentials do not contain 'clientKey' claim" in {
        implicit val stringNoShrink = Shrink[String](_ => Stream.empty)
        forAll(atlassianHostGen) { aHost =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          val claimsWithoutClientKeyClaim =
            Seq("iss" -> addonProperties.key,
                "aud" -> Seq(addonProperties.key).asJava)
          forAll(
            jwtCredentialsGen(host.sharedSecret, claimsWithoutClientKeyClaim)) {
            credentials =>
              val result = await {
                jwtAuthenticationProvider.authenticate(credentials).value
              }
              val expectedMessage =
                "Missing client key claim for self-authentication token"
              result mustBe Left(JwtBadCredentialsError(expectedMessage))
          }
        }
      }

      "successfully authenticate valid credentials" in {
        implicit val stringNoShrink = Shrink[String](_ => Stream.empty)
        forAll(atlassianHostGen, alphaStr) { (aHost, subject) =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          val customClaims = Seq("sub" -> subject)
          forAll(
            selfAuthenticatedJwtCredentialsGen(addonProperties.key,
                                               host,
                                               customClaims)) { credentials =>
            (hostRepository
              .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
              .successful(Some(host))

            val result = await {
              jwtAuthenticationProvider.authenticate(credentials).value
            }
            result mustBe Right(AtlassianHostUser(host, Option(subject)))
          }
        }
      }

    }

  }

}
