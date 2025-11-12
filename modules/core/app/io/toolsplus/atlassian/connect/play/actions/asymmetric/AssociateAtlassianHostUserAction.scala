package io.toolsplus.atlassian.connect.play.actions.asymmetric

import io.toolsplus.atlassian.connect.play.actions.AtlassianHostUserRequest
import io.toolsplus.atlassian.connect.play.api.models.{
  AtlassianHost,
  AtlassianHostUser,
  DefaultAtlassianHostUser
}
import io.toolsplus.atlassian.connect.play.api.repositories.{
  AtlassianHostRepository,
  ForgeInstallationRepository
}
import play.api.Logger
import play.api.mvc.Results.BadRequest
import play.api.mvc.{ActionRefiner, Result, WrappedRequest}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait AbstractAssociateAtlassianHostUserActionRefiner[R[A] <: WrappedRequest[A]]
    extends ActionRefiner[ForgeRemoteContextRequest, R] {

  implicit def executionContext: ExecutionContext

  def logger: Logger
  def hostRepository: AtlassianHostRepository
  def forgeInstallationRepository: ForgeInstallationRepository

  def hostSearchResultToActionResult[A](
      maybeHost: Option[AtlassianHost],
      request: ForgeRemoteContextRequest[A]
  ): Either[Result, R[A]]

  override def refine[A](
      request: ForgeRemoteContextRequest[A]
  ): Future[Either[Result, R[A]]] = {
    val installationId = request.context.invocationContext.app.installationId
    forgeInstallationRepository
      .findByInstallationId(installationId)
      .flatMap({
        case Some(installation) =>
          hostRepository
            .findByClientKey(installation.clientKey)
            .map(hostSearchResultToActionResult(_, request))
        case None =>
          logger.error(
            s"Failed to associate Connect host to Forge Remote Compute invocation: No host mapping for installation id $installationId found"
          )
          Future.successful(Left(BadRequest(s"Missing Connect mapping")))
      })
  }
}

case class ForgeRemoteAssociateAtlassianHostUserRequest[A](
    hostUser: AtlassianHostUser,
    request: ForgeRemoteContextRequest[A]
) extends AtlassianHostUserRequest[A](request)

/** Action that associates an Atlassian host to an existing Forge Remote context
  * and fails if no host could be found.
  *
  * Extracts the installation id from the given Forge Remote context and tries
  * to find the host associated with the installation. If no Atlassian host
  * could be found, this action will return a 400 Bad Request result.
  */
case class AssociateAtlassianHostUserActionRefiner @Inject() (
    override val hostRepository: AtlassianHostRepository,
    override val forgeInstallationRepository: ForgeInstallationRepository
)(override implicit val executionContext: ExecutionContext)
    extends AbstractAssociateAtlassianHostUserActionRefiner[
      ForgeRemoteAssociateAtlassianHostUserRequest
    ] {
  override val logger: Logger = Logger(
    classOf[AssociateAtlassianHostUserActionRefiner]
  )

  override def hostSearchResultToActionResult[A](
      maybeHost: Option[AtlassianHost],
      request: ForgeRemoteContextRequest[A]
  ): Either[Result, ForgeRemoteAssociateAtlassianHostUserRequest[A]] =
    maybeHost match {
      case Some(host) =>
        Right(
          ForgeRemoteAssociateAtlassianHostUserRequest(
            DefaultAtlassianHostUser(
              host,
              request.context.invocationContext.principal
            ),
            request
          )
        )
      case None =>
        val installationId =
          request.context.invocationContext.app.installationId
        logger.error(
          s"Failed to associate Connect host to Forge Remote Compute invocation: No host for installation id $installationId found"
        )
        Left(BadRequest(s"Missing Connect installation"))
    }

  object Implicits {

    /** Implicitly convert an instance of
      * [[ForgeRemoteAssociateAtlassianHostUserRequest]] to an instance of
      * AtlassianHostUser.
      *
      * @param request
      *   Forge Remote associated host user request instance.
      * @return
      *   Atlassian host user instance extracted from request.
      */
    implicit def requestToHostUser(implicit
        request: ForgeRemoteAssociateAtlassianHostUserRequest[_]
    ): AtlassianHostUser =
      request.hostUser

  }

}

case class ForgeRemoteAssociateMaybeAtlassianHostUserRequest[A](
    hostUser: Option[AtlassianHostUser],
    request: ForgeRemoteContextRequest[A]
) extends WrappedRequest[A](request)

/** Action that attempts to associate an Atlassian host to an existing Forge
  * Remote context.
  *
  * Extracts the installation id from the given Forge Remote context and tries
  * to find the host associated with the installation. If no Atlassian host
  * could be found, this action will succeed with a None option value.
  */
case class AssociateMaybeAtlassianHostUserActionRefiner @Inject() (
    override val hostRepository: AtlassianHostRepository,
    override val forgeInstallationRepository: ForgeInstallationRepository
)(override implicit val executionContext: ExecutionContext)
    extends AbstractAssociateAtlassianHostUserActionRefiner[
      ForgeRemoteAssociateMaybeAtlassianHostUserRequest
    ] {
  override val logger: Logger = Logger(
    classOf[AssociateMaybeAtlassianHostUserActionRefiner]
  )

  override def hostSearchResultToActionResult[A](
      maybeHost: Option[AtlassianHost],
      request: ForgeRemoteContextRequest[A]
  ): Either[Result, ForgeRemoteAssociateMaybeAtlassianHostUserRequest[A]] =
    Right(
      ForgeRemoteAssociateMaybeAtlassianHostUserRequest(
        maybeHost.map(
          DefaultAtlassianHostUser(
            _,
            request.context.invocationContext.principal
          )
        ),
        request
      )
    )
}
