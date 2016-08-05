package io.toolsplus.atlassian.connect.play.models

/**
  * Abstract Lifecycle event of Atlassian Connect add-on.
  */
sealed trait LifecycleEvent {
  val eventType: String
  val key: String
  val clientKey: String
  val publicKey: String
  val oauthClientId: Option[String]
  val serverVersion: String
  val pluginsVersion: String
  val baseUrl: String
  val productType: String
  val description: String
  val serviceEntitlementNumber: Option[String]
}

/**
  * Installed lifecycle event of Atlassian Connect add-on.
  *
  * @param eventType                Lifecycle event type
  * @param key                      Add-on key that was installed into the
  *                                 Atlassian Product, as it appears in your
  *                                 add-on's descriptor.
  * @param clientKey                Identifying key for the Atlassian product
  *                                 instance that the add-on was installed into.
  * @param publicKey                Public key for this Atlassian product
  *                                 instance.
  * @param oauthClientId            OAuth 2.0 client ID for the add-on used
  *                                 for OAuth 2.0 - JWT Bearer token
  *                                 authorization grant type.
  * @param sharedSecret             Secret to sign outgoing JWT tokens and
  *                                 validate incoming JWT tokens. Only sent
  *                                 on the installed event.
  * @param serverVersion            Host product's version.
  * @param pluginsVersion           Semver compliant version of Atlassian
  *                                 Connect which is running on the host server.
  * @param baseUrl                  URL prefix for this Atlassian product
  *                                 instance.
  * @param productType              Identifies the category of Atlassian
  *                                 product, e.g. jira or confluence.
  * @param description              Host product description.
  * @param serviceEntitlementNumber Service entitlement number (SEN) is the
  *                                 add-on license id. Only included during
  *                                 installation of a paid add-on.
  */
case class InstalledEvent(
    override val eventType: String,
    override val key: String,
    override val clientKey: String,
    override val publicKey: String,
    override val oauthClientId: Option[String],
    sharedSecret: String,
    override val serverVersion: String,
    override val pluginsVersion: String,
    override val baseUrl: String,
    override val productType: String,
    override val description: String,
    override val serviceEntitlementNumber: Option[String])
    extends LifecycleEvent

/**
  * Generic lifecycle event for all lifecycle actions other than installed event.
  * This event specifically does not contain the shared secret initially
  * provided in the installed event.
  *
  * @param eventType                Lifecycle event type
  * @param key                      Add-on key that was installed into the
  *                                 Atlassian Product, as it appears in your
  *                                 add-on's descriptor.
  * @param clientKey                Identifying key for the Atlassian product
  *                                 instance that the add-on was installed into.
  * @param publicKey                Public key for this Atlassian product
  *                                 instance.
  * @param oauthClientId            OAuth 2.0 client ID for the add-on used
  *                                 for OAuth 2.0 - JWT Bearer token
  *                                 authorization grant type.
  * @param serverVersion            Host product's version.
  * @param pluginsVersion           Semver compliant version of Atlassian
  *                                 Connect which is running on the host server.
  * @param baseUrl                  URL prefix for this Atlassian product
  *                                 instance.
  * @param productType              Identifies the category of Atlassian
  *                                 product, e.g. jira or confluence.
  * @param description              Host product description.
  * @param serviceEntitlementNumber Service entitlement number (SEN) is the
  *                                 add-on license id. Only included during
  *                                 installation of a paid add-on.
  */
case class GenericEvent(override val eventType: String,
                        override val key: String,
                        override val clientKey: String,
                        override val publicKey: String,
                        override val oauthClientId: Option[String],
                        override val serverVersion: String,
                        override val pluginsVersion: String,
                        override val baseUrl: String,
                        override val productType: String,
                        override val description: String,
                        override val serviceEntitlementNumber: Option[String])
    extends LifecycleEvent
