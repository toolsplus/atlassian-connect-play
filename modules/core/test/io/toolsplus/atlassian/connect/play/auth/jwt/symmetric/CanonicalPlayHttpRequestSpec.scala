package io.toolsplus.atlassian.connect.play.auth.jwt.symmetric

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.auth.jwt
import io.toolsplus.atlassian.connect.play.auth.jwt.{CanonicalPlayHttpRequest, symmetric}

class CanonicalPlayHttpRequestSpec extends TestSpec {

  "A CanonicalPlayHttpRequest" when {

    "created from a Play request" should {

      "return the request method" in {
        forAll(playRequestGen) { playRequest =>
          jwt.CanonicalPlayHttpRequest(playRequest).method mustBe playRequest.method
        }
      }

      "return the request path" in {
        forAll(playRequestGen) { playRequest =>
          CanonicalPlayHttpRequest(playRequest).relativePath mustBe playRequest.path
        }
      }

      "return the request parameter map" in {
        forAll(playRequestGen) { playRequest =>
          jwt.CanonicalPlayHttpRequest(playRequest).parameterMap mustBe playRequest.queryString
        }
      }

    }

  }

}
