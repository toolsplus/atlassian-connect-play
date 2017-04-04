package io.toolsplus.atlassian.connect.play.ws

import com.netaporter.uri.Uri
import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import org.scalacheck.Gen._
import UriImplicits._

import scala.concurrent.Future

class AtlassianHostUriResolverSpec extends TestSpec {

  val hostRepository = mock[AtlassianHostRepository]

  val $ = new AtlassianHostUriResolver(hostRepository)

  "Given a AtlassianHostUriResolver" when {

    "asked if a given URI is matching a given host" should {

      "return true if host base URL matches first part of given URI" in {
        forAll(pathGen, atlassianHostGen) { (path, host) =>
          val uri = Uri.parse(s"${host.baseUrl}/$path")
          AtlassianHostUriResolver.isRequestToHost(uri, host) mustBe true
        }
      }

      "return false if host base URL does not match given URI" in {
        forAll(alphaStr.suchThat(!_.isEmpty), pathGen, atlassianHostGen) { (hostName, path, host) =>
          val uri = Uri.parse(s"$hostName/$path")
          AtlassianHostUriResolver.isRequestToHost(uri, host) mustBe true
        }
      }

    }

    "asked to look-up host for a given URI" should {

      "successfully return matching host" in {
        forAll(pathGen, atlassianHostGen) { (path, host) =>
          val uri = Uri.parse(s"${host.baseUrl}/$path")

          (hostRepository
            .findByBaseUrl(_: String)) expects uri.baseUrl.get returning Future
            .successful(Some(host))

          val result = await {
            $.hostFromRequestUrl(uri)
          }
          result mustBe Some(host)
        }
      }

      "return None if no matching host can be found" in {
        forAll(pathGen, atlassianHostGen) { (path, host) =>
          val firstPathElement = "x"
          val uri = Uri.parse(s"${host.baseUrl}/x/$path")

          (hostRepository
            .findByBaseUrl(_: String)) expects uri.baseUrl.get returning Future
            .successful(None)

          (hostRepository
            .findByBaseUrl(_: String)) expects s"${uri.baseUrl.get}/$firstPathElement" returning Future
            .successful(None)

          val result = await {
            $.hostFromRequestUrl(uri)
          }
          result mustBe None
        }
      }

      "return None if given URI is relative" in {
        forAll(pathGen) { path =>
          await {
            $.hostFromRequestUrl(Uri.parse(path))
          } mustBe None
        }
      }

    }

  }

}
