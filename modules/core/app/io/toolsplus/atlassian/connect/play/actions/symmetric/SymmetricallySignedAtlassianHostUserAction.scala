package io.toolsplus.atlassian.connect.play.actions.symmetric

import cats.implicits._
import io.toolsplus.atlassian.connect.play.actions.{
  AtlassianHostUserRequest,
  JwtActionRefiner,
  JwtRequest
}
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHostUser
import io.toolsplus.atlassian.connect.play.auth.jwt._
import io.toolsplus.atlassian.connect.play.auth.jwt.symmetric.SymmetricJwtAuthenticationProvider
import play.api.mvc.Results.Unauthorized
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class ConnectAtlassianHostUserRequest[A](hostUser: AtlassianHostUser,
                                              request: JwtRequest[A])
    extends AtlassianHostUserRequest[A](request)

case class SymmetricallySignedAtlassianHostUserActionRefiner(
    jwtAuthenticationProvider: SymmetricJwtAuthenticationProvider,
    qshProvider: QshProvider)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[JwtRequest, ConnectAtlassianHostUserRequest] {
  override def refine[A](request: JwtRequest[A])
    : Future[Either[Result, ConnectAtlassianHostUserRequest[A]]] = {
    val expectedQsh = qshProvider match {
      case ContextQshProvider => ContextQshProvider.qsh
      case CanonicalHttpRequestQshProvider =>
        CanonicalHttpRequestQshProvider.qsh(
          request.credentials.canonicalHttpRequest)
    }
    jwtAuthenticationProvider
      .authenticate(request.credentials, expectedQsh)
      .map(ConnectAtlassianHostUserRequest(_, request))
      .leftMap(e => Unauthorized(s"JWT validation failed: ${e.getMessage}"))
      .value
  }
}

class SymmetricallySignedAtlassianHostUserAction @Inject()(
    bodyParser: BodyParsers.Default,
    jwtActionRefiner: JwtActionRefiner,
    symmetricJwtAuthenticationProvider: SymmetricJwtAuthenticationProvider)(
    implicit executionCtx: ExecutionContext) {

  /**
    * Creates an action builder that validates symmetrically signed JWT requests. Callers must specify
    * how the query string hash claim should be verified.
    *
    * @param qshProvider Query string hash provider that specifies what kind of QSH the qsh claim contains
    * @return Play action for symmetrically signed JWT requests
    */
  def authenticateWith(qshProvider: QshProvider)
    : ActionBuilder[ConnectAtlassianHostUserRequest, AnyContent] =
    new ActionBuilder[ConnectAtlassianHostUserRequest, AnyContent] {
      override val parser: BodyParsers.Default = bodyParser
      override val executionContext: ExecutionContext = executionCtx
      override def invokeBlock[A](
          request: Request[A],
          block: ConnectAtlassianHostUserRequest[A] => Future[Result])
        : Future[Result] = {
        (jwtActionRefiner andThen SymmetricallySignedAtlassianHostUserActionRefiner(
          symmetricJwtAuthenticationProvider,
          qshProvider)).invokeBlock(request, block)
      }
    }

  object Implicits {

    import scala.language.implicitConversions

    /**
      * Implicitly convert an instance of [[ConnectAtlassianHostUserRequest]] to an
      * instance of AtlassianHostUser.
      *
      * @param request Atlassian host user request instance.
      * @return Atlassian host user instance extracted from request.
      */
    implicit def hostUserRequestToHostUser(
        implicit request: ConnectAtlassianHostUserRequest[_])
      : AtlassianHostUser =
      request.hostUser

  }
}
