package io.toolsplus.atlassian.connect.play.generators

import com.nimbusds.jose.JWSAlgorithm
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.auth.jwt
import io.toolsplus.atlassian.connect.play.auth.jwt.JwtCredentials
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.generators.core.JwtGen
import io.toolsplus.atlassian.jwt.generators.nimbus.NimbusGen
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import org.scalacheck.Gen

import java.security.PrivateKey

trait AtlassianConnectJwtGen extends JwtGen with NimbusGen {

  def signedAsymmetricJwtStringGen(
      keyId: String,
      privateKey: PrivateKey,
      customClaims: Seq[(String, Any)]): Gen[RawJwt] = {
    signedAsymmetricJwtStringGen(keyId, privateKey, customClaims, JWSAlgorithm.RS256)
  }

  def signedSymmetricJwtStringGen(host: AtlassianHost,
                                  subject: String): Gen[RawJwt] = {
    val customClaims = Seq("iss" -> host.clientKey, "sub" -> subject)
    signedSymmetricJwtStringGen(host.sharedSecret, customClaims)
  }

  def jwtCredentialsGen(rawJwt: String): Gen[JwtCredentials] =
    for {
      canonicalHttpRequest <- canonicalHttpRequestGen
    } yield jwt.JwtCredentials(rawJwt, canonicalHttpRequest)

  /**
    * Generates symmetrically signed JWT credentials.
    *
    * @param secret Shared signing secret
    * @param customClaims Custom claims
    * @return Symmetrically signed JWT credentials
    */
  def symmetricJwtCredentialsGen(
      secret: String = JwtTestHelper.defaultSigningSecret,
      customClaims: Seq[(String, Any)] = Seq.empty): Gen[JwtCredentials] =
    for {
      rawJwt <- signedSymmetricJwtStringGen(secret, customClaims)
      credentials <- jwtCredentialsGen(rawJwt)
    } yield credentials

  /**
    * Generates symmetrically signed JWT credentials.
    *
    * @param host Atlassian host with which to populate and sign the JWT
    * @param subject JWT subject claim
    * @return Symmetrically signed JWT credentials
    */
  def symmetricJwtCredentialsGen(host: AtlassianHost,
                                 subject: String): Gen[JwtCredentials] =
    for {
      rawJwt <- signedSymmetricJwtStringGen(host, subject)
      credentials <- jwtCredentialsGen(rawJwt)
    } yield credentials

  /**
    * Generates asymmetrically signed JWT credentials.
    *
    * @param keyId Public key id of the JWT
    * @param privateKey Private key to sign the JWT
    * @param customClaims Custom claims
    * @return Asymmetrically signed JWT credentials
    */
  def asymmetricJwtCredentialsGen(
      keyId: String,
      privateKey: PrivateKey,
      customClaims: Seq[(String, Any)]): Gen[JwtCredentials] =
    for {
      rawJwt <- signedAsymmetricJwtStringGen(keyId, privateKey, customClaims)
      credentials <- jwtCredentialsGen(rawJwt)
    } yield credentials
}
