package io.toolsplus.atlassian.connect.play.services

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.{
  AtlassianHost,
  DefaultAtlassianHost,
  DefaultForgeInstallation,
  ForgeInstallation
}
import io.toolsplus.atlassian.connect.play.api.repositories.{
  AtlassianHostRepository,
  ForgeInstallationRepository
}
import io.toolsplus.atlassian.connect.play.models.AtlassianConnectProperties
import io.toolsplus.atlassian.connect.play.models.Implicits._
import org.scalacheck.Gen._

import java.time.{Duration, Instant}
import scala.concurrent.Future

class LifecycleServiceSpec extends TestSpec {

  val hostRepository: AtlassianHostRepository = mock[AtlassianHostRepository]
  val forgeInstallationRepository: ForgeInstallationRepository =
    mock[ForgeInstallationRepository]
  val connectProperties: AtlassianConnectProperties =
    mock[AtlassianConnectProperties]

  val lifecycleService =
    new LifecycleService(
      hostRepository,
      forgeInstallationRepository,
      connectProperties
    )

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
        forAll(installedEventGen) { someInstalledEvent =>
          val clientKey = "fake-client-key"
          val installedEvent =
            someInstalledEvent.copy(clientKey = clientKey)
          val newHost = installedEventToAtlassianHost(installedEvent)
          val maybeForgeInstallation = newHost.installationId.map(
            DefaultForgeInstallation(_, newHost.clientKey)
          )

          maybeForgeInstallation match {
            case Some(installation) =>
              (forgeInstallationRepository
                .save(
                  _: ForgeInstallation
                )) expects installation returning Future
                .successful(installation)
            case None =>
              (forgeInstallationRepository
                .deleteByClientKey(
                  _: String
                )) expects newHost.clientKey returning Future
                .successful(0)
          }

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

      "successfully mark Atlassian host as uninstalled with TTL" in {
        val ttlDays = 30
        forAll(genericEventGen, atlassianHostUserGen) {
          (genericEvent, someHostUser) =>
            val clientKey = "clientKey"
            val uninstalledEvent = genericEvent.copy(
              eventType = "uninstalled",
              clientKey = clientKey
            )
            val installedHost =
              someHostUser.host
                .asInstanceOf[DefaultAtlassianHost]
                .copy(clientKey = clientKey, installed = true)
            val ttl = Instant.now().plus(Duration.ofDays(ttlDays))
            val uninstalledHost =
              installedHost.copy(installed = false, ttl = Some(ttl))
            val installedHostUser = someHostUser.copy(host = installedHost)

            (() => connectProperties.hostTTLDays)
              .expects()
              .returning(Some(ttlDays))

            (hostRepository
              .save(_: AtlassianHost))
              .expects(where { host: AtlassianHost =>
                // since the created host has a slightly different TTL,
                // we cannot match against `uninstalledHost` directly
                !host.installed && host.ttl.isDefined
              })
              .returning(
                Future
                  .successful(uninstalledHost)
              )

            val result = await {
              lifecycleService
                .uninstalled(uninstalledEvent, Some(installedHostUser))
                .value
            }
            result mustBe Right(uninstalledHost)
        }
      }

      "successfully mark Atlassian host as uninstalled without TTL" in {
        forAll(genericEventGen, atlassianHostUserGen) {
          (genericEvent, someHostUser) =>
            val clientKey = "clientKey"
            val uninstalledEvent = genericEvent.copy(
              eventType = "uninstalled",
              clientKey = clientKey
            )
            val installedHost =
              someHostUser.host
                .asInstanceOf[DefaultAtlassianHost]
                .copy(clientKey = clientKey, installed = true)
            val uninstalledHost =
              installedHost.uninstalled(ttl = None)
            val installedHostUser = someHostUser.copy(host = installedHost)

            (() => connectProperties.hostTTLDays).expects().returning(None)

            (hostRepository
              .save(_: AtlassianHost)) expects uninstalledHost returning Future
              .successful(uninstalledHost)

            val result = await {
              lifecycleService
                .uninstalled(uninstalledEvent, Some(installedHostUser))
                .value
            }
            result mustBe Right(uninstalledHost)
        }
      }

      "fail if installed host clientKey does not match uninstall payload client key" in {
        forAll(genericEventGen, atlassianHostUserGen) {
          (genericEvent, someHostUser) =>
            val clientKey = "clientKeyA"
            val uninstalledEvent = genericEvent.copy(
              eventType = "uninstalled",
              clientKey = "clientKeyB"
            )
            val installedHost =
              someHostUser.host
                .asInstanceOf[DefaultAtlassianHost]
                .copy(clientKey = clientKey, installed = true)
            val installedHostUser = someHostUser.copy(host = installedHost)

            (() => connectProperties.hostTTLDays).expects().returning(None)

            val result = await {
              lifecycleService
                .uninstalled(uninstalledEvent, Some(installedHostUser))
                .value
            }
            result mustBe Left(HostForbiddenError)
        }
      }

      "fail if Atlassian host installation could not be found" in {
        forAll(genericEventGen) { genericEvent =>
          val clientKey = "clientKey"
          val uninstalledEvent =
            genericEvent.copy(eventType = "uninstalled", clientKey = clientKey)
          val result = await {
            lifecycleService.uninstalled(uninstalledEvent, None).value
          }
          result mustBe Left(MissingAtlassianHostError)
        }
      }

    }

  }

}
