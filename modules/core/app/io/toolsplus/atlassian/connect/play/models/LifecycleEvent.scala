package io.toolsplus.atlassian.connect.play.models

/**
  * Abstract Lifecycle event of Atlassian Connect add-on.
  */
sealed trait LifecycleEvent {
  val eventType: String
  val key: String
  val clientKey: String
  val oauthClientId: Option[String]
  val baseUrl: String
  val displayUrl: String
  val displayUrlServicedeskHelpCenter: String
  val productType: String
  val description: String
  val serviceEntitlementNumber: Option[String]
  val entitlementId: Option[String]
  val entitlementNumber: Option[String]
}

/**
  * Installed lifecycle event of Atlassian Connect app.
  */
case class InstalledEvent(
    override val eventType: String,
    override val key: String,
    override val clientKey: String,
    override val oauthClientId: Option[String],
    installationId: Option[String],
    sharedSecret: String,
    override val baseUrl: String,
    override val displayUrl: String,
    override val displayUrlServicedeskHelpCenter: String,
    override val productType: String,
    override val description: String,
    override val serviceEntitlementNumber: Option[String],
    override val entitlementId: Option[String],
    override val entitlementNumber: Option[String])
    extends LifecycleEvent

/**
  * Generic lifecycle event for all lifecycle actions other than installed event.
  * This event specifically does not contain the shared secret initially
  * provided in the installed event.
  */
case class GenericEvent(override val eventType: String,
                        override val key: String,
                        override val clientKey: String,
                        override val oauthClientId: Option[String],
                        override val baseUrl: String,
                        override val displayUrl: String,
                        override val displayUrlServicedeskHelpCenter: String,
                        override val productType: String,
                        override val description: String,
                        override val serviceEntitlementNumber: Option[String],
                        override val entitlementId: Option[String],
                        override val entitlementNumber: Option[String])
    extends LifecycleEvent
