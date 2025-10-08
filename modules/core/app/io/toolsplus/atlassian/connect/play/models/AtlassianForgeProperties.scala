package io.toolsplus.atlassian.connect.play.models

import play.api.Configuration

import javax.inject.{Inject, Singleton}

/** Class containing Atlassian Forge properties.
  *
  * All values are read lazily from Play config files, hence they will be
  * cached for the lifetime of the Play app.
  */
@Singleton
class AtlassianForgeProperties @Inject()(config: Configuration) {
  lazy val appId: String =
    atlassianForgeConfig.get[String]("appId")

  lazy val forgeRemoteJWKSetStagingUrl: String =
    atlassianForgeConfig.get[String]("remote.jwkSetStagingUrl")

  lazy val forgeRemoteJWKSetProductionUrl: String =
    atlassianForgeConfig.get[String]("remote.jwkSetProductionUrl")

  lazy val systemAccessTokenExpiryLeewayMs: Int =
    atlassianForgeConfig.get[Int]("systemAccessTokenExpiryLeewayMs")

  private lazy val atlassianForgeConfig =
    config.get[Configuration]("atlassian.forge")

}
