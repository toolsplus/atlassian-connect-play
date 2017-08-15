package io.toolsplus.atlassian.connect.play.actions

import javax.inject.Inject

import cats.implicits._
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHostUser
import io.toolsplus.atlassian.connect.play.auth.jwt.{
  JwtAuthenticationProvider,
  JwtCredentials
}
import play.api.mvc.Results.Unauthorized
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class JwtRequest[A](credentials: JwtCredentials, request: Request[A])
    extends WrappedRequest[A](request)

class JwtActionRefiner @Inject()(
    implicit val executionContext: ExecutionContext)
    extends ActionRefiner[Request, JwtRequest] {
  override def refine[A](
      request: Request[A]): Future[Either[Result, JwtRequest[A]]] =
    JwtExtractor.extractJwt(request) match {
      case Some(credentials) =>
        Future.successful(Right(JwtRequest(credentials, request)))
      case None =>
        Future.successful(Left(Unauthorized("No authentication token found")))
    }
}

case class AtlassianHostUserRequest[A](hostUser: AtlassianHostUser,
                                       request: JwtRequest[A])
    extends WrappedRequest[A](request)

class AtlassianHostUserActionRefiner @Inject()(
    jwtAuthenticationProvider: JwtAuthenticationProvider)(
    implicit val executionContext: ExecutionContext)
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

class AtlassianHostUserAction @Inject()(
    val parser: BodyParsers.Default,
    jwtActionRefiner: JwtActionRefiner,
    atlassianHostUserActionRefiner: AtlassianHostUserActionRefiner)(
    implicit val executionContext: ExecutionContext)
    extends ActionBuilder[AtlassianHostUserRequest, AnyContent] {
  override def invokeBlock[A](
      request: Request[A],
      block: (AtlassianHostUserRequest[A]) => Future[Result]) = {
    (jwtActionRefiner andThen atlassianHostUserActionRefiner)
      .invokeBlock(request, block)
  }

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

  }

}
