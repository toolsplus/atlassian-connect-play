package io.toolsplus.atlassian.connect.play.actions.asymmetric

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.actions.ForgeRemoteRequest
import io.toolsplus.atlassian.connect.play.auth.frc.jwt.{
  App,
  Environment,
  ForgeInvocationContext,
  ForgeInvocationTokenGen,
  ForgeJWSVerificationKeySelector,
  ForgeRemoteJwtAuthenticationProvider,
  Installation,
  InstallationContext,
  Module
}
import io.toolsplus.atlassian.connect.play.auth.frc.{
  ForgeRemoteContext,
  ForgeRemoteCredentials
}
import io.toolsplus.atlassian.connect.play.models.AtlassianForgeProperties
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status.UNAUTHORIZED
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status}

import java.security.interfaces.RSAPublicKey
import java.security.{KeyPair, PrivateKey}
import scala.concurrent.ExecutionContext.Implicits.global

class ForgeRemoteSignedForgeRemoteContextActionSpec
    extends TestSpec
    with GuiceOneAppPerSuite
    with ForgeInvocationTokenGen
    with EitherValues {

  val appId = "fake-app-id"

  val config: Configuration =
    Configuration.from(
      Map(
        "atlassian.forge" -> Map(
          "appId" -> appId,
          "remote.jwkSetStagingUrl" -> "fake-jwk-set-staging-url",
          "remote.jwkSetProductionUrl" -> "fake-jwk-set-production-url"
        )
      )
    )
  val forgeProperties = new AtlassianForgeProperties(config)

  val fakeForgeInvocationContext: ForgeInvocationContext =
    ForgeInvocationContext(
      App(
        "fake-installation-id",
        "fake-api-base-url",
        appId,
        "fake-app-version",
        Environment("fake-type", "fake-id"),
        Module("fake-type", "fake-key"),
        Installation(
          "fake-installation-id",
          Seq(
            InstallationContext(
              "fake-installation-context-name-1",
              "fake-installation-context-url-1"
            )
          )
        ),
        None
      ),
      None,
      None
    )

  val keyId: String = "0e50fccb-239d-4991-a5db-dc850ba3f236"
  val keyPair: KeyPair = JwtTestHelper.generateKeyPair()
  val publicKey: RSAPublicKey = keyPair.getPublic.asInstanceOf[RSAPublicKey]
  val privateKey: PrivateKey = keyPair.getPrivate
  val mockForgeJWSVerificationKeySelector: ForgeJWSVerificationKeySelector =
    mock[ForgeJWSVerificationKeySelector]
  val authenticationProvider =
    new ForgeRemoteJwtAuthenticationProvider(
      forgeProperties,
      mockForgeJWSVerificationKeySelector
    )

  "ForgeRemoteSignedForgeRemoteContextAction" when {

    "refining an Forge Remote signed ForgeRemoteRequest" should {

      val refiner: ForgeRemoteSignedForgeRemoteContextActionRefiner =
        ForgeRemoteSignedForgeRemoteContextActionRefiner(authenticationProvider)

      "successfully refine to ForgeRemoteContextRequest" in {
        forAll(
          forgeInvocationTokenGen(fakeForgeInvocationContext, keyId, privateKey)
        ) { invocationToken =>
          (mockForgeJWSVerificationKeySelector.selectJWSKeys _)
            .expects(*, fakeForgeInvocationContext)
            .returning(java.util.List.of(publicKey))

          val forgeRemoteCredentials = ForgeRemoteCredentials(
            "fake-trace-id",
            "fake-span-id",
            invocationToken,
            None,
            None
          )

          val forgeRemoteRequest =
            ForgeRemoteRequest(forgeRemoteCredentials, FakeRequest())

          val result = await {
            refiner.refine(forgeRemoteRequest)
          }
          result mustBe Right(
            ForgeRemoteContextRequest(
              ForgeRemoteContext(
                fakeForgeInvocationContext,
                forgeRemoteCredentials.traceId,
                forgeRemoteCredentials.spanId,
              ),
              forgeRemoteRequest
            )
          )
        }
      }

      "fail to refine if authentication fails" in {
        val anotherKeyPair = JwtTestHelper.generateKeyPair()
        forAll(
          forgeInvocationTokenGen(
            fakeForgeInvocationContext,
            keyId,
            anotherKeyPair.getPrivate
          )
        ) { invocationToken =>
          (mockForgeJWSVerificationKeySelector.selectJWSKeys _)
            .expects(*, fakeForgeInvocationContext)
            .returning(java.util.List.of(publicKey))

          val forgeRemoteCredentials = ForgeRemoteCredentials(
            "fake-trace-id",
            "fake-span-id",
            invocationToken,
            None,
            None
          )

          val forgeRemoteRequest =
            ForgeRemoteRequest(forgeRemoteCredentials, FakeRequest())

          val result = refiner.refine(forgeRemoteRequest)

          status(result.map(_.left.value)) mustBe UNAUTHORIZED
          contentAsString(
            result.map(_.left.value)
          ) startsWith "JWT validation failed"
        }
      }
    }
  }

}
