package io.toolsplus.atlassian.connect.play.actions

import cats.implicits._
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHostUser
import io.toolsplus.atlassian.connect.play.auth.jwt._
import play.api.mvc.Results.Unauthorized
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class JwtRequest[A](credentials: JwtCredentials, request: Request[A])
    extends WrappedRequest[A](request)

/**
  * Play action refiner that extracts the JWT credentials from a request
  *
  * Note that this refiner will intercept the request and return an unauthorized
  * result if no JWT credentials were found.
  */
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

case class AtlassianHostUserActionRefiner(
    jwtAuthenticationProvider: JwtAuthenticationProvider,
    qshProvider: QshProvider)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[JwtRequest, AtlassianHostUserRequest] {
  override def refine[A](request: JwtRequest[A])
    : Future[Either[Result, AtlassianHostUserRequest[A]]] = {
    val expectedQsh = qshProvider match {
      case ContextQshProvider => ContextQshProvider.qsh
      case CanonicalHttpRequestQshProvider =>
        CanonicalHttpRequestQshProvider.qsh(
          request.credentials.canonicalHttpRequest)
    }
    jwtAuthenticationProvider
      .authenticate(request.credentials, expectedQsh)
      .map(AtlassianHostUserRequest(_, request))
      .leftMap(e => Unauthorized(s"JWT validation failed: ${e.getMessage}"))
      .value
  }
}

class AtlassianHostUserActionRefinerFactory @Inject()(
    jwtAuthenticationProvider: JwtAuthenticationProvider)(
    implicit executionContext: ExecutionContext) {

  def withQshFrom(qshProvider: QshProvider)
    : AtlassianHostUserActionRefiner =
    AtlassianHostUserActionRefiner(jwtAuthenticationProvider, qshProvider)
}

class AtlassianHostUserAction @Inject()(
    bodyParser: BodyParsers.Default,
    jwtActionRefiner: JwtActionRefiner,
    atlassianHostUserActionRefinerFactory: AtlassianHostUserActionRefinerFactory)(
    implicit executionCtx: ExecutionContext) {

  /**
    * Creates an action builder that validates JWT authenticated requests and verifies the
    * query string hash claim against the provided query string hash provider.
    *
    * @param qshProvider Query string hash provider that specifies what kind of QSH the qsh claim contains
    * @return Play action for JWT validated requests
    */
  def withQshFrom(qshProvider: QshProvider)
    : ActionBuilder[AtlassianHostUserRequest, AnyContent] =
    new ActionBuilder[AtlassianHostUserRequest, AnyContent] {
      override val parser: BodyParsers.Default = bodyParser
      override val executionContext: ExecutionContext = executionCtx
      override def invokeBlock[A](
          request: Request[A],
          block: AtlassianHostUserRequest[A] => Future[Result])
        : Future[Result] = {
        (jwtActionRefiner andThen atlassianHostUserActionRefinerFactory
          .withQshFrom(qshProvider))
          .invokeBlock(request, block)
      }
    }

  object Implicits {

    import scala.language.implicitConversions

    /**
      * Implicitly convert an instance of [[AtlassianHostUserRequest]] to an
      * instance of AtlassianHostUser.
      *
      * @param r Atlassian host user request instance.
      * @return Atlassian host user instance extracted from request.
      */
    implicit def hostUserRequestToHostUser(
        implicit r: AtlassianHostUserRequest[_]): AtlassianHostUser =
      r.hostUser

  }

}
