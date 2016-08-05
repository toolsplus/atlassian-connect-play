package io.toolsplus.atlassian.connect.play.models

import javax.inject.{Inject, Singleton}

import io.toolsplus.atlassian.connect.play.api.models.Predefined.AddonKey
import play.api.Configuration

/** Class containing add-on properties such as add-on key, name, base url,
  * etc. by reading them lazily from a Play configuration.
  *
  * All values are read lazily from Play config files, hence they will be
  * cached for the lifetime of the Play app.
  */
@Singleton
class AddonProperties @Inject()(config: Configuration) {

  /** Key of this add-on. */
  lazy val key: AddonKey = atlassianConnectConfig.getString("key").get

  /** Name of this add-on. */
  lazy val name: String = atlassianConnectConfig.getString("name").get

  /** Base URL of this add-on. */
  lazy val baseUrl: String = atlassianConnectConfig.getString("baseUrl").get

  /** Flag determining whether Atlassian Connect license checking should be
    * enabled or not.
    *
    * In production this flag should be always enabled. */
  lazy val licenceCheck: Boolean =
    atlassianConnectConfig.getBoolean("licenseCheck").get

  /**
    * Expiration time for self-authentication tokens in seconds.
    */
  lazy val selfAuthenticationExpirationTime: Int =
    atlassianConnectConfig.getInt("selfAuthenticationExpirationTime").get

  lazy val allowReinstallMissingHost: Boolean =
    atlassianConnectConfig.getBoolean("allowReinstallMissingHost").get

  private lazy val atlassianConnectConfig =
    config.getConfig("atlassian.connect").get

}
