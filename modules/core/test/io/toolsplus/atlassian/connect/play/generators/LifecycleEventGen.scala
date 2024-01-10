package io.toolsplus.atlassian.connect.play.generators

import io.toolsplus.atlassian.connect.play.api.models.OAuth2AuthenticationType
import io.toolsplus.atlassian.connect.play.models.{GenericEvent, InstalledEvent}
import org.scalacheck.Gen
import org.scalacheck.Gen._

trait LifecycleEventGen extends SecurityContextGen {

  def connectInstalledEventGen: Gen[InstalledEvent] =
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
        None,
        None,
        securityContext.baseUrl,
        securityContext.displayUrl,
        securityContext.displayUrlServicedeskHelpCenter,
        securityContext.productType,
        securityContext.description,
        securityContext.serviceEntitlementNumber,
        securityContext.entitlementId,
        securityContext.entitlementNumber
      )

  def connectOnForgeInstalledEventGen: Gen[InstalledEvent] =
    for {
      connectInstalledEvent <- connectInstalledEventGen
      cloudId <- alphaStr
    } yield
      connectInstalledEvent.copy(authenticationType =
                                   Some(OAuth2AuthenticationType),
                                 cloudId = Some(cloudId))

  def anyInstalledEvent: Gen[InstalledEvent] =
    oneOf(connectInstalledEventGen, connectOnForgeInstalledEventGen)

  def genericEventTypeGen: Gen[String] =
    oneOf("uninstalled", "enabled", "disabled")

  def connectGenericEventGen: Gen[GenericEvent] =
    for {
      eventType <- genericEventTypeGen
      securityContext <- securityContextGen
    } yield
      GenericEvent(
        eventType,
        securityContext.key,
        securityContext.clientKey,
        securityContext.oauthClientId,
        None,
        None,
        securityContext.baseUrl,
        securityContext.displayUrl,
        securityContext.displayUrlServicedeskHelpCenter,
        securityContext.productType,
        securityContext.description,
        securityContext.serviceEntitlementNumber,
        securityContext.entitlementId,
        securityContext.entitlementNumber
      )

  def connectOnForeGenericEventGen: Gen[GenericEvent] =
    for {
      connectGenericEvent <- connectGenericEventGen
      cloudId <- alphaStr
    } yield
      connectGenericEvent.copy(authenticationType =
                                 Some(OAuth2AuthenticationType),
                               cloudId = Some(cloudId))

  def anyGenericEvent: Gen[GenericEvent] =
    oneOf(connectGenericEventGen, connectOnForeGenericEventGen)

}
