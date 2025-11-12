package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.auth.jwt
import io.toolsplus.atlassian.connect.play.auth.jwt.CanonicalPlayHttpRequest
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import org.scalacheck.Shrink
import org.scalatest.EitherValues
import play.api.http.HeaderNames
import play.api.mvc.Results.Unauthorized
import scala.concurrent.ExecutionContext.Implicits.global

class JwtActionSpec extends TestSpec with EitherValues {

  val jwtActionRefiner = new JwtActionRefiner()

  "JwtActionRefiner" when {

    "refining a standard Request" should {

      "successfully refine request to JwtRequest" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(signedSymmetricJwtStringGen(), playRequestGen) {
          (rawJwt, request) =>
            val jwtHeader =
              HeaderNames.AUTHORIZATION -> s"${JwtExtractor.AuthorizationHeaderPrefix} $rawJwt"
            val jwtRequest = request.withHeaders(jwtHeader)
            val jwtCredentials =
              jwt.JwtCredentials(rawJwt, CanonicalPlayHttpRequest(jwtRequest))
            val result = await {
              jwtActionRefiner.refine(jwtRequest)
            }
            result mustBe Right(JwtRequest(jwtCredentials, jwtRequest))
        }
      }

      "fail to refine request if it does not contain a token" in {
        forAll(playRequestGen) { request =>
          val result = await {
            jwtActionRefiner.refine(request)
          }
          result mustBe Left(Unauthorized("No authentication token found"))
        }
      }
    }
  }

}
