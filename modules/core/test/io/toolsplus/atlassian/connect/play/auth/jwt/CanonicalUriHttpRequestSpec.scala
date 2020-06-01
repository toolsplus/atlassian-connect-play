package io.toolsplus.atlassian.connect.play.auth.jwt

import java.net.URI

import io.lemonlabs.uri.Url
import io.toolsplus.atlassian.connect.play.TestSpec
import org.scalacheck.Shrink

class CanonicalUriHttpRequestSpec extends TestSpec {

  "A CanonicalUriHttpRequest" when {

    implicit val doNotShrinkStrings: Shrink[String] = Shrink.shrinkAny

    "created from standard parameters" should {

      "return the given method" in {
        forAll(methodGen, rootRelativePathGen, rootRelativePathGen) {
          (method, requestPath, contextPath) =>
            CanonicalUriHttpRequest(method,
                                    URI.create(requestPath),
                                    contextPath).method mustBe method
        }
      }

      "return the relative path" in {
        forAll(methodGen, rootRelativePathWithQueryGen, rootRelativePathGen) {
          (method, relativePath, contextPath) =>
            val relativeUri = Url.parse(relativePath)
            val contextUri = Url.parse(contextPath)
            val requestUri = contextUri
              .withPath(contextUri.path.addParts(relativeUri.path.parts))
              .withQueryString(relativeUri.query)
            val expectedRelativePath =
              if (relativeUri.path.isEmpty) "/" else relativeUri.path.toString
            CanonicalUriHttpRequest(
              method,
              URI.create(requestUri.toString),
              contextPath).relativePath mustBe expectedRelativePath
        }
      }

      "return the parameter map" in {
        forAll(methodGen, rootRelativePathWithQueryGen, rootRelativePathGen) {
          (method, requestPath, contextPath) =>
            CanonicalUriHttpRequest(method,
                                    URI.create(requestPath),
                                    contextPath).parameterMap mustBe Url
              .parse(requestPath)
              .query
              .paramMap
        }
      }

    }

  }

}
