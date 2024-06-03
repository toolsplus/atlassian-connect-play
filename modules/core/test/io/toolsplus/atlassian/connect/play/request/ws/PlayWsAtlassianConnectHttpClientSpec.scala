package io.toolsplus.atlassian.connect.play.request.ws

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.auth.jwt.symmetric.JwtGenerator
import io.toolsplus.atlassian.connect.play.request.ws.jwt.JwtSignatureCalculator
import org.apache.pekko.util.ByteString
import org.scalacheck.Shrink
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames.{AUTHORIZATION, USER_AGENT}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.test.{Helpers, TestServer, WsTestClient}

import java.net.URI
import scala.concurrent.{Future, Promise}

class PlayWsAtlassianConnectHttpClientSpec
    extends TestSpec
    with GuiceOneAppPerSuite {

  val action: DefaultActionBuilder =
    app.injector.instanceOf[DefaultActionBuilder]
  val parser: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]

  val testServerPort = 44444
  val jwtGenerator: JwtGenerator = mock[JwtGenerator]

  "Given a AtlassianConnectHttpClient" when {

    "asked to send a add-on authenticated request" should {

      "successfully return a WSRequest with resolved host URL" in WsTestClient
        .withClient { client =>
          val httpClient =
            new PlayWsAtlassianConnectHttpClient(client, jwtGenerator)
          forAll(rootRelativePathGen, atlassianHostGen) { (path, host) =>
            val absoluteRequestUri = URI.create(s"${host.baseUrl}$path")
            val request = httpClient.authenticatedAsAddon(path)(host)
            request.url mustBe absoluteRequestUri.toString
          }
        }

      "set correct authorization and user-agent request headers" in {
        implicit val doNotShrinkStrings: Shrink[String] = Shrink.shrinkAny
        forAll(atlassianHostGen) { host =>
          val path = "foo"
          forAll(symmetricJwtCredentialsGen(host, subject = "bar")) {
            credentials =>
              val absoluteRequestUri =
                URI.create(s"http://localhost:$testServerPort/$path")

              (jwtGenerator
                .createJwtToken(_: String, _: URI, _: AtlassianHost))
                .expects("GET", absoluteRequestUri, host)
                .returning(Right(credentials.rawJwt))

              val (request, _, _) = receiveRequest { hostUrl => wsClient =>
                val httpClient =
                  new PlayWsAtlassianConnectHttpClient(wsClient, jwtGenerator)

                httpClient.authenticatedAsAddon(s"$hostUrl/$path")(host).get()
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
          action(parser.raw) { request =>
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
