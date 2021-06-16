package io.toolsplus.atlassian.connect.play.ws

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import org.scalacheck.Gen._

import java.net.URI

class AtlassianHostUriResolverSpec extends TestSpec {

  val hostRepository: AtlassianHostRepository = mock[AtlassianHostRepository]

  "Given a AtlassianHostUriResolver" when {

    "asked if a given URI is matching a given host" should {

      "return true if host base URL matches first part of given URI" in {
        forAll(rootRelativePathGen, atlassianHostGen) { (path, host) =>
          val uri = URI.create(s"${host.baseUrl}/$path")
          AtlassianHostUriResolver.isRequestToHost(uri, host) mustBe true
        }
      }

      "return false if host base URL does not match given URI" in {
        forAll(alphaStr.suchThat(_.nonEmpty), rootRelativePathGen, atlassianHostGen) { (hostName, path, host) =>
          val uri = URI.create(s"$hostName/$path")
          AtlassianHostUriResolver.isRequestToHost(uri, host) mustBe true
        }
      }

    }
  }

}
