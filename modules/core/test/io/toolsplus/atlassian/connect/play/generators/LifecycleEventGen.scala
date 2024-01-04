package io.toolsplus.atlassian.connect.play.generators

import io.toolsplus.atlassian.connect.play.models.{
  GenericEvent,
  InstalledEvent
}
import org.scalacheck.Gen
import org.scalacheck.Gen._

trait LifecycleEventGen extends SecurityContextGen {

  def installedEventGen: Gen[InstalledEvent] =
    for {
      eventType <- const("installed")
      securityContext <- securityContextGen
    } yield
      InstalledEvent(
        eventType,
        securityContext.key,
        securityContext.clientKey,
        securityContext.oauthClientId,
        securityContext.sharedSecret,
        securityContext.baseUrl,
        securityContext.displayUrl,
        securityContext.displayUrlServicedeskHelpCenter,
        securityContext.productType,
        securityContext.description,
        securityContext.serviceEntitlementNumber,
        securityContext.entitlementId,
        securityContext.entitlementNumber
      )

  def genericEventTypeGen: Gen[String] =
    oneOf("uninstalled", "enabled", "disabled")

  def genericEventGen: Gen[GenericEvent] =
    for {
      eventType <- genericEventTypeGen
      securityContext <- securityContextGen
    } yield
      GenericEvent(
        eventType,
        securityContext.key,
        securityContext.clientKey,
        securityContext.oauthClientId,
        securityContext.baseUrl,
        securityContext.displayUrl,
        securityContext.displayUrlServicedeskHelpCenter,
        securityContext.productType,
        securityContext.description,
        securityContext.serviceEntitlementNumber,
        securityContext.entitlementId,
        securityContext.entitlementNumber
      )

}
