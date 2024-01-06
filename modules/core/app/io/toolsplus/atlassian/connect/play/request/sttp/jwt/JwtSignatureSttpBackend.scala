package io.toolsplus.atlassian.connect.play.request.sttp.jwt

import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.auth.jwt.symmetric.JwtGenerator
import io.toolsplus.atlassian.connect.play.request.sttp.jwt.JwtSignatureSttpBackend.atlassianHostTagKey
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import sttp.capabilities.Effect
import sttp.client3.{
  DelegateSttpBackend,
  Identity,
  Request,
  RequestT,
  Response,
  SttpBackend,
  UriContext
}
import sttp.monad.MonadError
import sttp.monad.syntax._

/**
  * Sttp backend that authenticates Atlassian host requests as the app.
  *
  * The app must specify the authentication type `jwt` in its app descriptor.
  *
  * For the backend to sign requests, the request must be associated with a `AtlassianHost` as follows:
  * {{{
  * import io.toolsplus.atlassian.connect.play.request.sttp.jwt.JwtSignatureSttpBackend._
  *
  * class MyJiraHttpClient @Inject()(sttpBackend: SttpBackend[Future, Any]) {
  *
  *   def fetchIssue(issueKey: String)(implicit host: AtlassianHost) = {
  *      basicRequest.withAtlassianHost(host).get(s"/rest/api/2/issue/{issueKey}").send(sttpBackend)
  *   }
  * }
  * }}}
  */
final class JwtSignatureSttpBackend[F[_], P](
    delegate: SttpBackend[F, P],
    jwtGenerator: JwtGenerator,
) extends DelegateSttpBackend(delegate) {
  implicit val F: MonadError[F] = delegate.responseMonad

  override def send[T, R >: P with Effect[F]](
      request: Request[T, R]): F[Response[T]] =
    for {
      host <- extractHost(request)
      absoluteUriRequest = ensureAbsoluteRequestUrl(request, host)
      jwt <- generateJwt(absoluteUriRequest, host)
      response <- delegate.send(
        absoluteUriRequest
          .header("Authorization", s"JWT $jwt")
          .header("User-Agent", "atlassian-connect-play")
      )
    } yield response

  private def extractHost[T, R >: P with Effect[F]](
      request: Request[T, R]): F[AtlassianHost] =
    request.tag(atlassianHostTagKey) match {
      case Some(host) =>
        host match {
          case h: AtlassianHost => F.unit(h)
          case h =>
            F.error(new Exception(
              s"Failed to extract Atlassian host from request: Invalid host type '${h.getClass.getName}', expected '${classOf[
                AtlassianHost].getName}'"))
        }
      case None =>
        F.error(new Exception(
          "Failed to extract Atlassian host from request: No host configured. Use `request.tag(\"ATLASSIAN_HOST\", host)` to associate a request to a host"))
    }

  private def generateJwt[T, R >: P with Effect[F]](
      request: Request[T, R],
      host: AtlassianHost): F[RawJwt] = {
    jwtGenerator.createJwtToken(request.method.toString,
                                request.uri.toJavaUri,
                                host) match {
      case Right(jwt) => F.unit(jwt)
      case Left(error) =>
        F.error(new Exception(s"Unexpected JWT error: ${error.message}"))
    }
  }

  private def ensureAbsoluteRequestUrl[T, R >: P with Effect[F]](
      request: Request[T, R],
      host: AtlassianHost): Request[T, R] = {
    if (request.uri.isAbsolute) {
      request
    } else {
      val absoluteUri = uri"${host.baseUrl}"
        .withPath(request.uri.path)
        .withParams(request.uri.params)
        .fragment(request.uri.fragment)
      request.copy[Identity, T, R](uri = absoluteUri)
    }
  }
}

object JwtSignatureSttpBackend {
  val atlassianHostTagKey = "ATLASSIAN_HOST"

  implicit class RequestTExtensions[U[_], T, -R](r: RequestT[U, T, R]) {
    def withAtlassianHost(host: AtlassianHost): RequestT[U, T, R] =
      r.tag(atlassianHostTagKey, host)
  }
}
