package io.toolsplus.atlassian.connect.play.actions

import cats.implicits._
import com.google.inject.Inject
import com.netaporter.uri.Uri
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHostUser
import io.toolsplus.atlassian.connect.play.auth.jwt._
import io.toolsplus.atlassian.connect.play.controllers.routes
import io.toolsplus.atlassian.connect.play.models.AtlassianConnectProperties
import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc.Results._
import play.api.mvc.{ActionRefiner, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JwtAuthenticationActions @Inject()(
    jwtAuthenticationProvider: JwtAuthenticationProvider,
    connectProperties: AtlassianConnectProperties) {

  private val logger = Logger(classOf[JwtAuthenticationActions])

  object JwtExtractor {
    def extractJwt[A](request: Request[A]): Option[JwtCredentials] = {
      extractJwtFromHeader(request)
        .orElse(extractJwtFromParameter(request))
        .map(JwtCredentials(_, CanonicalPlayHttpRequest(request)))
    }

    private def extractJwtFromHeader[A](request: Request[A]): Option[String] = {
      request.headers
        .get(HeaderNames.AUTHORIZATION)
        .filter(header =>
          !header.isEmpty && header.startsWith(AUTHORIZATION_HEADER_PREFIX))
        .map(_.substring(AUTHORIZATION_HEADER_PREFIX.length).trim)
    }

    private def extractJwtFromParameter[A](
        request: Request[A]): Option[String] = {
      request.getQueryString(QUERY_PARAMETER_NAME).filter(!_.isEmpty)
    }
  }

  case class JwtRequest[A](credentials: JwtCredentials, request: Request[A])
      extends WrappedRequest[A](request)

  object JwtAction
      extends ActionBuilder[JwtRequest]
      with ActionRefiner[Request, JwtRequest] {
    override def refine[A](
        request: Request[A]): Future[Either[Result, JwtRequest[A]]] =
      JwtExtractor.extractJwt(request) match {
        case Some(credentials) =>
          Future.successful(Right(JwtRequest(credentials, request)))
        case None =>
          Future.successful(
            Left(Unauthorized("No authentication token found")))
      }
  }

  case class MaybeJwtRequest[A](maybeCredentials: Option[JwtCredentials],
                                request: Request[A])
      extends WrappedRequest[A](request)

  object MaybeJwtAction
      extends ActionBuilder[MaybeJwtRequest]
      with ActionRefiner[Request, MaybeJwtRequest] {
    override def refine[A](
        request: Request[A]): Future[Either[Result, MaybeJwtRequest[A]]] = {
      val maybeCredentials = JwtExtractor.extractJwt(request)
      Future.successful(Right(MaybeJwtRequest(maybeCredentials, request)))
    }
  }

  case class AtlassianHostUserRequest[A](hostUser: AtlassianHostUser,
                                         request: JwtRequest[A])
      extends WrappedRequest[A](request)

  object AtlassianHostUserAction
      extends ActionRefiner[JwtRequest, AtlassianHostUserRequest] {
    override def refine[A](request: JwtRequest[A])
      : Future[Either[Result, AtlassianHostUserRequest[A]]] = {
      jwtAuthenticationProvider
        .authenticate(request.credentials)
        .map(AtlassianHostUserRequest(_, request))
        .leftMap(e => Unauthorized(s"JWT validation failed: ${e.getMessage}"))
        .value
    }
  }

  case class MaybeAtlassianHostUserRequest[A](
      maybeHostUser: Option[AtlassianHostUser],
      request: MaybeJwtRequest[A])
      extends WrappedRequest[A](request)

  object MaybeAtlassianHostUserAction
      extends ActionRefiner[MaybeJwtRequest, MaybeAtlassianHostUserRequest] {
    override def refine[A](request: MaybeJwtRequest[A])
      : Future[Either[Result, MaybeAtlassianHostUserRequest[A]]] = {
      request.maybeCredentials match {
        case Some(credentials) =>
          jwtAuthenticationProvider
            .authenticate(credentials)
            .value
            .map {
              case Right(hostUser) =>
                Right(MaybeAtlassianHostUserRequest(Some(hostUser), request))
              case Left(e) =>
                if (shouldIgnoreInvalidJwt(request, e)) {
                  logger.warn(
                    s"Received JWT authentication from unknown host (${e.asInstanceOf[UnknownJwtIssuerError].issuer}), but allowing anyway")
                  Right(MaybeAtlassianHostUserRequest(None, request))
                } else {
                  Left(Unauthorized(s"JWT validation failed: ${e.getMessage}"))
                }
            }
        case None =>
          Future.successful(
            Right(MaybeAtlassianHostUserRequest(None, request)))
      }
    }

    private def shouldIgnoreInvalidJwt[A](
        request: MaybeJwtRequest[A],
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
      val requestUri = Uri.parse(request.uri)
      val referenceUri = Uri.parse(url)
      requestUri.path == referenceUri.path && referenceUri.query.paramMap.toSet
        .subsetOf(requestUri.query.paramMap.toSet)
    }
  }

  val withAtlassianHostUser: ActionBuilder[AtlassianHostUserRequest] =
    JwtAction andThen AtlassianHostUserAction

  val withOptionalAtlassianHostUser
    : ActionBuilder[MaybeAtlassianHostUserRequest] = MaybeJwtAction andThen
    MaybeAtlassianHostUserAction

  object Implicits {

    import scala.language.implicitConversions

    /**
      * Implicitly convert an instance of [[AtlassianHostUserRequest]] to an
      * instance of [[AtlassianHostUser]].
      *
      * @param r Atlassian host user request instance.
      * @return Atlassian host user instance extracted from request.
      */
    implicit def hostUserRequestToHostUser(
        implicit r: AtlassianHostUserRequest[_]): AtlassianHostUser =
      r.hostUser

    /**
      * Implicitly convert an instance of [[MaybeAtlassianHostUserRequest]]
      * to an instance of [[Option[AtlassianHostUser]]].
      *
      * @param r Maybe Atlassian host user request instance.
      * @return Optional Atlassian host user instance extracted from request.
      */
    implicit def maybeHostUserRequestToMaybeHostUser(
        implicit r: MaybeAtlassianHostUserRequest[_])
      : Option[AtlassianHostUser] = r.maybeHostUser

  }

  private[actions] val AUTHORIZATION_HEADER_PREFIX = "JWT "
  private[actions] val QUERY_PARAMETER_NAME = "jwt"

}
