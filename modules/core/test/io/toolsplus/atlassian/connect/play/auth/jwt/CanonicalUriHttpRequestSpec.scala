package io.toolsplus.atlassian.connect.play.auth.jwt

import com.netaporter.uri.Uri
import io.toolsplus.atlassian.connect.play.TestSpec
import org.scalacheck.Shrink

class CanonicalUriHttpRequestSpec extends TestSpec {

  "A CanonicalUriHttpRequest" when {

    implicit val doNotShrinkStrings: Shrink[String] = Shrink.shrinkAny

    "created from standard parameters" should {

      "return the given method" in {
        forAll(methodGen, pathGen, pathGen) {
          (method, requestPath, contextPath) =>
            CanonicalUriHttpRequest(method,
                                    Uri.parse(requestPath),
                                    contextPath).method mustBe method
        }
      }

      "return the relative path" in {
        forAll(methodGen, pathWithQueryGen, pathGen) {
          (method, relativePath, contextPath) =>
            val relativeUri = Uri.parse(relativePath)
            val contextUri = Uri.parse(contextPath)
            val requestUri = Uri.parse(s"$contextUri$relativeUri")
            val expectedRelativePath =
              if (relativeUri.path.isEmpty) "/" else relativeUri.path
            CanonicalUriHttpRequest(method, requestUri, contextPath).relativePath mustBe expectedRelativePath
        }
      }

      "return the parameter map" in {
        forAll(methodGen, pathWithQueryGen, pathGen) {
          (method, requestPath, contextPath) =>
            val requestUri = Uri.parse(requestPath)
            CanonicalUriHttpRequest(method, requestUri, contextPath).parameterMap mustBe requestUri.query.paramMap
        }
      }

    }

  }

}
