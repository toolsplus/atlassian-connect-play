package io.toolsplus.atlassian.connect.play.api.models

import java.time.Instant

/**
  * Forge system access token
  *
  * @see https://developer.atlassian.com/platform/forge/remote/essentials/
  */
trait ForgeSystemAccessToken {

  /**
    * Unique identifier of a Forge installation in ARI format, e.g. ari:cloud:ecosystem::installation/c3658f0f-8380-41e5-bb1e-68903f8efdca.
    *
    * This ID is stable across upgrades but not uninstallation and re-installation. This value should be used to key
    * Forge-related tenant details in the app.
    *
    * @return Forge installation id.
    */
  def installationId: String

  /**
    * OAuth2 API base URL in the format https://api.atlassian.com/ex/confluence/00000000-0000-0000-0000-000000000000
    * @return OAuth2 API base URL
    */
  def apiBaseUrl: String

  /**
    * System access token this entity represents.
    * @return System access token
    */
  def accessToken: String

  /**
    * Timestamp when this token expires.
    * @return Access token expiry time.
    */
  def expirationTime: Instant
}
