package io.toolsplus.atlassian.connect.play.auth.jwt.asymmetric

import io.toolsplus.atlassian.connect.play.auth.jwt.{InvalidJwtError, JwtAuthenticationError}
import io.toolsplus.atlassian.connect.play.models.AtlassianConnectProperties
import play.api.Logger
import play.api.libs.ws.WSClient

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PublicKeyProvider @Inject()(
    ws: WSClient,
    atlassianConnectProperties: AtlassianConnectProperties) {

  private val logger = Logger(classOf[PublicKeyProvider])

  def fetchPublicKey(
      keyId: String): Future[Either[JwtAuthenticationError, String]] = {
    val publicKeyUrl = s"${atlassianConnectProperties.publicKeyHostBaseUrl}/$keyId"
    ws.url(publicKeyUrl)
      .get()
      .map(response =>
        response.status match {
          case 200 =>
            Right(response.body)
          case 404 =>
            logger.error(
              s"Failed to find public key for keyId '$keyId' ($response)")
            Left(
              InvalidJwtError(s"Failed to find public key for keyId '$keyId'"))
          case status =>
            logger.error(
              s"Unexpected error when fetching public key from '$publicKeyUrl' ($status) ($response)")
            Left(InvalidJwtError(
              s"Unexpected error when fetching public key from host '${atlassianConnectProperties.publicKeyHostBaseUrl}' ($status)"))
      })
  }

}
