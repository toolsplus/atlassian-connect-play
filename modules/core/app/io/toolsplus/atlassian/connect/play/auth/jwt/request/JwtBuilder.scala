package io.toolsplus.atlassian.connect.play.auth.jwt.request

import java.time.Duration

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.MACSigner
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.{JwtJsonBuilder, JwtSigningError, JwtWriter}

/**
  * A builder of JSON Web Tokens.
  */
class JwtBuilder(expireAfter: Duration) {

  private val jwtJsonBuilder = new JwtJsonBuilder(expireAfter)

  def withIssuer(iss: String): JwtBuilder = {
    jwtJsonBuilder.withIssuer(iss)
    this
  }

  def withSubject(sub: String): JwtBuilder = {
    jwtJsonBuilder.withSubject(sub)
    this
  }

  def withAudience(aud: Seq[String]): JwtBuilder = {
    jwtJsonBuilder.withAudience(aud)
    this
  }

  def withExpirationTime(exp: Long): JwtBuilder = {
    jwtJsonBuilder.withExpirationTime(exp)
    this
  }

  def withNotBefore(nbf: Long): JwtBuilder = {
    jwtJsonBuilder.withNotBefore(nbf)
    this
  }

  def withIssuedAt(iat: Long): JwtBuilder = {
    jwtJsonBuilder.withIssuedAt(iat)
    this
  }

  def withQueryHash(queryHash: String): JwtBuilder = {
    jwtJsonBuilder.withQueryHash(queryHash)
    this
  }

  def withClaim(name: String, value: AnyRef): JwtBuilder = {
    jwtJsonBuilder.withClaim(name, value)
    this
  }

  def build(sharedSecret: String): Either[JwtSigningError, RawJwt] = {
    val jwtPayload: String = jwtJsonBuilder.build
    createJwtWriter(sharedSecret).jsonToJwt(jwtPayload)
  }

  private def createJwtWriter(sharedSecret: String): JwtWriter = {
    new JwtWriter(JWSAlgorithm.HS256, new MACSigner(sharedSecret))
  }
}
