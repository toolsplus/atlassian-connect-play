package io.toolsplus.atlassian.connect.play.ws

import java.net.URI

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import org.scalacheck.Gen._

import scala.concurrent.Future

class AtlassianHostUriResolverSpec extends TestSpec {

  val hostRepository: AtlassianHostRepository = mock[AtlassianHostRepository]

  val hostUriResolver = new AtlassianHostUriResolver(hostRepository)

  "Given a AtlassianHostUriResolver" when {

    "asked if a given URI is matching a given host" should {

      "return true if host base URL matches first part of given URI" in {
        forAll(rootRelativePathGen, atlassianHostGen) { (path, host) =>
          val uri = URI.create(s"${host.baseUrl}/$path")
          AtlassianHostUriResolver.isRequestToHost(uri, host) mustBe true
        }
      }

      "return false if host base URL does not match given URI" in {
        forAll(alphaStr.suchThat(!_.isEmpty), rootRelativePathGen, atlassianHostGen) { (hostName, path, host) =>
          val uri = URI.create(s"$hostName/$path")
          AtlassianHostUriResolver.isRequestToHost(uri, host) mustBe true
        }
      }

    }

    "asked to look-up host for a given URI" should {

      "successfully return matching host" in {
        forAll(rootRelativePathGen, atlassianHostGen) { (path, host) =>
          val uri = URI.create(s"${host.baseUrl}/$path")

          (hostRepository
            .findByBaseUrl(_: String)) expects host.baseUrl returning Future
            .successful(Some(host))

          val result = await {
            hostUriResolver.hostFromRequestUrl(uri)
          }
          result mustBe Some(host)
        }
      }

      "return None if no matching host can be found" in {
        forAll(rootRelativePathGen, atlassianHostGen) { (path, host) =>
          val uri = URI.create(s"${host.baseUrl}/$path")

          (hostRepository
            .findByBaseUrl(_: String)) expects host.baseUrl returning Future
            .successful(None)

          val result = await {
            hostUriResolver.hostFromRequestUrl(uri)
          }
          result mustBe None
        }
      }

      "return None if given URI is relative" in {
        forAll(rootRelativePathGen) { path =>
          await {
            hostUriResolver.hostFromRequestUrl(URI.create(path))
          } mustBe None
        }
      }

    }

  }

}
