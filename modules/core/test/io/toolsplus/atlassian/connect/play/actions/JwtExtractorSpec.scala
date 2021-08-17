package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.auth.jwt
import io.toolsplus.atlassian.connect.play.auth.jwt.CanonicalPlayHttpRequest
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import org.scalacheck.Shrink
import play.api.http.HeaderNames

class JwtExtractorSpec extends TestSpec {

  "JwtExtractor" when {

    "trying to extract JWT from request " should {

      "successfully extract token from request header" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(signedSymmetricJwtStringGen(), playRequestGen) { (rawJwt, request) =>
          val jwtHeader = HeaderNames.AUTHORIZATION -> s"${JwtExtractor.AuthorizationHeaderPrefix} $rawJwt"
          val jwtRequest = request.withHeaders(jwtHeader)
          val jwtCredentials =
            jwt.JwtCredentials(rawJwt, jwt.CanonicalPlayHttpRequest(jwtRequest))
          JwtExtractor.extractJwt(jwtRequest) mustBe Some(jwtCredentials)
        }
      }

      "successfully extract token from request query string" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(signedSymmetricJwtStringGen()) { rawJwt =>
          val jwtQueryParams = Map("jwt" -> Seq(rawJwt))
          forAll(playRequestGen(jwtQueryParams)) { request =>
            val jwtCredentials =
              jwt.JwtCredentials(rawJwt, jwt.CanonicalPlayHttpRequest(request))
            JwtExtractor.extractJwt(request) mustBe Some(jwtCredentials)
          }
        }
      }

      "return None if request does not contain a token" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(playRequestGen) { request =>
          JwtExtractor.extractJwt(request) mustBe None
        }
      }

    }
  }

}
