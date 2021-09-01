package io.toolsplus.atlassian.connect.play.actions.asymmetric

import cats.implicits._
import io.toolsplus.atlassian.connect.play.actions.{
  JwtActionRefiner,
  JwtRequest
}
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHostUser
import io.toolsplus.atlassian.connect.play.auth.jwt._
import io.toolsplus.atlassian.connect.play.auth.jwt.asymmetric.AsymmetricJwtAuthenticationProvider
import play.api.mvc.Results.Unauthorized
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class MaybeAtlassianHostUserRequest[A](hostUser: Option[AtlassianHostUser],
                                            request: JwtRequest[A])
    extends WrappedRequest[A](request)

case class AsymmetricallySignedAtlassianHostUserActionRefiner(
    jwtAuthenticationProvider: AsymmetricJwtAuthenticationProvider,
    qshProvider: QshProvider)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[JwtRequest, MaybeAtlassianHostUserRequest] {
  override def refine[A](request: JwtRequest[A])
    : Future[Either[Result, MaybeAtlassianHostUserRequest[A]]] = {
    val expectedQsh = qshProvider match {
      case ContextQshProvider => ContextQshProvider.qsh
      case CanonicalHttpRequestQshProvider =>
        CanonicalHttpRequestQshProvider.qsh(
          request.credentials.canonicalHttpRequest)
    }
    jwtAuthenticationProvider
      .authenticate(request.credentials, expectedQsh)
      .map(MaybeAtlassianHostUserRequest(_, request))
      .leftMap(e => Unauthorized(s"JWT validation failed: ${e.getMessage}"))
      .value
  }
}

class AsymmetricallySignedAtlassianHostUserAction @Inject()(
    bodyParser: BodyParsers.Default,
    jwtActionRefiner: JwtActionRefiner,
    asymmetricJwtAuthenticationProvider: AsymmetricJwtAuthenticationProvider)(
    implicit executionCtx: ExecutionContext) {

  /**
    * Creates an action builder that validates asymmetrically signed JWT requests. Callers must specify
    * how the query string hash claim should be verified.
    *
    * @param qshProvider Query string hash provider that specifies what kind of QSH the qsh claim contains
    * @return Play action for asymmetrically signed JWT requests
    */
  def authenticateWith(qshProvider: QshProvider)
    : ActionBuilder[MaybeAtlassianHostUserRequest, AnyContent] =
    new ActionBuilder[MaybeAtlassianHostUserRequest, AnyContent] {
      override val parser: BodyParsers.Default = bodyParser
      override val executionContext: ExecutionContext = executionCtx
      override def invokeBlock[A](
          request: Request[A],
          block: MaybeAtlassianHostUserRequest[A] => Future[Result])
        : Future[Result] = {
        (jwtActionRefiner andThen AsymmetricallySignedAtlassianHostUserActionRefiner(
          asymmetricJwtAuthenticationProvider,
          qshProvider))
          .invokeBlock(request, block)
      }
    }
}
