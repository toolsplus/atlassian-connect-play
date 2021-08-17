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
  AtlassianHostUser
}
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.events.EventBus
import io.toolsplus.atlassian.connect.play.models.Implicits._
import io.toolsplus.atlassian.connect.play.models._
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LifecycleService @Inject()(hostRepository: AtlassianHostRepository) {

  private val logger = Logger(classOf[LifecycleService])

  /** Installs the given Atlassian host by saving the security context to the
    * database.
    *
    * @param installedEvent         Lifecycle event holding the security context
    *                               provided by Atlassian product.
    * @param atlassianHostUser Atlassian host user associated with the request.
    * @return Installed AtlassianHost instance.
    */
  def installed(installedEvent: InstalledEvent)(
      implicit atlassianHostUser: AtlassianHostUser)
    : EitherT[Future, LifecycleError, AtlassianHost] =
    for {
      _ <- assertLifecycleEventType(installedEvent, "installed")
        .toEitherT[Future]
      host <- install(installedEvent, atlassianHostUser)
      _ = EventBus.publish(AppInstalledEvent(host))
    } yield host

  /** Install the given [[AtlassianHost]].
    *
    * Assert that the client key of the authenticated host matches the client key in the lifecycle event.
    *
    * @param installedEvent Lifecycle event holding the security context
    *                       provided by Atlassian product.
    * @param hostUser       Authenticated Atlassian host user.
    * @return Either the newly installed [[AtlassianHost]] or a [[HostForbiddenError]]
    */
  private def install(installedEvent: InstalledEvent,
                      hostUser: AtlassianHostUser)
    : EitherT[Future, LifecycleError, AtlassianHost] = {
      val newHost = installedEventToAtlassianHost(installedEvent)
      for {
        _ <- assertHostAuthorized(installedEvent, hostUser).toEitherT[Future]
        _ = logger.info(
          s"Saved installation for host ${newHost.baseUrl} (${newHost.clientKey})")
        host <- EitherT.right[LifecycleError](hostRepository.save(newHost))
      } yield host
  }

  /** Uninstalls the given Atlassian host by setting the installed flag in
    * the host store to false.
    *
    * If there is no Atlassian host for the given lifecycle event installed
    * [[MissingAtlassianHostError]] is returned. This error however can be
    * treated as successful uninstallation as the host is not there anyways.
    *
    * @param uninstalledEvent Lifecycle event payload.
    * @param hostUser         Atlassian host user that made the request.
    * @return Either [[LifecycleError]] or AtlassianHost.
    */
  def uninstalled(uninstalledEvent: GenericEvent)(
      implicit hostUser: AtlassianHostUser)
    : EitherT[Future, LifecycleError, AtlassianHost] =
    for {
      _ <- assertLifecycleEventType(uninstalledEvent, "uninstalled")
        .toEitherT[Future]
      _ <- assertHostAuthorized(uninstalledEvent, hostUser).toEitherT[Future]
      maybeExistingHost <- EitherT
        .right[LifecycleError](existingHostByLifecycleEvent(uninstalledEvent))
      result <- uninstall(uninstalledEvent, maybeExistingHost)
      _ = EventBus.publish(AppUninstalledEvent(hostUser.host))
    } yield result

  private def uninstall(uninstalledEvent: GenericEvent,
                        maybeExistingHost: Option[AtlassianHost])
    : EitherT[Future, LifecycleError, AtlassianHost] = {
    maybeExistingHost match {
      case Some(host) =>
        logger.info(
          s"Saved uninstallation for host ${host.baseUrl} (${host.clientKey})")
        EitherT.right(hostRepository.save(host.uninstalled))
      case None =>
        logger.error(
          s"Received authenticated uninstall request but no installation for host ${uninstalledEvent.baseUrl} has been found. Assume the add-on has been removed.")
        EitherT[Future, LifecycleError, AtlassianHost](
          Future.successful(Left(MissingAtlassianHostError)))
    }
  }

  private def existingHostByLifecycleEvent(
      lifecycleEvent: LifecycleEvent): Future[Option[AtlassianHost]] = {
    hostRepository.findByClientKey(lifecycleEvent.clientKey)
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

  private def assertHostAuthorized(lifecycleEvent: LifecycleEvent,
                                   hostUser: AtlassianHostUser)
    : Either[LifecycleError, AtlassianHostUser] = {
    if (hostUser.host.clientKey != lifecycleEvent.clientKey) {
      logger.error(
        s"Request was authenticated for host ${hostUser.host.clientKey}, but the host in the body of the request is ${lifecycleEvent.clientKey}. Returning 403.")
      Left(HostForbiddenError)
    } else Right(hostUser)
  }

}

sealed trait LifecycleError

object InvalidLifecycleEventTypeError extends LifecycleError {
  val reason = "Invalid lifecycle event type"
}

object HostForbiddenError extends LifecycleError

object MissingJwtError extends LifecycleError

object MissingAtlassianHostError extends LifecycleError
