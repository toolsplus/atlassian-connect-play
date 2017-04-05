package io.toolsplus.atlassian.connect.play.auth.jwt

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import io.toolsplus.atlassian.jwt.{
  HttpRequestCanonicalizer,
  Jwt,
  JwtParser,
  JwtSigningError
}
import org.scalacheck.Gen._
import org.scalatest.Assertion

import scala.collection.JavaConverters._

class JwtBuilderSpec extends TestSpec {

  "A JwtBuilder" when {

    "used to generate a token" should {

      "successfully create JWT claims with specific expiration time" in {
        val expireAfter = Duration.of(10, ChronoUnit.SECONDS)
        val result =
          new JwtBuilder(expireAfter).build(JwtTestHelper.defaultSigningSecret)

        def assertion(jwt: Jwt) = {
          val expectedExpiry = Instant.now plus expireAfter
          val expiry = jwt.claims.getExpirationTime.getTime / 1000
          expiry mustBe expectedExpiry.getEpochSecond +- 1
        }

        validate(assertion)(result)
      }

      "successfully create JWT claims with overridden expiration time" in {
        val initialExpireAfter = Duration.of(5, ChronoUnit.SECONDS)
        val overrideExpireAfter = Duration.of(10, ChronoUnit.SECONDS)
        val expectedExpiry = Instant.now plus overrideExpireAfter
        val result = new JwtBuilder(initialExpireAfter)
          .withExpirationTime(expectedExpiry.getEpochSecond)
          .build(JwtTestHelper.defaultSigningSecret)

        def assertion(jwt: Jwt) = {
          val expiry = jwt.claims.getExpirationTime.getTime / 1000
          expiry mustBe expectedExpiry.getEpochSecond +- 1
        }

        validate(assertion)(result)
      }

      "successfully create JWT claims with overridden issue time" in {
        val expireAfter = Duration.of(10, ChronoUnit.SECONDS)
        val expectedIssuedAt = Instant.now plus Duration.of(5,
                                                            ChronoUnit.SECONDS)
        val result = new JwtBuilder(expireAfter)
          .withIssuedAt(expectedIssuedAt.getEpochSecond)
          .build(JwtTestHelper.defaultSigningSecret)

        def assertion(jwt: Jwt) = {
          val issuedAt = jwt.claims.getIssueTime.getTime / 1000
          issuedAt mustBe expectedIssuedAt.getEpochSecond +- 1
        }

        validate(assertion)(result)
      }

      "successfully create JWT with standard claims" in {
        forAll(alphaStr, alphaStr, listOf(alphaStr)) {
          (issuer, subject, audience) =>
            val expireAfter = Duration.of(10, ChronoUnit.SECONDS)
            val now = Instant.now
            val expectedExpiry = now plus expireAfter
            val notBefore = now minus expireAfter
            val result = new JwtBuilder(expireAfter)
              .withIssuer(issuer)
              .withSubject(subject)
              .withAudience(audience)
              .withNotBefore(notBefore.getEpochSecond)
              .build(JwtTestHelper.defaultSigningSecret)

            def assertion(jwt: Jwt) = {
              val claims = jwt.claims
              claims.getIssueTime.getTime / 1000 mustBe now.getEpochSecond +- 1
              claims.getExpirationTime.getTime / 1000 mustBe expectedExpiry.getEpochSecond +- 1
              claims.getSubject mustBe subject
              claims.getAudience mustBe audience.asJava
              claims.getNotBeforeTime.getTime / 1000 mustBe notBefore.getEpochSecond
            }

            validate(assertion)(result)
        }
      }

      "successfully create JWT with custom claims" in {
        forAll(alphaStr, alphaStr) { (claimName, claimValue) =>
          val expireAfter = Duration.of(10, ChronoUnit.SECONDS)
          val result = new JwtBuilder(expireAfter)
            .withClaim(claimName, claimValue)
            .build(JwtTestHelper.defaultSigningSecret)

          def assertion(jwt: Jwt) = {
            jwt.claims
              .getClaim(claimName)
              .asInstanceOf[String] mustBe claimValue
          }

          validate(assertion)(result)
        }
      }

      "successfully create JWT with qsh claim" in {
        forAll(canonicalHttpRequestGen) { (request) =>
          val expireAfter = Duration.of(10, ChronoUnit.SECONDS)
          val qsh =
            HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
          val result = new JwtBuilder(expireAfter)
            .withQueryHash(qsh)
            .build(JwtTestHelper.defaultSigningSecret)
          def assertion(jwt: Jwt) = {
            jwt.claims.getClaim("qsh").asInstanceOf[String] mustBe qsh
          }

          validate(assertion)(result)
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
