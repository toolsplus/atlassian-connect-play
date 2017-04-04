package io.toolsplus.atlassian.connect.play.ws

import akka.util.ByteString
import com.netaporter.uri.Uri
import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.auth.jwt.JwtGenerator
import io.toolsplus.atlassian.connect.play.ws.jwt.JwtSignatureCalculator
import org.scalacheck.Shrink
import play.api.http.HeaderNames.{AUTHORIZATION, USER_AGENT}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, BodyParsers, RequestHeader, Results}
import play.api.test.{Helpers, TestServer, WsTestClient}

import scala.concurrent.{Future, Promise}

class AtlassianConnectHttpClientSpec extends TestSpec {

  val testServerPort = 44444
  val jwtGenerator = mock[JwtGenerator]

  "Given a AtlassianConnectHttpClient" when {

    "asked to send a add-on authenticated request" should {

      "successfully return a WSRequest with resolved host URL" in WsTestClient
        .withClient { client =>
          val $ = new AtlassianConnectHttpClient(client, jwtGenerator)
          forAll(pathGen, atlassianHostGen) { (path, host) =>
            val absoluteRequestUri = Uri.parse(s"${host.baseUrl}/$path")
            val request = $.authenticatedAsAddon(path)(host)
            request.url mustBe absoluteRequestUri.toString
          }
        }

      "set correct authorization and user-agent request headers" in {
        implicit val doNotShrinkStrings = Shrink[String](_ => Stream.empty)
        forAll(atlassianHostGen) { (host) =>
          val path = "foo"
          forAll(jwtCredentialsGen(host, subject = "bar")) { credentials =>
            val absoluteHostUri = Uri.parse(s"${host.baseUrl}/$path")
            val absoluteRequestUri =
              Uri.parse(s"http://localhost:$testServerPort/$path")

            (jwtGenerator
              .createJwtToken(_: String, _: Uri, _: AtlassianHost))
              .expects("GET", absoluteRequestUri, host)
              .returning(Right(credentials.rawJwt))

            val (request, _, _) = receiveRequest { hostUrl => wsClient =>
              val $ = new AtlassianConnectHttpClient(wsClient, jwtGenerator)

              $.authenticatedAsAddon(s"$hostUrl/$path")(host).get()
            }
            request.headers(AUTHORIZATION).startsWith("JWT ") mustBe true
            request
              .headers(AUTHORIZATION)
              .drop("JWT ".length) mustBe credentials.rawJwt
            request.headers(USER_AGENT) mustBe JwtSignatureCalculator.userAgent
          }
        }
      }
    }

  }

  def receiveRequest(makeRequest: String => WSClient => Future[_])
    : (RequestHeader, ByteString, String) = {
    val hostUrl = s"http://localhost:$testServerPort"
    val promise = Promise[(RequestHeader, ByteString)]()
    val app = GuiceApplicationBuilder()
      .routes {
        case _ =>
          Action(BodyParsers.parse.raw) { request =>
            promise.success(
              (request, request.body.asBytes().getOrElse(ByteString.empty)))
            Results.Ok
          }
      }
      .build()
    val wsClient = app.injector.instanceOf[WSClient]
    Helpers.running(TestServer(testServerPort, app)) {
      await(makeRequest(hostUrl)(wsClient))
    }
    val (request, body) = await(promise.future)
    (request, body, hostUrl)
  }

}
