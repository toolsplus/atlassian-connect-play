package io.toolsplus.atlassian.connect.play.auth.jwt.asymmetric

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import io.toolsplus.atlassian.connect.play.api.models.{
  AppProperties,
  AtlassianHostUser
}
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.auth.jwt._
import io.toolsplus.atlassian.jwt.Jwt
import io.toolsplus.atlassian.jwt.asymmetric.AsymmetricJwtReader
import play.api.Logger

import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
  * Authentication provider that verifies asymmetrically signed JWTs.
  *
  * Note that for this authentication to succeed the Atlassian host indicated by the clientKey in the JWT does not
  * have to be installed. As a result this authentication provider may or may not return the installed Atlassian host
  * record.
  */
class AsymmetricJwtAuthenticationProvider @Inject()(
    appProperties: AppProperties,
    publicKeyProvider: PublicKeyProvider,
    hostRepository: AtlassianHostRepository)
    extends AbstractJwtAuthenticationProvider[Option](hostRepository) {

  private val logger = Logger(classOf[AsymmetricJwtAuthenticationProvider])

  /**
    * Authenticates the given JWT credentials and query string hash (QSH).
    *
    * @param jwtCredentials Untrusted JWT credentials
    * @param qsh Query string hash computed from the request the JWT credentials were attached to
    * @return Atlassian host user associated with the given JWT credentials if it is installed, or an authentication error.
    * @see https://developer.atlassian.com/cloud/jira/platform/understanding-jwt-for-connect-apps/#verifying-a-asymmetric-jwt-token-for-install-callbacks
    */
  override def authenticate(jwtCredentials: JwtCredentials, qsh: String)
    : EitherT[Future, JwtAuthenticationError, Option[AtlassianHostUser]] = {

    for {
      jwt <- parseJwt(jwtCredentials.rawJwt).toEitherT[Future]
      keyId <- extractPublicKeyId(jwt).toEitherT[Future]
      encodedPublicKey <- EitherT(publicKeyProvider.fetchPublicKey(keyId))
      publicKey <- readPublicKey(encodedPublicKey).toEitherT[Future]
      verifiedToken <- verifyJwt(jwtCredentials, publicKey, qsh)
        .toEitherT[Future]
      clientKey <- extractClientKey(jwt).toEitherT[Future]
      result <- maybeHostUser(clientKey, verifiedToken)
    } yield result
  }

  private def extractPublicKeyId(
      jwt: Jwt): Either[JwtAuthenticationError, String] =
    Option(jwt.jwsObject.getHeader.getKeyID) match {
      case Some(keyId) => Right(keyId)
      case None =>
        Left(JwtBadCredentialsError("Missing key id (kid) in token header"))
    }

  /**
    * Tries to read the given PEM encoded public key. This may fail if the
    * PEM encoded key has an invalid format.
    *
    * @see https://www.baeldung.com/java-read-pem-file-keys#1-read-public-key
    *
    * @param pemEncodedKey PEM encoded public key
    * @return Decoded RSA public key
    */
  private def readPublicKey(
      pemEncodedKey: String): Either[JwtAuthenticationError, RSAPublicKey] = {
    val startTag = "-----BEGIN PUBLIC KEY-----"
    val endTag = "-----END PUBLIC KEY-----"
    val base64 = pemEncodedKey
      .replace(startTag, "")
      .replace(endTag, "")
      .replaceAll("\\R", "")
    Try {
      val bytes = Base64.getDecoder.decode(base64)
      val keyFactory = KeyFactory.getInstance("RSA")
      val keySpec = new X509EncodedKeySpec(bytes)
      keyFactory.generatePublic(keySpec).asInstanceOf[RSAPublicKey]
    }.toEither.leftMap { e =>
      logger.error(
        s"Failed to read PEM encoded public key associated with JWT (pemEncodedKey: $pemEncodedKey): $e")
      JwtBadCredentialsError("Failed to read PEM encoded public key")
    }
  }

  private def verifyJwt(jwtCredentials: JwtCredentials,
                        publicKey: RSAPublicKey,
                        qsh: String): Either[JwtAuthenticationError, Jwt] = {
    AsymmetricJwtReader(publicKey, appProperties.baseUrl)
      .readAndVerify(jwtCredentials.rawJwt, qsh)
      .leftMap { e =>
        logger.error(s"Reading and validating of JWT failed: $e")
        InvalidJwtError(e.getMessage)
      }
  }

  /**
    * Tries to find an Atlassian host for the given client key and constructs a
    * host user record if a host has been found.
    *
    * @param clientKey Client key of the Atlassian host to find
    * @param verifiedToken Verified JWT claims to extract the subject claim from (subject claim == user account id)
    * @return Atlassian host user if one has been found
    */
  private def maybeHostUser(clientKey: String, verifiedToken: Jwt)
    : EitherT[Future, JwtAuthenticationError, Option[AtlassianHostUser]] =
    fetchAtlassianHost(clientKey).transform {
      case Right(host) => Right(Some(hostUserFromSubjectClaim(host, verifiedToken.claims)))
      case Left(_) => Right(None)
    }

}
