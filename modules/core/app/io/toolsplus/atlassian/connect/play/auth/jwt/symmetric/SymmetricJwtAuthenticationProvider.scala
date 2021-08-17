package io.toolsplus.atlassian.connect.play.auth.jwt.symmetric

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import io.toolsplus.atlassian.connect.play.api.models.{
  AtlassianHost,
  AtlassianHostUser
}
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.auth.jwt.{
  AbstractJwtAuthenticationProvider,
  InvalidJwtError,
  JwtAuthenticationError,
  JwtCredentials
}
import io.toolsplus.atlassian.jwt.Jwt
import io.toolsplus.atlassian.jwt.symmetric.SymmetricJwtReader
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SymmetricJwtAuthenticationProvider @Inject()(
    hostRepository: AtlassianHostRepository)
    extends AbstractJwtAuthenticationProvider(hostRepository) {

  private val logger = Logger(classOf[SymmetricJwtAuthenticationProvider])

  /**
    * Authenticates the given JWT credentials and query string hash (QSH).
    *
    * @param jwtCredentials Untrusted JWT credentials
    * @param qsh Query string hash computed from the request the JWT credentials were attached to
    * @return Atlassian host user associated with the given JWT credentials, or an authentication error.
    * @see https://developer.atlassian.com/cloud/jira/platform/understanding-jwt-for-connect-apps/#decoding-and-verifying-a-jwt-token
    */
  override def authenticate(jwtCredentials: JwtCredentials, qsh: String)
    : EitherT[Future, JwtAuthenticationError, AtlassianHostUser] = {

    for {
      jwt <- parseJwt(jwtCredentials.rawJwt).toEitherT[Future]
      clientKey <- extractClientKey(jwt).toEitherT[Future]
      host <- fetchAtlassianHost(clientKey)
      verifiedToken <- verifyJwt(jwtCredentials, host, qsh).toEitherT[Future]
    } yield hostUserFromSubjectClaim(host, verifiedToken.claims)
  }

  private def verifyJwt(jwtCredentials: JwtCredentials,
                        host: AtlassianHost,
                        qsh: String): Either[JwtAuthenticationError, Jwt] = {
    SymmetricJwtReader(host.sharedSecret)
      .readAndVerify(jwtCredentials.rawJwt, qsh)
      .leftMap { e =>
        logger.error(s"Reading and validating of JWT failed: $e")
        InvalidJwtError(e.getMessage)
      }
  }
}
