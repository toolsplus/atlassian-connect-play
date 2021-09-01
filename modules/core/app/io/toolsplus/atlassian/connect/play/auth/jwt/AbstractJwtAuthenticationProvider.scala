package io.toolsplus.atlassian.connect.play.auth.jwt

import cats.data.EitherT
import cats.implicits._
import com.nimbusds.jwt.JWTClaimsSet
import io.toolsplus.atlassian.connect.play.api.models.{AtlassianHost, AtlassianHostUser, DefaultAtlassianHostUser}
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.jwt.{Jwt, JwtParser}
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

abstract class AbstractJwtAuthenticationProvider[F[_]](
    hostRepository: AtlassianHostRepository)(
    implicit executionContext: ExecutionContext) {

  private val logger = Logger(classOf[AbstractJwtAuthenticationProvider[F]])

  def authenticate(jwtCredentials: JwtCredentials, qsh: String)
    : EitherT[Future, JwtAuthenticationError, F[AtlassianHostUser]]

  protected def parseJwt(rawJwt: String): Either[JwtAuthenticationError, Jwt] =
    JwtParser.parse(rawJwt).leftMap { e =>
      logger.error(s"Parsing of JWT failed: $e")
      InvalidJwtError(e.getMessage())
    }

  /**
    * Extracts the client key from the issuer claim of the given JWT.
    *
    * @param jwt JWT from which to extract the client key
    * @return Client key or authentication error if no issue claim could be found
    */
  protected def extractClientKey(
      jwt: Jwt): Either[JwtAuthenticationError, String] = {
    Option(jwt.claims.getIssuer) match {
      case Some(clientKeyClaim) => Right(clientKeyClaim)
      case None =>
        Left(
          JwtBadCredentialsError(
            "Failed to extract client key due to missing issuer claim")
        )
    }
  }

  /**
    * Creates Atlassian host user from subject claim.
    *
    * @param host Atlassian host user involved in this request
    * @param verifiedClaims Verified JWT claims for this request
    * @return Atlassian host user created from subject claim.
    */
  protected def hostUserFromSubjectClaim(
      host: AtlassianHost,
      verifiedClaims: JWTClaimsSet
  ): AtlassianHostUser = {
    DefaultAtlassianHostUser(host, Option(verifiedClaims.getSubject))
  }

  protected def fetchAtlassianHost(clientKey: String)
    : EitherT[Future, JwtAuthenticationError, AtlassianHost] = {
    EitherT(
      hostRepository.findByClientKey(clientKey).map {
        case Some(host) => Right(host)
        case None       => Left(UnknownJwtIssuerError(clientKey))
      }
    )
  }
}
