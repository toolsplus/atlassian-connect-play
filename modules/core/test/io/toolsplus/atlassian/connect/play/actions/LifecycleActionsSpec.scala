package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.actions.LifecycleActions.{
  CheckLifecycleEventTypeAction,
  LifecycleRequest
}
import org.scalacheck.Gen._
import play.api.mvc.Results.BadRequest

import scala.concurrent.ExecutionContext.Implicits.global

class LifecycleActionsSpec extends TestSpec {

  "A LifecycleAction" when {

    "given a lifecycle event" should {

      "return bad request if installed event type does not match" in {
        forAll(installedEventGen, playRequestGen, alphaStr) {
          (event, request, randomEvent) =>
            val lifecycleRequest = LifecycleRequest(event, request)
            val action = CheckLifecycleEventTypeAction(randomEvent)
            val result = await {
              action.filter(lifecycleRequest)
            }
            result mustBe Some(BadRequest("Invalid lifecycle event type"))
        }
      }

      "return bad request if generic event type does not match" in {
        forAll(genericEventGen, playRequestGen, alphaStr) {
          (event, request, randomEvent) =>
            val lifecycleRequest = LifecycleRequest(event, request)
            val action = CheckLifecycleEventTypeAction(randomEvent)
            val result = await {
              action.filter(lifecycleRequest)
            }
            result mustBe Some(BadRequest("Invalid lifecycle event type"))
        }
      }

      "return None if installed event type matches" in {
        forAll(installedEventGen, playRequestGen) { (event, request) =>
          val lifecycleRequest = LifecycleRequest(event, request)
          val action = CheckLifecycleEventTypeAction("installed")
          val result = await {
            action.filter(lifecycleRequest)
          }
          result mustBe None
        }
      }

      "return None if generic event type matches" in {
        forAll(genericEventGen, playRequestGen) { (event, request) =>
          val lifecycleRequest = LifecycleRequest(event, request)
          val action = CheckLifecycleEventTypeAction(event.eventType)
          val result = await {
            action.filter(lifecycleRequest)
          }
          result mustBe None
        }
      }

    }

  }

}
