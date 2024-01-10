package io.toolsplus.atlassian.connect.play.request.sttp

import io.toolsplus.atlassian.connect.play.api.models.JwtAuthenticationType
import io.toolsplus.atlassian.connect.play.request.sttp.AtlassianHostRequest.RequestTExtensions
import io.toolsplus.atlassian.connect.play.request.sttp.jwt.JwtSignatureSttpBackend
import io.toolsplus.atlassian.connect.play.request.sttp.oauth2.OAuth2ClientCredentialsSignatureSttpBackend
import sttp.capabilities.Effect
import sttp.client3.{DelegateSttpBackend, Request, Response, SttpBackend}
import sttp.monad.MonadError

/**
  * Sttp backend that authenticates Atlassian host requests as the app.
  *
  * Depending on the host authentication type this will either send the request with a JWT signature to the Atlassian
  * host (Connect installation), or with an OAuth2 client credentials bearer token to the Atlassian API gateway (Connect on
  * Forge installation).
  *
  * Requests must be associated with a `AtlassianHost` as follows, otherwise the backend will not be able to execute the
  * request:
  *
  * {{{
  * import io.toolsplus.atlassian.connect.play.request.sttp.AtlassianHostRequest._
  *
  * class MyJiraHttpClient @Inject()(sttpBackend: SttpBackend[Future, Any]) {
  *
  *   def fetchIssue(issueKey: String)(implicit host: AtlassianHost) = {
  *      atlassianHostRequest.get(s"/rest/api/2/issue/{issueKey}").send(sttpBackend)
  *   }
  * }
  * }}}
  */
class RequestAsAppSttpBackend[F[_], P](
    delegate: SttpBackend[F, P],
    jwtSignatureSttpBackendProvider: SttpBackend[F, P] => JwtSignatureSttpBackend[
      F,
      P],
    oAuth2ClientCredentialsSignatureSttpBackendProvider: SttpBackend[F, P] => OAuth2ClientCredentialsSignatureSttpBackend[
      F,
      P],
) extends DelegateSttpBackend(delegate) {

  private val jwtSignatureSttpBackend = jwtSignatureSttpBackendProvider(
    delegate)
  private val oAuth2ClientCredentialsSttpBackend =
    oAuth2ClientCredentialsSignatureSttpBackendProvider(delegate)

  implicit val F: MonadError[F] = delegate.responseMonad

  override def send[T, R >: P with Effect[F]](
      request: Request[T, R]): F[Response[T]] =
    request.atlassianHost.fold(
      F.error,
      host =>
        host.authenticationType match {
          case JwtAuthenticationType =>
            jwtSignatureSttpBackend.send(request)
            oAuth2ClientCredentialsSttpBackend.send(request)
      }
    )
}
