package io.toolsplus.atlassian.connect.play.auth.jwt.symmetric

import io.lemonlabs.uri.Url
import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.auth.jwt
import io.toolsplus.atlassian.connect.play.auth.jwt.{CanonicalUriHttpRequest, symmetric}
import org.scalacheck.Gen.option
import org.scalacheck.{Gen, Shrink}

import java.net.URI

class CanonicalUriHttpRequestSpec extends TestSpec {

  "A CanonicalUriHttpRequest" when {

    implicit val doNotShrinkStrings: Shrink[String] = Shrink.shrinkAny

    "method" should {

      "return the given method" in {
        forAll(methodGen, rootRelativePathGen, option(rootRelativePathGen)) {
          (method, requestPath, contextPath) =>
            jwt.CanonicalUriHttpRequest(method,
                                    URI.create(requestPath),
                                    contextPath).method mustBe method
        }
      }
    }

    "relativePath" should {

      "return '/' for empty string path parameter" in {
        forAll(methodGen) { method =>
          CanonicalUriHttpRequest(method, URI.create(""), None).relativePath mustBe "/"
        }
      }

      "return '/' for '/' path parameter" in {
        forAll(methodGen) { method =>
          jwt.CanonicalUriHttpRequest(method, URI.create("/"), None).relativePath mustBe "/"
        }
      }

      "remove context path from path parameter" in {
        forAll(methodGen) { method =>
          jwt.CanonicalUriHttpRequest(
            method,
            URI.create("https://example.com/jira/getsomething"),
            Some("/jira")).relativePath mustBe "/getsomething"
        }
      }

      "ignore context path if it is not a prefix of the path parameter" in {
        forAll(methodGen) { method =>
          jwt.CanonicalUriHttpRequest(
            method,
            URI.create("https://example.com/test/getsomething"),
            Some("/jira")).relativePath mustBe "/test/getsomething"
        }
      }

      "ignore context path if path parameter is empty" in {
        forAll(methodGen) { method =>
          jwt.CanonicalUriHttpRequest(
            method,
            URI.create("https://example.com"),
            Some("/jira")).relativePath mustBe "/"
        }
      }

      "return path parameter if no context path exists" in {
        forAll(methodGen) { method =>
          jwt.CanonicalUriHttpRequest(method,
                                  URI.create("https://example.com/test/getsomething"),
                                  None).relativePath mustBe "/test/getsomething"
        }
      }

      "return the relative path" in {
        forAll(methodGen,
               option(Gen.oneOf("/test", "/jira", "/context", "", "/"))) {
          (method, maybeContextPath) =>
            forAll(rootRelativePathWithQueryGen) {
              requestUrlWithoutContextPath =>
                val requestUri = maybeContextPath match {
                  case None | Some("") | Some("/") =>
                    URI.create(requestUrlWithoutContextPath)
                  case Some(contextPath) =>
                    URI.create(s"$contextPath$requestUrlWithoutContextPath")
                }
                val expectedRelativePath =
                  URI.create(requestUrlWithoutContextPath).getPath
                jwt.CanonicalUriHttpRequest(method, requestUri, maybeContextPath).relativePath mustBe expectedRelativePath
            }
        }
      }

    }

    "parameter map" should {
      "return the parameter map" in {
        forAll(methodGen,
               rootRelativePathWithQueryGen,
               option(rootRelativePathGen)) {
          (method, requestPath, contextPath) =>
            jwt.CanonicalUriHttpRequest(method,
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
