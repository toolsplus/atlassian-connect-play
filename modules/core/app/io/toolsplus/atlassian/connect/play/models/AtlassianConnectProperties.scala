package io.toolsplus.atlassian.connect.play.models

import javax.inject.{Inject, Singleton}

import play.api.Configuration

/** Class containing Atlassian Connect properties.
  *
  * These properties allow configuration of the framework's behaviour.
  *
  * All values are read lazily from Play config files, hence they will be
  * cached for the lifetime of the Play app.
  */
@Singleton
class AtlassianConnectProperties @Inject()(config: Configuration) {

  /**
    * Expiration time for self-authentication tokens in seconds.
    */
  @deprecated(
    "SelfAuthenticationTokens are deprecated. Use AP.context.getToken() to generate tokens and sign requests from client side code.",
    "0.1.9")
  lazy val selfAuthenticationExpirationTime: Int =
    atlassianConnectConfig.get[Int]("selfAuthenticationExpirationTime")

  lazy val allowReinstallMissingHost: Boolean =
    atlassianConnectConfig.get[Boolean]("allowReinstallMissingHost")

  /**
    * Expiration time for JSON Web Tokens in seconds.
    */
  lazy val jwtExpirationTime: Int =
    atlassianConnectConfig.get[Int]("jwtExpirationTime")

  private lazy val atlassianConnectConfig =
    config.get[Configuration]("atlassian.connect")

}
