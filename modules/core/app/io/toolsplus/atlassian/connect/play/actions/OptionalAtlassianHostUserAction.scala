package io.toolsplus.atlassian.connect.play.actions

import io.lemonlabs.uri.Url
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHostUser
import io.toolsplus.atlassian.connect.play.auth.jwt.{JwtAuthenticationError, JwtAuthenticationProvider, JwtCredentials, UnknownJwtIssuerError}
import io.toolsplus.atlassian.connect.play.controllers.routes
import io.toolsplus.atlassian.connect.play.models.AtlassianConnectProperties
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.Results.Unauthorized
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class MaybeJwtRequest[A](maybeCredentials: Option[JwtCredentials],
                              request: Request[A])
    extends WrappedRequest[A](request)

class MaybeJwtActionRefiner @Inject()()(
    implicit val executionContext: ExecutionContext)
    extends ActionRefiner[Request, MaybeJwtRequest] {
  override def refine[A](
      request: Request[A]): Future[Either[Result, MaybeJwtRequest[A]]] = {
    val maybeCredentials = JwtExtractor.extractJwt(request)
    Future.successful(Right(MaybeJwtRequest(maybeCredentials, request)))
  }
}

case class MaybeAtlassianHostUserRequest[A](
    maybeHostUser: Option[AtlassianHostUser],
    request: MaybeJwtRequest[A])
    extends WrappedRequest[A](request)

class MaybeAtlassianHostUserActionRefiner @Inject()(
    jwtAuthenticationProvider: JwtAuthenticationProvider,
    connectProperties: AtlassianConnectProperties)(
    implicit val executionContext: ExecutionContext)
    extends ActionRefiner[MaybeJwtRequest, MaybeAtlassianHostUserRequest] {

  private val logger = Logger(classOf[MaybeAtlassianHostUserActionRefiner])

  override def refine[A](request: MaybeJwtRequest[A])
    : Future[Either[Result, MaybeAtlassianHostUserRequest[A]]] = {
    request.maybeCredentials match {
      case Some(credentials) =>
        jwtAuthenticationProvider.authenticate(credentials).value.map {
          case Right(hostUser) =>
            Right(MaybeAtlassianHostUserRequest(Some(hostUser), request))
          case Left(e) =>
            if (shouldIgnoreInvalidJwt(request, e)) {
              logger.warn(
                s"Received JWT authentication from unknown host (${e.asInstanceOf[UnknownJwtIssuerError].issuer}), but allowing anyway")
              Right(MaybeAtlassianHostUserRequest(None, request))
            } else {
              logger.debug(s"Authentication of JWT signed request failed: $e")
              Left(Unauthorized(s"JWT validation failed: ${e.getMessage}"))
            }
        }
      case None =>
        Future.successful(Right(MaybeAtlassianHostUserRequest(None, request)))
    }
  }

  /** Checks framework property allowReinstallMissingHost to decide whether to accept installations
    * signed by an unknown host. This can be useful in development mode using an in-memory database
    * but should never be enabled in production.
    *
    * @param request Request to check
    * @param e Error that occurred during token authentication
    * @return True if it's ok to ignore UnknownJwtIssuerError and install anyways, false otherwise.
    */
  private def shouldIgnoreInvalidJwt[A](request: MaybeJwtRequest[A],
                                        e: JwtAuthenticationError): Boolean = {
    e match {
      case UnknownJwtIssuerError(_) =>
        (isInstalledLifecycleRequest(request) &&
          connectProperties.allowReinstallMissingHost) ||
          isUninstalledLifecycleRequest(request)
      case _ => false
    }

  }

  private def isInstalledLifecycleRequest[A](request: Request[A]) =
    isRequestToUrl(
      request,
      routes.LifecycleController.installed().absoluteURL()(request))

  private def isUninstalledLifecycleRequest[A](request: Request[A]) =
    isRequestToUrl(
      request,
      routes.LifecycleController.uninstalled().absoluteURL()(request))

  private def isRequestToUrl[A](request: Request[A], url: String): Boolean = {
    val requestUri = Url.parse(request.uri)
    val referenceUri = Url.parse(url)
    requestUri.path == referenceUri.path && referenceUri.query.paramMap.toSet
      .subsetOf(requestUri.query.paramMap.toSet)
  }
}

class OptionalAtlassianHostUserAction @Inject()(
    val parser: BodyParsers.Default,
    jwtActionRefiner: MaybeJwtActionRefiner,
    atlassianHostUserActionRefiner: MaybeAtlassianHostUserActionRefiner)(
    implicit val executionContext: ExecutionContext)
    extends ActionBuilder[MaybeAtlassianHostUserRequest, AnyContent] {
  override def invokeBlock[A](
      request: Request[A],
      block: MaybeAtlassianHostUserRequest[A] => Future[Result]): Future[Result] = {
    (jwtActionRefiner andThen atlassianHostUserActionRefiner)
      .invokeBlock(request, block)
  }

  object Implicits {

    import scala.language.implicitConversions

    /**
      * Implicitly convert an instance of [[MaybeAtlassianHostUserRequest]]
      * to an instance of Option[AtlassianHostUser].
      *
      * @param r Maybe Atlassian host user request instance.
      * @return Optional Atlassian host user instance extracted from request.
      */
    implicit def maybeHostUserRequestToMaybeHostUser(
        implicit r: MaybeAtlassianHostUserRequest[_])
      : Option[AtlassianHostUser] = r.maybeHostUser

  }

}
