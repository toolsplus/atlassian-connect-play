package io.toolsplus.atlassian.connect.play.request.sttp.oauth2

import cats.implicits._
import cats.MonadThrow
import io.toolsplus.atlassian.connect.play.TestSpec
import .AccessTokenResponse
import io.toolsplus.atlassian.connect.play.request.sttp.oauth2.core.{AccessTokenProvider, OAuth2ClientCredentialsSttpBackend}
import sttp.client3.{UriContext, asStringAlways, basicRequest}
import sttp.client3.testing._
import sttp.client3.testing.SttpBackendStub
import sttp.model.HeaderNames.Authorization
import sttp.model.{Header, Method, StatusCode, Uri}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class OAuth2ClientCredentialsSignatureSttpBackendSpec extends TestSpec {

  "OAuth2ClientCredentialsSttpBackend" when {
    val tokenUrl: Uri = uri"https://authserver.org/oauth2/token"
    val clientId: String = "clientid"
    val clientSecret: String = "secret"
    val scope: String = "scope"
    val accessToken: String = "token"
    val accessTokenProvider =
      new TestAccessTokenProvider[Future](Map(Some(scope) -> accessToken))
    val testAppUrl: Uri = uri"https://testapp.org/test"

    "TestApp is invoked once" should {
      "request a token and add the token to the TestApp request" in {

        val mockBackend: SttpBackendStub[Future, Any] =
          SttpBackendStub.asynchronousFuture
            .whenTokenIsRequested()
            .thenRespond(
              Right(
                AccessTokenResponse(accessToken,
                                    Some("domain"),
                                    100.seconds,
                                    Some(scope))))
            .whenTestAppIsRequestedWithToken(accessToken)
            .thenRespondOk()

        val backend = OAuth2ClientCredentialsSttpBackend[Future, Any](
          accessTokenProvider)(Some(scope))(mockBackend)

        backend
          .send(basicRequest.get(testAppUrl).response(asStringAlways))
          .map(_.code mustBe StatusCode.Ok)
      }
    }

    implicit class SttpBackendStubOps(
        val backend: SttpBackendStub[Future, Any]) {
      import backend.WhenRequest

      def whenTokenIsRequested(): WhenRequest = backend.whenRequestMatches {
        request =>
          request.method == Method.POST &&
          request.uri == tokenUrl &&
          request.forceBodyAsString == "grant_type=client_credentials&" +
            s"client_id=$clientId&" +
            s"client_secret=$clientSecret&" +
            s"scope=$scope"
      }

      def whenTestAppIsRequestedWithToken(accessToken: String): WhenRequest =
        backend.whenRequestMatches { request =>
          request.method == Method.GET &&
          request.uri == testAppUrl &&
          request.headers.contains(
            Header(Authorization, s"Bearer $accessToken"))
        }
    }
  }

  private class TestAccessTokenProvider[F[_]: MonadThrow](
      tokens: Map[Option[String], String])
      extends AccessTokenProvider[F] {

    def requestToken(
        scope: Option[String]): F[ClientCredentialsToken.AccessTokenResponse] =
      tokens
        .get(scope)
        .map(
          secret =>
            ClientCredentialsToken
              .AccessTokenResponse(secret, Some("domain"), 100.seconds, scope)
              .pure[F])
        .getOrElse(MonadThrow[F].raiseError(
          new IllegalArgumentException(s"Unknown $scope")))

  }

}
