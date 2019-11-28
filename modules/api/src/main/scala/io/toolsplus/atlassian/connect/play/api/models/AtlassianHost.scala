package io.toolsplus.atlassian.connect.play.api.models

import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey

/** An Atlassian host in which the add-on is or has been installed. Hosts are
  * stored in AtlassianHostRepository.
  *
  * During processing of a request from an Atlassian host, the details of the
  * host and of the user at the browser can be obtained from the [[AtlassianHostUser]].
  */
trait AtlassianHost {

  /** Identifying key for the Atlassian product
    * instance that the add-on was installed into.
    *
    * @return Client key for this product instance.
    */
  def clientKey: ClientKey

  /** Add-on key that was installed into the
    * Atlassian product, as it appears in your
    * add-on's descriptor.
    *
    * @return Add-on key for this add-on.
    */
  def key: String

  /** Public key for this Atlassian product
    * instance.
    *
    * @return Public key for this product instance.
    */
  def publicKey: String

  /** OAuth 2.0 client ID for the add-on used
    * for OAuth 2.0 - JWT Bearer token
    * authorization grant type.
    *
    * @return OAuth 2.0 client id.
    */
  def oauthClientId: Option[String]

  /** Secret to sign outgoing JWT tokens and
    * validate incoming JWT tokens. Only sent
    * on the installed event.
    *
    * @return Shared secret for this product instance.
    */
  def sharedSecret: String

  /** Host product's version.
    *
    * @return Version of this product instance.
    */
  def serverVersion: String

  /** Semver compliant version of Atlassian
    * Connect which is running on the host server.
    *
    * @return Version of Atlassian Connect on this product instance.
    */
  def pluginsVersion: String

  /** URL prefix for this Atlassian product
    * instance.
    *
    * @return Base URL for this product instance.
    */
  def baseUrl: String

  /** URL through which users will access the product. This is either the
    * custom domain set by an admin, or if not present baseUrl should be
    * used.
    *
    * @return Custom domain URL for this product instance.
    */
  def displayUrl: Option[String]

  /** URL through which users will access the Jira Service Desk Help Center. This is either the
    * custom domain set by an admin, or if not present baseUrl should be
    * used.
    *
    * @return Custom domain URL for the Jira Service Desk Help Center.
    */
  def displayUrlServicedeskHelpCenter: Option[String]

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
    * add-on license id. Only included during
    * installation of a paid add-on.
    *
    * @return Add-on license id if this is a paid add-on.
    */
  def serviceEntitlementNumber: Option[String]

  /** Indicates if the add-on is currently
    * installed on the host. Upon uninstallation,
    * the value of this flag will be set to false.
    *
    * @return Installation status for this add-on.
    */
  def installed: Boolean

  /** Uninstalls this host by setting the installed field to false.
    *
    * @return A new version of this host with the installed field set to false.
    */
  def uninstalled: AtlassianHost
}
