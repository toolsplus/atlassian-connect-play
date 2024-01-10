package io.toolsplus.atlassian.connect.play.request.sttp.oauth2

import cats.implicits._
import io.circe
import io.circe.Decoder
import io.circe.parser.decode
import io.toolsplus.atlassian.connect.play.request.sttp.oauth2.model.{
  AccessTokenProvider,
  AccessTokenResponse,
  Error
}
import io.toolsplus.atlassian.connect.play.request.sttp.oauth2.model.Error._
import io.toolsplus.atlassian.connect.play.request.sttp.oauth2.model.AccessTokenResponse._
import sttp.client3.{
  DeserializationException,
  HttpError,
  ResponseAs,
  ResponseException,
  SttpBackend,
  asString,
  basicRequest
}
import sttp.model.Uri
import sttp.monad.MonadError
import sttp.monad.syntax._

object DefaultAccessTokenProvider extends AccessTokenProvider {

  /** Request new token with given scope from OAuth2 provider.
    *
    * The scope is the scope of the application we want to communicate with.
    */
  override def requestToken[F[_]](tokenUrl: Uri,
                                  clientId: String,
                                  clientSecret: String,
                                  scope: Option[String] = None)(
      backend: SttpBackend[F, Any]
  ): F[AccessTokenResponse] = {
    implicit val F: MonadError[F] = backend.responseMonad
    backend
      .send(
        basicRequest
          .post(tokenUrl)
          .body(requestTokenParams(clientId, clientSecret, scope))
          .response(responseWithCommonError[AccessTokenResponse])
      )
      .map(_.body)
      .map(_.leftMap(OAuth2Exception.apply))
      .flatMap(_.fold(F.error, F.unit))
  }

  private def requestTokenParams(clientId: String,
                                 clientSecret: String,
                                 scope: Option[String]) =
    Map(
      "grant_type" -> "client_credentials",
      "client_id" -> clientId,
      "client_secret" -> clientSecret
    ) ++ scope.map(s => Map("scope" -> s)).getOrElse(Map.empty)

  private def responseWithCommonError[A](
      implicit decoder: Decoder[A],
      oAuth2ErrorDecoder: Decoder[OAuth2Error]
  ): ResponseAs[Either[Error, A], Any] =
    asJson[A].mapWithMetadata {
      case (either, meta) =>
        either match {
          case Left(HttpError(response, statusCode))
              if statusCode.isClientError =>
            decode[OAuth2Error](response)
              .fold(
                error =>
                  Error
                    .HttpClientError(statusCode,
                                     DeserializationException(response, error))
                    .asLeft[A],
                _.asLeft[A])
          case Left(sttpError) =>
            Left(Error.HttpClientError(meta.code, sttpError))
          case Right(value) => value.asRight[Error]
        }
    }

  private def asJson[A: Decoder]
    : ResponseAs[Either[ResponseException[String, circe.Error], A], Any] =
    asString
      .mapWithMetadata(ResponseAs.deserializeRightWithError(decode[A]))
      .showAs("either(as string, as json)")

}
