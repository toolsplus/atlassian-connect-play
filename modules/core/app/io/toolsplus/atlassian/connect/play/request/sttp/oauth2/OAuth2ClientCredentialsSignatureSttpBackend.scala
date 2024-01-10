package io.toolsplus.atlassian.connect.play.request.sttp.oauth2

import cats.data.Validated
import io.toolsplus.atlassian.connect.play.request.AtlassianUrlUtils.authServerTokenUrl
import io.toolsplus.atlassian.connect.play.request.sttp.AtlassianHostRequest.RequestTExtensions
import io.toolsplus.atlassian.connect.play.request.sttp.oauth2.model.{
  AccessTokenProvider,
  OAuth2ClientCredentialsAtlassianHost,
  OAuth2ClientCredentialsAtlassianHostValidator
}
import sttp.capabilities.Effect
import sttp.client3.{
  DelegateSttpBackend,
  Identity,
  Request,
  RequestT,
  Response,
  SttpBackend
}
import sttp.model.Uri
import sttp.monad.MonadError
import sttp.monad.syntax._

/**
  * SttpBackend that adds client credentials auth bearer headers to every request and routes the given requests to
  * the appropriate API gateway.
  *
  * Do not use this backend directly. It is recommended to use [[io.toolsplus.atlassian.connect.play.request.sttp.RequestAsAppSttpBackend]]
  * instead, which will ensure requests to Atlassian hosts and APIs are signed according to the host and installation
  * status.
  */
final class OAuth2ClientCredentialsSignatureSttpBackend[F[_], P](
    delegate: SttpBackend[F, P],
    accessTokenProvider: AccessTokenProvider,
) extends DelegateSttpBackend(delegate) {
  implicit val F: MonadError[F] = delegate.responseMonad

  override def send[T, R >: P with Effect[F]](
      request: Request[T, R]): F[Response[T]] =
    for {
      host <- extractOAuth2Host(request)
      token <- accessTokenProvider.requestToken(
        Uri(authServerTokenUrl(host.toAtlassianHost)),
        host.oauthClientId,
        host.sharedSecret)(delegate)
      apiGatewayRequest <- routeRequestViaApiGateway(request, host)
      response <- delegate.send(
        apiGatewayRequest.auth.bearer(token.accessToken))
    } yield response

  private def extractOAuth2Host[T, R >: P with Effect[F]](
      request: Request[T, R]): F[OAuth2ClientCredentialsAtlassianHost] =
    request.atlassianHost match {
      case Right(host) =>
        OAuth2ClientCredentialsAtlassianHostValidator.validate(host) match {
          case Validated.Valid(oauth2Host) => F.unit(oauth2Host)
          case Validated.Invalid(errors) =>
            F.error(new Exception(
              s"Misconfigured OAuth2 host (${host.clientKey}): ${errors.toNonEmptyList.toList
                .map(_.getMessage)
                .mkString(", ")}"))
        }
      case Left(error) => F.error(error)
    }

  private def routeRequestViaApiGateway[T, R >: P with Effect[F]](
      request: Request[T, R],
      host: OAuth2ClientCredentialsAtlassianHost)
    : F[RequestT[Identity, T, R]] = {
    AtlassianHostUriResolver
      .resolveToAbsoluteApiGatewayUri(request.uri, host)
      .fold(F.error,
            apiGatewayUri =>
              F.unit(request.copy[Identity, T, R](uri = apiGatewayUri)))
  }

}

object OAuth2ClientCredentialsSignatureSttpBackend {
  def apply[F[_], P](
      accessTokenProvider: AccessTokenProvider
  )(
      backend: SttpBackend[F, P]
  ) =
    new OAuth2ClientCredentialsSignatureSttpBackend[F, P](backend,
                                                          accessTokenProvider)
}
