package io.toolsplus.atlassian.connect.play.auth.jwt

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.models.AddonProperties
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class CanonicalPlayHttpRequestSpec extends TestSpec with GuiceOneAppPerSuite {

  val config = app.configuration

  val hostRepository = mock[AtlassianHostRepository]
  val addonProperties = new AddonProperties(config)

  val $ = new JwtAuthenticationProvider(hostRepository, addonProperties)

  "A CanonicalPlayHttpRequest" when {

    "created from a Play request" should {

      "return the request method" in {
        forAll(playRequestGen) { playRequest =>
          CanonicalPlayHttpRequest(playRequest).method mustBe playRequest.method
        }
      }

      "return the request path" in {
        forAll(playRequestGen) { playRequest =>
          CanonicalPlayHttpRequest(playRequest).relativePath mustBe playRequest.path
        }
      }

      "return the request parameter map" in {
        forAll(playRequestGen) { playRequest =>
          CanonicalPlayHttpRequest(playRequest).parameterMap mustBe playRequest.queryString
        }
      }

    }

  }

}
