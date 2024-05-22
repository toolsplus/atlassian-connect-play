package io.toolsplus.atlassian.connect.play.auth.frc.jwt

import cats.implicits._
import io.circe.generic.auto._
import com.nimbusds.jwt.JWTClaimsSet
import io.circe.parser.decode
import io.toolsplus.atlassian.connect.play.auth.frc.{
  ForgeRemoteContext,
  ForgeRemoteCredentials
}
import io.toolsplus.atlassian.connect.play.auth.jwt.{
  InvalidJwtError,
  JwtAuthenticationError
}
import io.toolsplus.atlassian.connect.play.models.AtlassianForgeProperties
import io.toolsplus.atlassian.jwt.{Jwt, JwtParser}
import play.api.Logger

import javax.inject.Inject
import scala.util.{Failure, Success, Try}

/**
  * Authentication provider that verifies requests signed by Forge Remote Compute.
  */
class ForgeRemoteJwtAuthenticationProvider @Inject()(
    private val forgeProperties: AtlassianForgeProperties,
    private val forgeJWSKeySelector: ForgeJWSVerificationKeySelector) {

  private val logger = Logger(classOf[ForgeRemoteJwtAuthenticationProvider])

  private val fitProcessor = ForgeInvocationTokenProcessor.create(
    forgeProperties.appId,
    forgeJWSKeySelector)

  /**
    * Authenticates the given Forge Remote credentials.
    *
    * @param credentials Untrusted Forge Remote credentials
    * @return Verified Forge Remote context associated with the given credentials, or an authentication error.
    * @see https://developer.atlassian.com/cloud/jira/platform/understanding-jwt-for-connect-apps/#verifying-a-asymmetric-jwt-token-for-install-callbacks
    */
  def authenticate(credentials: ForgeRemoteCredentials)
    : Either[JwtAuthenticationError, ForgeRemoteContext] = {
    for {
      jwt <- parseJwt(credentials.forgeInvocationToken)
      invocationContext <- decodeForgeInvocationTokenPayload(jwt.json)
      _ <- verifyJwt(credentials, invocationContext)
    } yield ForgeRemoteContext(invocationContext, credentials)
  }

  private def parseJwt(rawJwt: String): Either[JwtAuthenticationError, Jwt] =
    JwtParser.parse(rawJwt).leftMap { e =>
      logger.error(s"Parsing of JWT failed: $e")
      InvalidJwtError(e.getMessage())
    }

  private def decodeForgeInvocationTokenPayload(fitPayload: String)
    : Either[JwtAuthenticationError, ForgeInvocationContext] =
    decode[ForgeInvocationContext](fitPayload).leftMap(e =>
      InvalidJwtError(e.getMessage))

  private def verifyJwt(credentials: ForgeRemoteCredentials,
                        invocationContext: ForgeInvocationContext)
    : Either[JwtAuthenticationError, JWTClaimsSet] = {
    Try(
      fitProcessor
        .process(credentials.forgeInvocationToken, invocationContext)) match {
      case Success(claimSet) => Right(claimSet)
      case Failure(e) =>
        logger.error(
          s"Reading and validating of Forge Invocation Token failed: $e")
        Left(InvalidJwtError(e.getMessage))
    }
  }
}
