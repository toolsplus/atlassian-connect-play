package io.toolsplus.atlassian.connect.play.generators

import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.api.models.Predefined.AppKey
import io.toolsplus.atlassian.connect.play.auth.jwt.{
  JwtCredentials,
  SelfAuthenticationTokenGenerator
}
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.generators.core.JwtGen
import io.toolsplus.atlassian.jwt.generators.nimbus.NimbusGen
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import org.scalacheck.Gen

import scala.collection.JavaConverters._

trait AtlassianConnectJwtGen extends JwtGen with NimbusGen {

  def signedJwtStringGen(host: AtlassianHost, subject: String): Gen[RawJwt] = {
    val customClaims = Seq("iss" -> host.clientKey, "sub" -> subject)
    signedJwtStringGen(host.sharedSecret, customClaims)
  }

  def selfAuthenticatedJwtCredentialsGen(addonKey: AppKey,
                                         host: AtlassianHost,
                                         customClaims: Seq[(String, Any)] =
                                           Seq.empty): Gen[JwtCredentials] = {
    val claims = Seq(
      "iss" -> addonKey,
      "aud" -> Seq(addonKey).asJava,
      SelfAuthenticationTokenGenerator.HostClientKeyClaim -> host.clientKey
    ) ++ customClaims

    for {
      rawJwt <- signedJwtStringGen(host.sharedSecret, claims)
      canonicalHttpRequest <- canonicalHttpRequestGen
    } yield JwtCredentials(rawJwt, canonicalHttpRequest)
  }

  def jwtCredentialsGen(rawJwt: String): Gen[JwtCredentials] =
    for {
      canonicalHttpRequest <- canonicalHttpRequestGen
    } yield JwtCredentials(rawJwt, canonicalHttpRequest)

  def jwtCredentialsGen(
      secret: String = JwtTestHelper.defaultSigningSecret,
      customClaims: Seq[(String, Any)] = Seq.empty): Gen[JwtCredentials] =
    for {
      rawJwt <- signedJwtStringGen(secret, customClaims)
      credentials <- jwtCredentialsGen(rawJwt)
    } yield credentials

  def jwtCredentialsGen(host: AtlassianHost,
                        subject: String): Gen[JwtCredentials] =
    for {
      rawJwt <- signedJwtStringGen(host, subject)
      credentials <- jwtCredentialsGen(rawJwt)
    } yield credentials
}
