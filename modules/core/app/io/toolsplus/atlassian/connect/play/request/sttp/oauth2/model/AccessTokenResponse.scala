package io.toolsplus.atlassian.connect.play.request.sttp.oauth2.model

import io.circe.Decoder

import scala.concurrent.duration.{DurationLong, FiniteDuration}

final case class AccessTokenResponse(
    accessToken: String,
    domain: Option[String],
    expiresIn: FiniteDuration,
    scope: Option[String]
)

object AccessTokenResponse {
  implicit val secondsDecoder: Decoder[FiniteDuration] =
    Decoder.decodeLong.map(_.seconds)

  implicit val accessTokenResponseDecoder: Decoder[AccessTokenResponse] =
    Decoder
      .forProduct4(
        "access_token",
        "domain",
        "expires_in",
        "scope"
      )(AccessTokenResponse.apply)
      .validate {
        _.downField("token_type").as[String] match {
          case Right(value) if value.equalsIgnoreCase("Bearer") => List.empty
          case Right(string) =>
            List(
              s"Error while decoding '.token_type': value '$string' is not equal to 'Bearer'")
          case Left(s) =>
            List(s"Error while decoding '.token_type': ${s.getMessage}")
        }
      }
}
