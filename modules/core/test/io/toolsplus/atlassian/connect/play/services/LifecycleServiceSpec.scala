package io.toolsplus.atlassian.connect.play.services

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.{
  AtlassianHost,
  DefaultAtlassianHost
}
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.models.Implicits._
import org.scalacheck.Gen._

import scala.concurrent.Future

class LifecycleServiceSpec extends TestSpec {

  val hostRepository: AtlassianHostRepository = mock[AtlassianHostRepository]

  val lifecycleService = new LifecycleService(hostRepository)

  "A LifecycleService" when {

    "asked to install any security context" should {

      "fail if event type is not 'installed'" in {
        forAll(installedEventGen, alphaStr) {
          (installedEvent, randomEventType) =>
            val invalidInstalledEvent =
              installedEvent.copy(eventType = randomEventType)
            val result = await {
              lifecycleService
                .installed(invalidInstalledEvent)
                .value
            }
            result mustBe Left(InvalidLifecycleEventTypeError)
        }
      }

    }

    "asked to install a security context from an authenticated request" should {

      "successfully install Atlassian host from security context" in {
        forAll(installedEventGen, atlassianHostUserGen) {
          (someInstalledEvent, someHostUser) =>
            val clientKey = "fake-client-key"
            val installedEvent =
              someInstalledEvent.copy(clientKey = clientKey)
            val newHost = installedEventToAtlassianHost(installedEvent)

            (hostRepository
              .save(_: AtlassianHost)) expects newHost returning Future
              .successful(newHost)

            val result = await {
              lifecycleService.installed(installedEvent).value
            }
            result mustBe Right(newHost)
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
              someHostUser.host
                .asInstanceOf[DefaultAtlassianHost]
                .copy(clientKey = clientKey, installed = true)
            val uninstalledHost = installedHost.copy(installed = false)
            val installedHostUser = someHostUser.copy(host = installedHost)

            (hostRepository
              .save(_: AtlassianHost)) expects uninstalledHost returning Future
              .successful(uninstalledHost)

            val result = await {
              lifecycleService.uninstalled(uninstalledEvent, Some(installedHostUser)).value
            }
            result mustBe Right(uninstalledHost)
        }
      }

      "fail if installed host clientKey does not match uninstall payload client key" in {
        forAll(genericEventGen, atlassianHostUserGen) {
          (genericEvent, someHostUser) =>
            val clientKey = "clientKeyA"
            val uninstalledEvent = genericEvent.copy(eventType = "uninstalled",
              clientKey = "clientKeyB")
            val installedHost =
              someHostUser.host
                .asInstanceOf[DefaultAtlassianHost]
                .copy(clientKey = clientKey, installed = true)
            val installedHostUser = someHostUser.copy(host = installedHost)

            val result = await {
              lifecycleService.uninstalled(uninstalledEvent, Some(installedHostUser)).value
            }
            result mustBe Left(HostForbiddenError)
        }
      }

      "fail if Atlassian host installation could not be found" in {
        forAll(genericEventGen, atlassianHostUserGen) {
          (genericEvent, someHostUser) =>
            val clientKey = "clientKey"
            val uninstalledEvent = genericEvent.copy(eventType = "uninstalled",
                                                     clientKey = clientKey)
            val installedHost =
              someHostUser.host
                .asInstanceOf[DefaultAtlassianHost]
                .copy(clientKey = clientKey, installed = true)
            val hostUser = someHostUser.copy(host = installedHost)

            val result = await {
              lifecycleService.uninstalled(uninstalledEvent, None).value
            }
            result mustBe Left(MissingAtlassianHostError)
        }
      }

    }

  }

}
