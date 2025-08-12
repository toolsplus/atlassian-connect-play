package io.toolsplus.atlassian.connect.play.models

import javax.inject.{Inject, Singleton}

import play.api.Configuration

trait AtlassianConnectProperties {

  /** Expiration time for JSON Web Tokens in seconds.
    */
  def jwtExpirationTime: Int

  def publicKeyHostBaseUrl: String

  /** Number of days to retain a host record before deletion.
    *
    * Note that by leaving this value undefined, records will not be marked with
    * a TTL. By setting this value, the framework will mark records with a TTL
    * upon uninstallation, but it will not delete the records.
    */
  def hostTTLDays: Option[Int]
}

/** Class containing Atlassian Connect properties.
  *
  * These properties allow configuration of the framework's behaviour.
  *
  * All values are read lazily from Play config files, hence they will be cached
  * for the lifetime of the Play app.
  */
@Singleton
class PlayAtlassianConnectProperties @Inject() (config: Configuration)
    extends AtlassianConnectProperties {

  /** Expiration time for JSON Web Tokens in seconds.
    */
  override lazy val jwtExpirationTime: Int =
    atlassianConnectConfig.get[Int]("jwtExpirationTime")

  override lazy val publicKeyHostBaseUrl: String =
    atlassianConnectConfig.get[String]("publicKeyHostBaseUrl")

  override lazy val hostTTLDays: Option[Int] =
    atlassianConnectConfig.get[Option[Int]]("hostTTLDays")

  private lazy val atlassianConnectConfig =
    config.get[Configuration]("atlassian.connect")

}
