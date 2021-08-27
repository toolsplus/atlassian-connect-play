package io.toolsplus.atlassian.connect.play.auth.jwt.symmetric

import cats.syntax.either._
import io.toolsplus.atlassian.connect.play.api.models.{AppProperties, AtlassianHost}
import io.toolsplus.atlassian.connect.play.auth.jwt.CanonicalUriHttpRequest
import io.toolsplus.atlassian.connect.play.auth.jwt.symmetric.JwtGenerator._
import io.toolsplus.atlassian.connect.play.models.AtlassianConnectProperties
import io.toolsplus.atlassian.connect.play.ws.AtlassianHostUriResolver
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.{HttpRequestCanonicalizer, JwtBuilder}
import play.api.Logger

import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
  * JwtGenerator used to generated symmetrically signed JWTs to make requests from the
  * app host to the Atlassian host.
  *
  * @param appProperties App properties of this app
  * @param atlassianConnectProperties Atlassian Connect properties of this app
  */
class JwtGenerator @Inject()(
                              appProperties: AppProperties,
                              atlassianConnectProperties: AtlassianConnectProperties) {

  private val logger = Logger(classOf[JwtGenerator])

  /**
    * Generates a JWT for the given Atlassian host and request details. This token
    * can be used to make requests to the host itself.
    *
    * Note that JWTs to send requests to an Atlassian host need to include a query string
    * hash (QSH) claim. To compute the QSH this generator needs to know the HTTP request
    * method and URI (including query string parameters) at token creation time.
    *
    * @param httpMethod HTTP method of the intended host request
    * @param uri URI of the intended host request
    * @param host Atlassian host the request is targeting
    * @return JWT token for the specific host request defined by the input parameters
    */
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
        .withIssuer(appProperties.key)
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
