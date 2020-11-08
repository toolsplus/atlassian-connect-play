package io.toolsplus.atlassian.connect.play.auth.jwt

import cats.data.EitherT
import cats.syntax.all._
import com.google.inject.Inject
import com.nimbusds.jwt.JWTClaimsSet
import io.circe.generic.auto._
import io.circe.parser.decode
import io.toolsplus.atlassian.connect.play.api.models.{
  AppProperties,
  AtlassianHost,
  AtlassianHostUser,
  DefaultAtlassianHostUser
}
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.jwt.{
  HttpRequestCanonicalizer,
  Jwt,
  JwtParser,
  JwtReader
}
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JwtAuthenticationProvider @Inject()(
    hostRepository: AtlassianHostRepository,
    addonConfiguration: AppProperties) {

  private val logger = Logger(classOf[JwtAuthenticationProvider])

  def authenticate(jwtCredentials: JwtCredentials)
    : EitherT[Future, JwtAuthenticationError, AtlassianHostUser] =
    for {
      jwt <- parseJwt(jwtCredentials.rawJwt).toEitherT[Future]
      clientKey <- extractClientKey(jwt).toEitherT[Future]
      host <- fetchAtlassianHost(clientKey)
      verifiedToken <- verifyJwt(jwtCredentials, host).toEitherT[Future]
    } yield
      hostUserFromContextClaim(host, verifiedToken.claims)
        .getOrElse(hostUserFromSubjectClaim(host, verifiedToken.claims))

  /**
    * Tries to create host user from context claim. Note that this claim
    * will be removed by Atlassian effective from 29 March, 2019.
    *
    * After opting in to GDPR APIs or after March 29 we can safely remove
    * this method because there won't be any context claim in JWTs anymore.
    *
    * @param host Atlassian host user involved in this request
    * @param verifiedClaims Verified JWT claims for this request
    * @return Atlassian host user if one could be parsed from context claim.
    */
  private def hostUserFromContextClaim(
      host: AtlassianHost,
      verifiedClaims: JWTClaimsSet): Option[AtlassianHostUser] = {
    case class JwtUser(accountId: Option[String], userKey: Option[String])
    case class JwtContextClaim(user: JwtUser)
    Option(verifiedClaims.getJSONObjectClaim("context"))
      .map(_.toJSONString)
      .flatMap(decode[JwtContextClaim](_).toOption)
      .map(
        context =>
          DefaultAtlassianHostUser(host,
                                   context.user.userKey,
                                   context.user.accountId))
  }

  /**
    * Creates Atlassian host user from subject claim. The resulting host user will
    * only have a userAccountId but no more userKey.
    *
    * This mechanism kicks in at the latest after March 29, 2019 or before if GDPR APIs have been enabled.
    *
    * @param host Atlassian host user involved in this request
    * @param verifiedClaims Verified JWT claims for this request
    * @return Atlassian host user created from subject claim.
    */
  private def hostUserFromSubjectClaim(
      host: AtlassianHost,
      verifiedClaims: JWTClaimsSet
  ): AtlassianHostUser = {
    DefaultAtlassianHostUser(host, None, Option(verifiedClaims.getSubject))
  }

  private def parseJwt(rawJwt: String): Either[JwtAuthenticationError, Jwt] =
    JwtParser.parse(rawJwt).leftMap { e =>
      logger.error(s"Parsing of JWT failed: $e")
      InvalidJwtError(e.getMessage())
    }

  private def extractClientKey(
      jwt: Jwt): Either[JwtAuthenticationError, String] = {
    Option(jwt.claims.getIssuer) match {
      case Some(clientKeyClaim) => Right(clientKeyClaim)
      case None =>
        Left(
          JwtBadCredentialsError("Missing client key claim for Atlassian token")
        )
    }
  }

  private def fetchAtlassianHost(clientKey: String)
    : EitherT[Future, JwtAuthenticationError, AtlassianHost] = {
    EitherT[Future, JwtAuthenticationError, AtlassianHost](
      hostRepository.findByClientKey(clientKey).map {
        case Some(host) => Right(host)
        case None =>
          logger.error(
            s"Could not find an installed host for the provided client key: $clientKey")
          Left(UnknownJwtIssuerError(clientKey))
      }
    )
  }

  private def verifyJwt(
      jwtCredentials: JwtCredentials,
      host: AtlassianHost): Either[JwtAuthenticationError, Jwt] = {
    val qsh = HttpRequestCanonicalizer.computeCanonicalRequestHash(
      jwtCredentials.canonicalHttpRequest)

    JwtReader(host.sharedSecret)
      .readAndVerify(jwtCredentials.rawJwt, qsh)
      .leftMap { e =>
        logger.error(s"Reading and validating of JWT failed: $e")
        InvalidJwtError(e.getMessage)
      }
  }
}
