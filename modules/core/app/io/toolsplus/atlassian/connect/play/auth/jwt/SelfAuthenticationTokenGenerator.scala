package io.toolsplus.atlassian.connect.play.auth.jwt

import java.time.Duration
import java.time.temporal.ChronoUnit

import com.google.inject.Inject
import io.toolsplus.atlassian.connect.play.api.models.{
  AppProperties,
  AtlassianHostUser
}
import io.toolsplus.atlassian.connect.play.models.AtlassianConnectProperties
import io.toolsplus.atlassian.jwt.{JwtBuilder, JwtSigningError}
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt

/**
  * Generator of JSON Web Tokens for authenticating requests from the
  * add-on to itself.
  */
@deprecated(
  "SelfAuthenticationTokens are deprecated. Use AP.context.getToken() to generate tokens and sign requests from client side code.",
  "0.1.9")
class SelfAuthenticationTokenGenerator @Inject()(
    addonProperties: AppProperties,
    connectProperties: AtlassianConnectProperties) {

  def createSelfAuthenticationToken(
      hostUser: AtlassianHostUser): Either[JwtSigningError, RawJwt] = {
    val expirationAfter = Duration.of(
      connectProperties.selfAuthenticationExpirationTime,
      ChronoUnit.SECONDS)
    val jwt = new JwtBuilder(expirationAfter)
      .withIssuer(addonProperties.key)
      .withAudience(Seq(addonProperties.key))
      .withClaim(SelfAuthenticationTokenGenerator.HostClientKeyClaim,
                 hostUser.host.clientKey)
    hostUser.userKey.map(jwt.withSubject)
    jwt.build(hostUser.host.sharedSecret)
  }
}

object SelfAuthenticationTokenGenerator {

  /**
    * The name of the JWT claim used for the client key of the Atlassian host
    * in self-authentication tokens.
    */
  val HostClientKeyClaim = "clientKey"
}
