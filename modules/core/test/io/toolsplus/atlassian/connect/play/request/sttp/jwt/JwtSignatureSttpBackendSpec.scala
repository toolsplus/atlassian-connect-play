package io.toolsplus.atlassian.connect.play.request.sttp.jwt

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.auth.jwt.symmetric.JwtGenerator
import io.toolsplus.atlassian.connect.play.models.{
  PlayAddonProperties,
  PlayAtlassianConnectProperties
}
import io.toolsplus.atlassian.connect.play.request.sttp.AtlassianHostRequest.atlassianHostRequest
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import sttp.client3.testing.{RecordingSttpBackend, SttpBackendStub}
import sttp.client3.{Response, UriContext, basicRequest}
import sttp.model.StatusCode._
import sttp.model.Uri

import scala.concurrent.Future

class JwtSignatureSttpBackendSpec
    extends TestSpec
    with ScalaFutures
    with OptionValues
    with GuiceOneAppPerSuite {

  val relativeTestUrl: Uri = uri"/rest/api/2/issue/1"

  val config: Configuration = app.configuration
  val addonProperties = new PlayAddonProperties(config)
  val connectProperties = new PlayAtlassianConnectProperties(config)

  val jwtGenerator =
    new JwtGenerator(addonProperties, connectProperties)

  val ok: Response[String] = Response("", Ok)

  val always200Backend: SttpBackendStub[Future, Any] =
    SttpBackendStub.asynchronousFuture.whenAnyRequest.thenRespond(ok)

  "Given a SttpBackend wrapped in JwtSignatureSttpBackend" when {

    "sending a request without an associated host" should {
      "fail with a host not configured message" in {
        val backend: JwtSignatureSttpBackend[Future, Any] =
          new JwtSignatureSttpBackend[Future, Any](
            always200Backend,
            jwtGenerator
          )
        val response =
          basicRequest.get(relativeTestUrl).send(backend)
        whenReady(response.failed) { error =>
          error.getMessage must startWith(
            "Failed to extract Atlassian host from request: No host configured."
          )
        }
      }
    }

    "sending a request with an invalid host" should {
      "fail with a invalid host message" in {
        val backend: JwtSignatureSttpBackend[Future, Any] =
          new JwtSignatureSttpBackend[Future, Any](
            always200Backend,
            jwtGenerator
          )
        val response =
          basicRequest
            .tag("ATLASSIAN_HOST", "not-an-atlassian-host")
            .get(relativeTestUrl)
            .send(backend)
        whenReady(response.failed) { error =>
          error.getMessage must startWith(
            "Failed to extract Atlassian host from request: Invalid host type"
          )
        }
      }
    }

    "sending a request with a valid host" should {
      "successfully sign the request to a relative URL" in {
        forAll(atlassianHostGen) { host =>
          val recordingBackend = new RecordingSttpBackend(always200Backend)
          val backend: JwtSignatureSttpBackend[Future, Any] =
            new JwtSignatureSttpBackend[Future, Any](
              recordingBackend,
              jwtGenerator
            )
          val response =
            atlassianHostRequest(host)
              .get(relativeTestUrl)
              .send(backend)
          val result = await(response)
          result.code mustBe Ok

          recordingBackend.allInteractions.head._1
            .header("Authorization")
            .get must fullyMatch regex "^JWT .+"
        }
      }

      "successfully sign the request to an absolute URL if the host matches" in {
        forAll(atlassianHostGen) { host =>
          val recordingBackend = new RecordingSttpBackend(always200Backend)
          val backend: JwtSignatureSttpBackend[Future, Any] =
            new JwtSignatureSttpBackend[Future, Any](
              recordingBackend,
              jwtGenerator
            )
          val response =
            atlassianHostRequest(host)
              .get(uri"${host.baseUrl}".withPath(relativeTestUrl.path))
              .send(backend)
          val result = await(response)
          result.code mustBe Ok

          recordingBackend.allInteractions.head._1
            .header("Authorization")
            .get must fullyMatch regex "^JWT .+"
        }
      }

      "fail to sign the request if the absolute URL host does not match the given host" in {
        val backend: JwtSignatureSttpBackend[Future, Any] =
          new JwtSignatureSttpBackend[Future, Any](
            always200Backend,
            jwtGenerator
          )
        forAll(atlassianHostGen) { host =>
          val response =
            atlassianHostRequest(host)
              .get(
                uri"https://mismatch-host-base-url.atlassian.net"
                  .withPath(relativeTestUrl.path)
              )
              .send(backend)
          whenReady(response.failed) { error =>
            error.getMessage must startWith(
              "Unexpected JWT error: The given URI is not under the base URL of the given host"
            )
          }
        }
      }

    }

  }

}
