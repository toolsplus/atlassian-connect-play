package io.toolsplus.atlassian.connect.play.services

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.models.Implicits._
import org.scalacheck.Gen._

import scala.concurrent.Future

class LifecycleServiceSpec extends TestSpec {

  val hostRepository = mock[AtlassianHostRepository]

  val $ = new LifecycleService(hostRepository)

  "A LifecycleService" when {

    "asked to install any security context" should {

      "fail if event type is not 'installed'" in {
        forAll(installedEventGen, maybeAtlassianHostUserGen, alphaStr) {
          (installedEvent, maybeHostUser, randomEventType) =>
            val invalidInstalledEvent =
              installedEvent.copy(eventType = randomEventType)
            val result = await {
              $.installed(invalidInstalledEvent)(maybeHostUser).value
            }
            result mustBe Left(InvalidLifecycleEventTypeError)
        }
      }

    }

    "asked to install a security context from an authenticated request" should {

      "successfully install Atlassian host from security context" in {
        forAll(installedEventGen, atlassianHostUserGen) {
          (someInstalledEvent, someHostUser) =>
            val installedEvent =
              someInstalledEvent.copy(clientKey = "clientKeyA")
            val hostUser = someHostUser.copy(
              host = someHostUser.host.copy(clientKey = "clientKeyA"))
            val newHost = installedEventToAtlassianHost(installedEvent)

            (hostRepository
              .save(_: AtlassianHost)) expects newHost returning Future
              .successful(newHost)

            val result = await {
              $.installed(installedEvent)(Some(hostUser)).value
            }
            result mustBe Right(newHost)
        }
      }

      "fail if AtlassianHostUser.clientKey does not match InstalledEvent.clientKey" in {
        forAll(installedEventGen, atlassianHostUserGen) {
          (installedEvent, hostUser) =>
            val clientKeyMismatchEvent =
              installedEvent.copy(clientKey = "clientKeyA")
            val clientKeyMismatchHostUser = hostUser.copy(
              host = hostUser.host.copy(clientKey = "clientKeyB"))

            val result = await {
              $.installed(clientKeyMismatchEvent)(
                Some(clientKeyMismatchHostUser)).value
            }
            result mustBe Left(HostForbiddenError)
        }
      }

    }

    "asked to install a security context from an unauthenticated request" should {

      "successfully install Atlassian host from security context" in {
        forAll(installedEventGen) { (installedEvent) =>
          val newHost = installedEventToAtlassianHost(installedEvent)

          (hostRepository
            .findByClientKey(_: ClientKey)) expects installedEvent.clientKey returning Future
            .successful(None)

          (hostRepository
            .save(_: AtlassianHost)) expects newHost returning Future
            .successful(newHost)

          val result = await {
            $.installed(installedEvent)(None).value
          }
          result mustBe Right(newHost)
        }
      }

      "fail if there is an existing host for the given 'installed' event" in {
        forAll(installedEventGen, atlassianHostGen) { (installedEvent, host) =>
          val hostForEvent = host.copy(clientKey = installedEvent.clientKey)

          (hostRepository
            .findByClientKey(_: ClientKey)) expects installedEvent.clientKey returning Future
            .successful(Some(hostForEvent))

          val result = await {
            $.installed(installedEvent)(None).value
          }
          result mustBe Left(MissingJwtError)
        }
      }

    }

    "asked to uninstall a host from an authenticated request" should {

      "successfully mark Atlassian host as uninstalled" in {
        forAll(genericEventGen, atlassianHostUserGen) {
          (genericEvent, someHostUser) =>
            val clientKey = "clientKey"
            val uninstalledEvent = genericEvent.copy(eventType = "uninstalled",
                                                     clientKey = clientKey)
            val installedHost =
              someHostUser.host.copy(clientKey = clientKey, installed = true)
            val uninstalledHost = installedHost.copy(installed = false)
            val hostUser = someHostUser.copy(host = installedHost)

            (hostRepository
              .findByClientKey(_: ClientKey)) expects uninstalledEvent.clientKey returning Future
              .successful(Some(installedHost))

            (hostRepository
              .save(_: AtlassianHost)) expects uninstalledHost returning Future
              .successful(uninstalledHost)

            val result = await {
              $.uninstalled(uninstalledEvent)(hostUser).value
            }
            result mustBe Right(uninstalledHost)
        }
      }

      "fail if Atlassian host installation could not be found" in {
        forAll(genericEventGen, atlassianHostUserGen) {
          (genericEvent, someHostUser) =>
            val clientKey = "clientKey"
            val uninstalledEvent = genericEvent.copy(eventType = "uninstalled",
                                                     clientKey = clientKey)
            val installedHost =
              someHostUser.host.copy(clientKey = clientKey, installed = true)
            val hostUser = someHostUser.copy(host = installedHost)

            (hostRepository
              .findByClientKey(_: ClientKey)) expects uninstalledEvent.clientKey returning Future
              .successful(None)

            val result = await {
              $.uninstalled(uninstalledEvent)(hostUser).value
            }
            result mustBe Left(MissingAtlassianHostError)
        }
      }

    }

  }

}
