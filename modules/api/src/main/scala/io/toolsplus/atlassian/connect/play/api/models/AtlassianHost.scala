package io.toolsplus.atlassian.connect.play.api.models

import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey

/** An Atlassian host in which the app is or has been installed. Hosts are
  * stored in AtlassianHostRepository.
  *
  * During processing of a request from an Atlassian host, the details of the
  * host and of the user at the browser can be obtained from the [[AtlassianHostUser]].
  *
  * @see https://developer.atlassian.com/cloud/jira/platform/connect-app-descriptor/#lifecycle-http-request-payload
  */
trait AtlassianHost {

  /** Identifying key for the Atlassian product
    * instance that the app was installed into.
    *
    * @return Client key for this product instance.
    */
  def clientKey: ClientKey

  /** App key that was installed into the
    * Atlassian product, as it appears in your
    * app's descriptor.
    *
    * @return App key for this app.
    */
  def key: String

  /** OAuth 2.0 client ID for the app used
    * for OAuth 2.0 - JWT Bearer token
    * authorization grant type.
    *
    * @return OAuth 2.0 client id.
    */
  def oauthClientId: Option[String]

  /**
    * Identifier for this host's Forge installation, if the host has migrated to Connect on Forge.
    *
    * This is also the ID that any remote storage should be keyed against.
    *
    * @return Forge installation id.
    */
  def installationId: Option[String]

  /** Secret to sign outgoing JWT tokens and
    * validate incoming JWT tokens. Only sent
    * on the installed event.
    *
    * @return Shared secret for this product instance.
    */
  def sharedSecret: String

  /** URL prefix for this Atlassian product
    * instance.
    *
    * @return Base URL for this product instance.
    */
  def baseUrl: String

  /**
    * If the Atlassian product instance has an associated custom domain, this is the URL through which users will
    * access the product. Any links which an app renders server-side should use this as the prefix of the link.
    * This ensures links are rendered in the same context as the remainder of the user's site. If a custom domain
    * is not configured, this field will still be present but will be the same as the baseUrl.
    *
    * Note that API requests from your app should always use the baseUrl value.
    *
    * @return Custom domain URL if configured, app's base URL otherwise.
    */
  def displayUrl: String

  /**
    * If the Atlassian product instance has an associated custom domain for Jira Service Desk functionality, this is
    * the URL for the Jira Service Desk Help Center. Any related links which an app renders server-side should use this
    * as the prefix of the link.
    * This ensures links are rendered in the same context as the user's Jira Service Desk. If a custom domain is not
    * configured, this field will still be present but will be the same as the baseUrl.
    *
    * Note that API requests from your App should always use the baseUrl value.
    *
    * @return Custom domain URL for Jira Service Desk if configured, app's base URL otherwise.
    */
  def displayUrlServicedeskHelpCenter: String

  /** Identifies the category of Atlassian
    * product, e.g. jira or confluence.
    *
    * @return Category of this product.
    */
  def productType: String

  /** Host product description.
    *
    * @return Description of this product.
    */
  def description: String

  /** Service entitlement number (SEN) is the
    * app license id. Only included during
    * installation of a paid app.
    *
    * @return App license id if this is a paid app.
    */
  @deprecated("No replacement")
  def serviceEntitlementNumber: Option[String]

  /**
    * License entitlement ID is the app license ID.
    *
    * This attribute will only be included during installation of a paid app.
    *
    * @return App entitlement ID if this is a paid app.
    */
  def entitlementId: Option[String]

  /**
    * License entitlement number is the app license number.
    *
    * This attribute will only be included during installation of a paid app.
    *
    * @return App entitlement number if this is a paid app.
    */
  def entitlementNumber: Option[String]

  /** Indicates if the app is currently
    * installed on the host. Upon uninstallation,
    * the value of this flag will be set to false.
    *
    * @return Installation status for this app.
    */
  def installed: Boolean

  /** Uninstalls this host by setting the installed field to false.
    *
    * @return A new version of this host with the installed field set to false.
    */
  def uninstalled: AtlassianHost
}
