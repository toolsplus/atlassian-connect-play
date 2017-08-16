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
        securityContext.publicKey,
        securityContext.oauthClientId,
        securityContext.sharedSecret,
        securityContext.serverVersion,
        securityContext.pluginsVersion,
        securityContext.baseUrl,
        securityContext.productType,
        securityContext.description,
        securityContext.serviceEntitlementNumber
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
        securityContext.publicKey,
        securityContext.oauthClientId,
        securityContext.serverVersion,
        securityContext.pluginsVersion,
        securityContext.baseUrl,
        securityContext.productType,
        securityContext.description,
        securityContext.serviceEntitlementNumber
      )

}
