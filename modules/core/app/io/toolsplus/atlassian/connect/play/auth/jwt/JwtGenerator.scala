package io.toolsplus.atlassian.connect.play.auth.jwt

import cats.syntax.either._
import io.toolsplus.atlassian.connect.play.api.models.{AppProperties, AtlassianHost}
import io.toolsplus.atlassian.connect.play.auth.jwt.JwtGenerator._
import io.toolsplus.atlassian.connect.play.models.AtlassianConnectProperties
import io.toolsplus.atlassian.connect.play.ws.AtlassianHostUriResolver
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.{HttpRequestCanonicalizer, JwtBuilder}
import play.api.Logger

import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class JwtGenerator @Inject()(
    addonProperties: AppProperties,
    atlassianConnectProperties: AtlassianConnectProperties) {

  private val logger = Logger(classOf[JwtGenerator])

  def createJwtToken(httpMethod: String,
                     uri: URI,
                     host: AtlassianHost): Either[JwtGeneratorError, RawJwt] =
    for {
      absoluteUri <- assertUriAbsolute(uri)
      uriToHost <- assertRequestToHost(absoluteUri, host)
      jwt <- internalCreateJwtToken(httpMethod, uriToHost, host)
    } yield jwt

  private def internalCreateJwtToken(
      httpMethod: String,
      uri: URI,
      host: AtlassianHost): Either[JwtGeneratorError, RawJwt] = {
    val hostContextPath = Option(URI.create(host.baseUrl).getPath)
    val canonicalHttpRequest =
      CanonicalUriHttpRequest(httpMethod, uri, hostContextPath)
    logger.trace(
      s"Generating JWT with canonical request: $canonicalHttpRequest")
    val queryHash =
      HttpRequestCanonicalizer.computeCanonicalRequestHash(canonicalHttpRequest)

    val expireAfter = Duration.of(atlassianConnectProperties.jwtExpirationTime,
                                  ChronoUnit.SECONDS)

    for {
      sharedSecret <- assertSecretKeyLessThan256Bits(host.sharedSecret)
      jwt <- new JwtBuilder(expireAfter)
        .withIssuer(addonProperties.key)
        .withQueryHash(queryHash)
        .build(sharedSecret)
        .leftMap(e => JwtSigningError(e.message, e.underlying))
    } yield jwt
  }

  private def assertSecretKeyLessThan256Bits(secretKey: String): Either[JwtGeneratorError, String] =
    if (secretKey.getBytes.length < (256 / 8)) Left(InvalidSecretKey)
    else Right(secretKey)

  private def assertUriAbsolute(uri: URI): Either[JwtGeneratorError, URI] = {
    if (uri.isAbsolute) Right(uri) else Left(RelativeUriError)
  }

  private def assertRequestToHost(
      uri: URI,
      host: AtlassianHost): Either[JwtGeneratorError, URI] = {
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

  final case class AtlassianHostNotFoundError(uri: URI)
      extends JwtGeneratorError {
    override val message: String =
      s"No Atlassian host found for the given URI $uri"
  }

  final case object InvalidSecretKey extends JwtGeneratorError {
    override def message = "Secret key must be more than 256 bits"
  }

}
