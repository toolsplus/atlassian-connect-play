package io.toolsplus.atlassian.connect.play.services

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import io.toolsplus.atlassian.connect.play.api.events.{
  AppInstalledEvent,
  AppUninstalledEvent
}
import io.toolsplus.atlassian.connect.play.api.models.{
  AtlassianHost,
  AtlassianHostUser,
  DefaultForgeInstallation
}
import io.toolsplus.atlassian.connect.play.api.repositories.{
  AtlassianHostRepository,
  ForgeInstallationRepository
}
import io.toolsplus.atlassian.connect.play.events.EventBus
import io.toolsplus.atlassian.connect.play.models.Implicits._
import io.toolsplus.atlassian.connect.play.models._
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.chaining._

class LifecycleService @Inject()(
    hostRepository: AtlassianHostRepository,
    forgeInstallationRepository: ForgeInstallationRepository) {

  private val logger = Logger(classOf[LifecycleService])

  /** Installs the given Atlassian host by saving the security context to the
    * database.
    *
    * @param installedEvent         Lifecycle event holding the security context
    *                               provided by Atlassian product.
    * @return Installed AtlassianHost instance.
    */
  def installed(installedEvent: InstalledEvent)
    : EitherT[Future, LifecycleError, AtlassianHost] =
    for {
      _ <- assertLifecycleEventType(installedEvent, "installed")
        .toEitherT[Future]
      host <- EitherT.right(install(installedEvent))
      _ = EventBus.publish(AppInstalledEvent(host))
    } yield host

  /** Extracts the Atlassian host from the installed event and saves it to the database.
    *
    * @param installedEvent Lifecycle event holding the security context
    *                       provided by Atlassian product.
    * @return Either the newly installed [[AtlassianHost]] or a [[HostForbiddenError]]
    */
  private def install(installedEvent: InstalledEvent): Future[AtlassianHost] =
    for {
      savedHost <- hostRepository.save(
        installedEventToAtlassianHost(installedEvent))
      _ <- Future.successful(logger.info(
        s"Saved installation for host ${savedHost.baseUrl} (${savedHost.clientKey})"))
      _ <- savedHost.installationId match {
        case Some(installationId) =>
          forgeInstallationRepository
            .save(DefaultForgeInstallation(installationId, savedHost.clientKey))
            .map(_.tap(_ =>
              logger.info(
                s"Saved Forge installation for host ${savedHost.baseUrl} (${savedHost.clientKey}, ${savedHost.installationId})")))
            .recover { error =>
              logger.error(
                s"Failed to save Forge installation for host ${savedHost.baseUrl} (${savedHost.clientKey}, ${savedHost.installationId}): $error")
            }
        case None =>
          forgeInstallationRepository
            .deleteByClientKey(savedHost.clientKey)
            .map(_.tap(deleteCount =>
              if (deleteCount > 0)
                logger.info(
                  s"Removed old Forge installations for host ${savedHost.baseUrl} (${savedHost.clientKey})")))
            .recover { error =>
              logger.error(
                s"Failed to remove old Forge installation for host ${savedHost.baseUrl} (${savedHost.clientKey}, ${savedHost.installationId}): $error")
            }
      }
    } yield savedHost

  /** Uninstalls the given Atlassian host by setting the installed flag in
    * the host store to false.
    *
    * If there is no Atlassian host for the given lifecycle event installed
    * [[MissingAtlassianHostError]] is returned. This error however can be
    * treated as successful uninstallation as the host is not there anyways.
    *
    * @param uninstalledEvent Lifecycle event payload.
    * @param maybeHostUser    Installed Atlassian host user if one has been found.
    * @return Either [[LifecycleError]] or AtlassianHost.
    */
  def uninstalled(uninstalledEvent: GenericEvent,
                  maybeHostUser: Option[AtlassianHostUser])
    : EitherT[Future, LifecycleError, AtlassianHost] =
    for {
      _ <- assertLifecycleEventType(uninstalledEvent, "uninstalled")
        .toEitherT[Future]
      host <- uninstall(uninstalledEvent, maybeHostUser.map(_.host))
      _ = EventBus.publish(AppUninstalledEvent(host))
    } yield host

  /**
    * Uninstalls the Atlassian host indicated by the uninstall event payload.
    *
    * Note that if there is an installed host record found, the host client key in the given payload will be verified
    * against the installed host client key. Uninstallation will only succeed if the two client key values match.
    *
    * @param uninstalledEvent Uninstall event payload
    * @param maybeExistingHost Atlassian host associated with the JWT request if one has been found
    * @return
    */
  private def uninstall(uninstalledEvent: GenericEvent,
                        maybeExistingHost: Option[AtlassianHost])
    : EitherT[Future, LifecycleError, AtlassianHost] = {
    maybeExistingHost match {
      case Some(host) =>
        logger.info(
          s"Saved uninstallation for host ${host.baseUrl} (${host.clientKey})")
        assertHostAuthorized(uninstalledEvent, host)
          .toEitherT[Future]
          .flatMap(host => EitherT.right(hostRepository.save(host.uninstalled)))
      case None =>
        logger.error(
          s"Received authenticated uninstall request but no installation for host ${uninstalledEvent.baseUrl} has been found. Assume the app has been removed.")
        EitherT.left(
          Future.successful[LifecycleError](MissingAtlassianHostError))
    }
  }

  private def assertLifecycleEventType(
      lifecycleEvent: LifecycleEvent,
      expectedEventType: String): Either[LifecycleError, LifecycleEvent] = {
    val eventType = lifecycleEvent.eventType
    if (expectedEventType != eventType) {
      logger.error(
        s"Received lifecycle callback with unexpected event type $eventType, expected $expectedEventType")
      Left(InvalidLifecycleEventTypeError)
    } else Right(lifecycleEvent)
  }

  private def assertHostAuthorized(
      lifecycleEvent: LifecycleEvent,
      host: AtlassianHost): Either[LifecycleError, AtlassianHost] = {
    if (host.clientKey != lifecycleEvent.clientKey) {
      logger.error(
        s"Request was authenticated for host ${host.clientKey}, but the host in the body of the request is ${lifecycleEvent.clientKey}. Returning 403.")
      Left(HostForbiddenError)
    } else Right(host)
  }

}

sealed trait LifecycleError

object InvalidLifecycleEventTypeError extends LifecycleError {
  val reason = "Invalid lifecycle event type"
}

object HostForbiddenError extends LifecycleError

object MissingJwtError extends LifecycleError

object MissingAtlassianHostError extends LifecycleError
