package io.toolsplus.atlassian.connect.play.actions

import cats.data.Validated.{Invalid, Valid}
import io.toolsplus.atlassian.connect.play.auth.frc.ForgeRemoteCredentials
import play.api.Logger
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{ActionRefiner, Request, Result, WrappedRequest}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class ForgeRemoteRequest[A](credentials: ForgeRemoteCredentials,
                                 request: Request[A])
    extends WrappedRequest[A](request)

/**
  * Play action refiner that extracts the Forge Remote credentials from a request
  *
  * Note that this refiner will intercept the request and return an unauthorized
  * result if invalid or no Forge Remote credentials were found.
  */
class ForgeRemoteActionRefiner @Inject()(
    implicit val executionContext: ExecutionContext)
    extends ActionRefiner[Request, ForgeRemoteRequest] {

  private val logger = Logger(classOf[ForgeRemoteActionRefiner])

  override def refine[A](
      request: Request[A]): Future[Either[Result, ForgeRemoteRequest[A]]] =
    ForgeRemoteCredentialsExtractor.extract(request) match {
      case Valid(credentials) =>
        Future.successful(Right(ForgeRemoteRequest(credentials, request)))
      case Invalid(errors) =>
        logger.info(
          s"Failed to extract Forge Remote credentials: ${errors.map(_.getMessage).toNonEmptyList.toList.mkString(", ")}")
        Future.successful(
          Left(Unauthorized("Invalid or missing Forge Remote credentials")))
    }
}
