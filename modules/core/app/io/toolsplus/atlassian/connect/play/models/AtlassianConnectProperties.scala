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

  lazy val allowReinstallMissingHost: Boolean =
    atlassianConnectConfig.get[Boolean]("allowReinstallMissingHost")

  /**
    * Expiration time for JSON Web Tokens in seconds.
    */
  lazy val jwtExpirationTime: Int =
    atlassianConnectConfig.get[Int]("jwtExpirationTime")

  lazy val publicKeyHostBaseUrl: String =
    atlassianConnectConfig.get[String]("publicKeyHostBaseUrl")

  private lazy val atlassianConnectConfig =
    config.get[Configuration]("atlassian.connect")

}
