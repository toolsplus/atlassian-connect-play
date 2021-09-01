package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.auth.jwt.JwtCredentials
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{ActionRefiner, Request, Result, WrappedRequest}

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
