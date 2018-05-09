package io.toolsplus.atlassian.connect.play.auth.jwt

import java.time.Duration
import java.time.temporal.ChronoUnit

import cats.syntax.either._
import com.netaporter.uri.Uri
import io.toolsplus.atlassian.connect.play.api.models.{
  AppProperties,
  AtlassianHost
}
import io.toolsplus.atlassian.connect.play.auth.jwt.JwtGenerator._
import io.toolsplus.atlassian.connect.play.models.AtlassianConnectProperties
import io.toolsplus.atlassian.connect.play.ws.AtlassianHostUriResolver
import io.toolsplus.atlassian.jwt.HttpRequestCanonicalizer
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import javax.inject.Inject
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JwtGenerator @Inject()(
    addonProperties: AppProperties,
    atlassianConnectProperties: AtlassianConnectProperties,
    hostUriResolver: AtlassianHostUriResolver) {

  private val logger = Logger(classOf[JwtGenerator])

  def createJwtToken(httpMethod: String,
                     uri: Uri): Future[Either[JwtGeneratorError, RawJwt]] = {
    assertUriAbsolute(uri) match {
      case Left(e) => Future.successful(Left(e))
      case Right(_) =>
        hostUriResolver.hostFromRequestUrl(uri).map {
          case Some(host) => internalCreateJwtToken(httpMethod, uri, host)
          case None       => Left(AtlassianHostNotFoundError(uri))
        }
    }
  }

  def createJwtToken(httpMethod: String,
                     uri: Uri,
                     host: AtlassianHost): Either[JwtGeneratorError, RawJwt] = {
    assertUriAbsolute(uri)
      .flatMap(assertRequestToHost(_, host))
      .flatMap(internalCreateJwtToken(httpMethod, _, host))
  }

  private def internalCreateJwtToken(
      httpMethod: String,
      uri: Uri,
      host: AtlassianHost): Either[JwtGeneratorError, RawJwt] = {
    val canonicalHttpRequest =
      CanonicalUriHttpRequest(httpMethod, uri, host.baseUrl)
    logger.debug(
      s"Generating JWT with canonical request: $canonicalHttpRequest")
    val queryHash =
      HttpRequestCanonicalizer.computeCanonicalRequestHash(canonicalHttpRequest)

    val expireAfter = Duration.of(atlassianConnectProperties.jwtExpirationTime,
                                  ChronoUnit.SECONDS)

    new JwtBuilder(expireAfter)
      .withIssuer(addonProperties.key)
      .withQueryHash(queryHash)
      .build(host.sharedSecret)
      .leftMap(e => JwtSigningError(e.message, e.underlying))
  }

  private def assertUriAbsolute(uri: Uri): Either[JwtGeneratorError, Uri] = {
    if (uri.toURI.isAbsolute) Right(uri) else Left(RelativeUriError)
  }

  private def assertRequestToHost(
      uri: Uri,
      host: AtlassianHost): Either[JwtGeneratorError, Uri] = {
    if (AtlassianHostUriResolver.isRequestToHost(uri, host)) Right(uri)
    else Left(BaseUrlMismatchError)
  }

}

object JwtGenerator {

  sealed trait JwtGeneratorError {
    def message: String
  }

  final case object RelativeUriError extends JwtGeneratorError {
    override val message: String = "The given URI is not absolute"
  }

  final case object BaseUrlMismatchError extends JwtGeneratorError {
    override val message: String =
      "The given URI is not under the base URL of the given host"
  }

  final case class JwtSigningError(message: String, cause: Throwable)
      extends JwtGeneratorError

  final case class AtlassianHostNotFoundError(uri: Uri)
      extends JwtGeneratorError {
    override val message: String =
      s"No Atlassian host found for the given URI $uri"
  }

}
