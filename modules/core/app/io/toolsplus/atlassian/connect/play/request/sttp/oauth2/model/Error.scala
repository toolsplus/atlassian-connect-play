package io.toolsplus.atlassian.connect.play.request.sttp.oauth2.model

import io.circe.Decoder
import sttp.model.StatusCode

sealed trait Error extends Throwable with Product with Serializable

object Error {

  final case class HttpClientError(statusCode: StatusCode, cause: Throwable)
      extends Exception(
        s"Client call resulted in error ($statusCode): ${cause.getMessage}",
        cause)
      with Error

  sealed trait OAuth2Error extends Error

  /** Token errors as listed in documentation: https://tools.ietf.org/html/rfc6749#section-5.2
    */
  final case class OAuth2ErrorResponse(
      errorType: OAuth2ErrorResponse.OAuth2ErrorResponseType,
      errorDescription: Option[String])
      extends Exception(errorDescription.fold(s"$errorType")(description =>
        s"$errorType: $description"))
      with OAuth2Error

  object OAuth2ErrorResponse {

    sealed trait OAuth2ErrorResponseType extends Product with Serializable

    case object InvalidRequest extends OAuth2ErrorResponseType

    case object InvalidClient extends OAuth2ErrorResponseType

    case object InvalidGrant extends OAuth2ErrorResponseType

    case object UnauthorizedClient extends OAuth2ErrorResponseType

    case object UnsupportedGrantType extends OAuth2ErrorResponseType

    case object InvalidScope extends OAuth2ErrorResponseType

  }

  final case class UnknownOAuth2Error(error: String,
                                      errorDescription: Option[String])
      extends Exception(
        errorDescription.fold(s"Unknown OAuth2 error type: $error")(
          description =>
            s"Unknown OAuth2 error type: $error, description: $description")
      )
      with OAuth2Error

  object OAuth2Error {

    private def fromErrorTypeAndDescription(
        errorType: String,
        description: Option[String]): OAuth2Error =
      errorType match {
        case "invalid_request" =>
          OAuth2ErrorResponse(OAuth2ErrorResponse.InvalidRequest, description)
        case "invalid_client" =>
          OAuth2ErrorResponse(OAuth2ErrorResponse.InvalidClient, description)
        case "invalid_grant" =>
          OAuth2ErrorResponse(OAuth2ErrorResponse.InvalidGrant, description)
        case "unauthorized_client" =>
          OAuth2ErrorResponse(OAuth2ErrorResponse.UnauthorizedClient,
                              description)
        case "unsupported_grant_type" =>
          OAuth2ErrorResponse(OAuth2ErrorResponse.UnsupportedGrantType,
                              description)
        case "invalid_scope" =>
          OAuth2ErrorResponse(OAuth2ErrorResponse.InvalidScope, description)
        case unknown => UnknownOAuth2Error(unknown, description)
      }

    implicit val errorDecoder: Decoder[OAuth2Error] =
      Decoder.forProduct2[OAuth2Error, String, Option[String]](
        "error",
        "error_description")(OAuth2Error.fromErrorTypeAndDescription)

  }

  final case class OAuth2Exception(error: Error)
      extends Exception(error.getMessage, error)

  final case class ParsingException(msg: String) extends Exception(msg)

}
