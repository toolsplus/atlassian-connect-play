package io.toolsplus.atlassian.connect.play.actions.asymmetric

import cats.implicits._
import io.toolsplus.atlassian.connect.play.actions.{
  ForgeRemoteActionRefiner,
  ForgeRemoteRequest
}
import io.toolsplus.atlassian.connect.play.auth.frc.ForgeRemoteContext
import io.toolsplus.atlassian.connect.play.auth.frc.jwt.ForgeRemoteJwtAuthenticationProvider
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{
  ActionBuilder,
  ActionRefiner,
  AnyContent,
  BodyParsers,
  Request,
  Result,
  WrappedRequest
}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class ForgeRemoteContextRequest[A](context: ForgeRemoteContext,
                                        request: ForgeRemoteRequest[A])
    extends WrappedRequest[A](request)

case class ForgeRemoteSignedForgeRemoteContextActionRefiner(
    authenticationProvider: ForgeRemoteJwtAuthenticationProvider)(
    implicit val executionContext: ExecutionContext)
    extends ActionRefiner[ForgeRemoteRequest, ForgeRemoteContextRequest] {
  override def refine[A](request: ForgeRemoteRequest[A])
    : Future[Either[Result, ForgeRemoteContextRequest[A]]] = {
    Future.successful(
      authenticationProvider
        .authenticate(request.credentials)
        .map(ForgeRemoteContextRequest(_, request))
        .leftMap(e => Unauthorized(s"JWT validation failed")))
  }
}

class ForgeRemoteSignedForgeRemoteContextAction @Inject()(
    bodyParser: BodyParsers.Default,
    forgeRemoteActionRefiner: ForgeRemoteActionRefiner,
    authenticationProvider: ForgeRemoteJwtAuthenticationProvider)(
    implicit executionCtx: ExecutionContext) {

  /**
    * Creates an action builder that validates requests signed by Forge Remote Compute.
    *
    * @return Play action for Forge Remote Compute requests
    */
  def authenticate: ActionBuilder[ForgeRemoteContextRequest, AnyContent] =
    new ActionBuilder[ForgeRemoteContextRequest, AnyContent] {
      override val parser: BodyParsers.Default = bodyParser
      override val executionContext: ExecutionContext = executionCtx
      override def invokeBlock[A](
          request: Request[A],
          block: ForgeRemoteContextRequest[A] => Future[Result])
        : Future[Result] = {
        (forgeRemoteActionRefiner andThen ForgeRemoteSignedForgeRemoteContextActionRefiner(
          authenticationProvider))
          .invokeBlock(request, block)
      }
    }
}
