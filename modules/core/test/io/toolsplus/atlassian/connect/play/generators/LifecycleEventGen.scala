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
        securityContext._1,
        securityContext._2,
        securityContext._3,
        securityContext._4,
        securityContext._5,
        securityContext._6,
        securityContext._7,
        securityContext._8,
        securityContext._9,
        securityContext._10,
        securityContext._11
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
        securityContext._1,
        securityContext._2,
        securityContext._3,
        securityContext._4,
        securityContext._6,
        securityContext._7,
        securityContext._8,
        securityContext._9,
        securityContext._10,
        securityContext._11
      )

}
