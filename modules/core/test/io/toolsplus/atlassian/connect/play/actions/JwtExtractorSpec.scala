package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.auth.jwt.{
  CanonicalPlayHttpRequest,
  JwtCredentials
}
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import org.scalacheck.Shrink
import play.api.http.HeaderNames

class JwtExtractorSpec extends TestSpec {

  "JwtExtractor" when {

    "trying to extract JWT from request " should {

      "successfully extract token from request header" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(signedJwtStringGen(), playRequestGen) { (rawJwt, request) =>
          val jwtHeader = HeaderNames.AUTHORIZATION -> s"${JwtExtractor.AuthorizationHeaderPrefix} $rawJwt"
          val jwtRequest = request.withHeaders(jwtHeader)
          val jwtCredentials =
            JwtCredentials(rawJwt, CanonicalPlayHttpRequest(jwtRequest))
          JwtExtractor.extractJwt(jwtRequest) mustBe Some(jwtCredentials)
        }
      }

      "successfully extract token from request query string" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(signedJwtStringGen()) { rawJwt =>
          val jwtQueryParams = Map("jwt" -> Seq(rawJwt))
          forAll(playRequestGen(jwtQueryParams)) { request =>
            val jwtCredentials =
              JwtCredentials(rawJwt, CanonicalPlayHttpRequest(request))
            JwtExtractor.extractJwt(request) mustBe Some(jwtCredentials)
          }
        }
      }

      "return None if request does not contain a token" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen) { request =>
          JwtExtractor.extractJwt(request) mustBe None
        }
      }

    }
  }

}
