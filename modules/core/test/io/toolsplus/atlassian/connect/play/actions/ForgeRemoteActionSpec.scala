package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.auth.frc.ForgeRemoteCredentials
import org.scalatest.EitherValues
import play.api.mvc.Results.Unauthorized
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global

class ForgeRemoteActionSpec
    extends TestSpec
    with EitherValues
    with ForgeRemoteRequestGen {

  val forgeRemoteActionRefiner = new ForgeRemoteActionRefiner()

  "ForgeRemoteActionRefiner" when {

    "refining a standard Request" should {

      "successfully refine request to ForgeRemoteRequest" in {
        forAll(forgeRemoteRequestGen) { request =>
          val result = await {
            forgeRemoteActionRefiner.refine(request)
          }
          result.value mustBe a[ForgeRemoteRequest[_]]
        }
      }

      "fail to refine request if it credential extraction fails" in {
        val result = await {
          forgeRemoteActionRefiner.refine(FakeRequest())
        }
        result mustBe Left(
          Unauthorized("Invalid or missing Forge Remote credentials"))
      }
    }
  }

}
