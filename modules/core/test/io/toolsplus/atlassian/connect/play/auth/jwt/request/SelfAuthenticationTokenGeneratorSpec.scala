package io.toolsplus.atlassian.connect.play.auth.jwt.request

import io.toolsplus.atlassian.connect.jwt.scala._
import io.toolsplus.atlassian.connect.jwt.scala.api.Predef.RawJwt
import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.models.AddonProperties
import org.scalatest.Assertion
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.collection.JavaConverters._

class SelfAuthenticationTokenGeneratorSpec
    extends TestSpec
    with GuiceOneAppPerSuite {

  val config = app.configuration

  val addonProperties = new AddonProperties(config)

  val $ = new SelfAuthenticationTokenGenerator(addonProperties)

  private val leewaySeconds = 30

  "A SelfAuthenticationTokenGenerator" when {

    "asked to generate a self-authenticated token" should {

      "set expiry based on configured 'selfAuthenticationExpirationTime'" in {
        forAll(atlassianHostUserGen) { hostUser =>
          val now = System.currentTimeMillis / 1000
          val result = $.createSelfAuthenticationToken(hostUser)

          def assertion(jwt: Jwt) = {
            val expiry = jwt.claims.getExpirationTime.getTime / 1000
            val expectedExpiry = now + addonProperties.selfAuthenticationExpirationTime
            expiry mustBe expectedExpiry +- leewaySeconds
          }

          validate(assertion)(result)
        }
      }

      "set issuer to add-on key" in {
        forAll(atlassianHostUserGen) { hostUser =>
          val result = $.createSelfAuthenticationToken(hostUser)
          def assertion(jwt: Jwt) = jwt.iss mustBe addonProperties.key
          validate(assertion)(result)
        }
      }

      "set audience to add-on key" in {
        forAll(atlassianHostUserGen) { hostUser =>
          val result = $.createSelfAuthenticationToken(hostUser)
          def assertion(jwt: Jwt) =
            jwt.claims.getAudience.asScala.toList mustBe List(
              addonProperties.key)
          validate(assertion)(result)
        }
      }

      "set client key claim to host client key" in {
        forAll(atlassianHostUserGen) { hostUser =>
          val result = $.createSelfAuthenticationToken(hostUser)
          def assertion(jwt: Jwt) =
            jwt.claims
              .getClaim(SelfAuthenticationTokenGenerator.HOST_CLIENT_KEY_CLAIM)
              .asInstanceOf[String] mustBe hostUser.host.clientKey
          validate(assertion)(result)
        }
      }

      "sign token with host's shared secret" in {
        forAll(atlassianHostUserGen, canonicalHttpRequestGen) {
          (hostUser, canonicalRequest) =>
            $.createSelfAuthenticationToken(hostUser) match {
              case Right(rawJwt) =>
                val qsh = HttpRequestCanonicalizer.computeCanonicalRequestHash(
                  canonicalRequest)
                JwtReader(hostUser.host.sharedSecret)
                  .readAndVerify(rawJwt, qsh) match {
                  case Right(_) => succeed
                  case Left(e) => fail(e)
                }
              case Left(e) => fail(e)
            }
        }
      }

    }

  }

  private def validate(assertion: Jwt => Assertion)(
      result: Either[JwtSigningError, RawJwt]) = {
    result match {
      case Right(rawJwt) =>
        JwtParser.parse(rawJwt) match {
          case Right(jwt) => assertion(jwt)
          case Left(e) => fail(e)
        }
      case Left(e) => fail(e)
    }
  }

}
