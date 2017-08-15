package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.models.LifecycleEvent
import play.api.mvc.{ActionFilter, Request, Result, WrappedRequest}
import play.api.mvc.Results._

import scala.concurrent.{ExecutionContext, Future}

object LifecycleActions {

  case class LifecycleRequest[A](event: LifecycleEvent, request: Request[A])
      extends WrappedRequest[A](request)

  case class CheckLifecycleEventTypeAction(expectedEventType: String)(
      implicit val executionContext: ExecutionContext)
      extends ActionFilter[LifecycleRequest] {
    override def filter[A](
        request: LifecycleRequest[A]): Future[Option[Result]] =
      Future.successful {
        if (request.event.eventType != expectedEventType)
          Some(BadRequest("Invalid lifecycle event type"))
        else
          None
      }
  }

}
