package io.toolsplus.atlassian.connect.play.auth.jwt

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.DefaultAtlassianHostUser
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.jwt.generators.core.CanonicalHttpRequestGen
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import org.scalacheck.Gen._
import org.scalacheck.Shrink
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class JwtAuthenticationProviderSpec extends TestSpec with GuiceOneAppPerSuite with  CanonicalHttpRequestGen {

  val config: Configuration = app.configuration

  val hostRepository: AtlassianHostRepository = mock[AtlassianHostRepository]

  val jwtAuthenticationProvider =
    new JwtAuthenticationProvider(hostRepository)

  "A JwtAuthenticationProvider" when {

    "asked to authenticate any invalid credentials" should {

      "fail if credentials cannot be parsed" in {
        forAll(jwtCredentialsGen()) { credentials =>
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
          implicit val stringNoShrink: Shrink[String] =
            Shrink[String](_ => Stream.empty)
          val customClaims = Seq("iss" -> host.clientKey)
          forAll(jwtCredentialsGen(secret = host.sharedSecret, customClaims)) {
            credentials =>
              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(None)

              val result = await {
                jwtAuthenticationProvider.authenticate(credentials, "fake-qsh").value
              }
              result mustBe Left(UnknownJwtIssuerError(host.clientKey))
          }
        }
      }

    }

    "asked to authenticate Atlassian authenticated credentials" should {

      "fail if authenticated credentials' issuer does not exist" in {
        implicit val stringNoShrink: Shrink[String] =
          Shrink[String](_ => Stream.empty)
        forAll(atlassianHostGen) { aHost =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          val customClaims = Seq("iss" -> null)
          forAll(jwtCredentialsGen(secret = host.sharedSecret, customClaims)) {
            credentials =>
              val result = await {
                jwtAuthenticationProvider.authenticate(credentials, "fake-qsh").value
              }
              val expectedMessage =
                "Missing client key claim for Atlassian token"
              result mustBe Left(JwtBadCredentialsError(expectedMessage))
          }
        }
      }

      "fail if credentials' signature is not valid" in {
        implicit val stringNoShrink: Shrink[String] =
          Shrink[String](_ => Stream.empty)
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
                  .authenticate(invalidSignatureCredentials, "fake-qsh")
                  .value
              }
              result mustBe Left(
                InvalidJwtError(invalidSignatureCredentials.rawJwt))
          }
        }
      }

      "successfully authenticate valid credentials" in {
        implicit val stringNoShrink: Shrink[String] =
          Shrink[String](_ => Stream.empty)
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
                jwtAuthenticationProvider.authenticate(credentials, "fake-qsh").value
              }
              result mustBe Right(
                DefaultAtlassianHostUser(host, None, Option(subject)))
          }
        }
      }

      "successfully authenticate credentials without context claim (as after GDPR migration)" in {
        implicit val stringNoShrink: Shrink[String] =
          Shrink[String](_ => Stream.empty)
        forAll(atlassianHostGen, alphaStr) { (aHost, userAccountId) =>
          val host =
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          val customClaims =
            Seq("iss" -> host.clientKey, "sub" -> userAccountId)
          forAll(jwtCredentialsGen(secret = host.sharedSecret, customClaims)) {
            credentials =>
              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                jwtAuthenticationProvider.authenticate(credentials, "fake-qsh").value
              }
              result mustBe Right(
                DefaultAtlassianHostUser(host, None, Some(userAccountId)))
          }
        }
      }

      "successfully authenticate credentials with context claim (as before GDPR migration)" in {
        implicit val stringNoShrink: Shrink[String] =
          Shrink[String](_ => Stream.empty)
        forAll(atlassianHostGen, alphaStr, option(alphaStr)) {
          (aHost, userKey, userAccountId) =>
            val host =
              aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
            val contextClaim = Map(
              "user" -> Map("accountId" -> userAccountId.orNull,
                            "userKey" -> userKey).asJava).asJava
            val customClaims =
              Seq("iss" -> host.clientKey,
                  "sub" -> userKey,
                  "context" -> contextClaim)
            forAll(jwtCredentialsGen(secret = host.sharedSecret, customClaims)) {
              credentials =>
                (hostRepository
                  .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                  .successful(Some(host))

                val result = await {
                  jwtAuthenticationProvider.authenticate(credentials, "fake-qsh").value
                }
                result mustBe Right(
                  DefaultAtlassianHostUser(host, Some(userKey), userAccountId))
            }
        }
      }

      "successfully authenticate credentials with a with HTTP request QSH claim" in {
        implicit val stringNoShrink: Shrink[String] = {
          Shrink[String](_ => Stream.empty)
        }
        forAll(atlassianHostGen, alphaStr, canonicalHttpRequestGen) { (aHost, userAccountId, canonicalHttpRequest) =>
          val host = {
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          }
          val qsh = CanonicalHttpRequestQshProvider.qsh(canonicalHttpRequest)
          val customClaims =
            Seq("iss" -> host.clientKey, "sub" -> userAccountId, "qsh" -> qsh)
          forAll(jwtCredentialsGen(secret = host.sharedSecret, customClaims)) {
            credentials =>
              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                jwtAuthenticationProvider.authenticate(credentials, qsh).value
              }
              result mustBe Right(
                DefaultAtlassianHostUser(host, None, Some(userAccountId)))
          }
        }
      }

      "successfully authenticate credentials with a context QSH claim" in {
        implicit val stringNoShrink: Shrink[String] = {
          Shrink[String](_ => Stream.empty)
        }
        forAll(atlassianHostGen, alphaStr) { (aHost, userAccountId) =>
          val host = {
            aHost.copy(sharedSecret = JwtTestHelper.defaultSigningSecret)
          }
          val qsh = ContextQshProvider.qsh
          val customClaims =
            Seq("iss" -> host.clientKey, "sub" -> userAccountId, "qsh" -> qsh)
          forAll(jwtCredentialsGen(secret = host.sharedSecret, customClaims)) {
            credentials =>
              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                jwtAuthenticationProvider.authenticate(credentials, qsh).value
              }
              result mustBe Right(
                DefaultAtlassianHostUser(host, None, Some(userAccountId)))
          }
        }
      }
    }
  }

}
