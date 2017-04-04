package io.toolsplus.atlassian.connect.play.ws

import io.toolsplus.atlassian.connect.play.TestSpec
import UriImplicits._
import org.scalacheck.Gen._
import com.netaporter.uri.Uri
import org.scalacheck.Shrink

class UriImplicitsSpec extends TestSpec {

  "Given a URI" when {

    implicit val doNotShrinkStrings: Shrink[String] = Shrink.shrinkAny

    "checking if URI is absolute" should {

      "return true for an absolute URI" in {
        forAll(alphaStr.suchThat(!_.isEmpty), pathGen) { (hostName, path) =>
          Uri.parse(s"http://$hostName.com/$path").isAbsolute mustBe true
        }
      }

      "return false for a relative URI" in {
        forAll(pathGen) { path =>
          Uri.parse(path).isAbsolute mustBe false
        }
      }

    }

    "computing the base URL of a URL" should {

      "successfully return it" in {
        forAll(alphaStr.suchThat(!_.isEmpty), pathGen) { (hostName, path) =>
          val baseUrl = s"http://$hostName.com"
          Uri.parse(s"$baseUrl/$path").baseUrl mustBe Some(baseUrl)
        }
      }

      "return None if URI is relative" in {
        forAll(pathGen) { path =>
          Uri.parse(path).baseUrl mustBe None
        }
      }

    }

    "asked to append another URI" should {

      "correctly append the two together" in {
        forAll(alphaStr.suchThat(!_.isEmpty), pathGen) { (hostName, path) =>
          val baseUrl = s"http://$hostName.com"
          Uri.parse(baseUrl).append(Uri.parse(path)) mustBe Uri.parse(
            s"$baseUrl/$path")
        }
      }

    }

    "computing relative URI" should {

      "return relative part of absolute URIs" in {
        forAll(alphaStr.suchThat(!_.isEmpty), pathGen) { (hostName, path) =>
          val baseUrl = s"http://$hostName.com"
          Uri.parse(s"$baseUrl/$path").asRelativeUri mustBe Uri.parse(path)
        }
      }

      "return same URI if URI is already relative" in {
        forAll(pathGen) { path =>
          Uri.parse(path).asRelativeUri mustBe Uri.parse(path)
        }
      }

    }

  }

}
