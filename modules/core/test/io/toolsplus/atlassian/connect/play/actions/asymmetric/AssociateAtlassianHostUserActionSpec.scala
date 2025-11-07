package io.toolsplus.atlassian.connect.play.actions.asymmetric

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.actions.ForgeRemoteRequest
import io.toolsplus.atlassian.connect.play.api.models.{
  AtlassianHost,
  DefaultAtlassianHostUser,
  DefaultForgeInstallation
}
import io.toolsplus.atlassian.connect.play.api.repositories.{
  AtlassianHostRepository,
  ForgeInstallationRepository
}
import io.toolsplus.atlassian.connect.play.auth.frc.jwt.{
  App,
  Environment,
  ForgeInvocationContext,
  ForgeInvocationTokenGen,
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
import play.api.mvc.Results.BadRequest
import play.api.test.FakeRequest

import java.security.interfaces.RSAPublicKey
import java.security.{KeyPair, PrivateKey}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AssociateAtlassianHostUserActionSpec
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

  val mockHostRepository: AtlassianHostRepository =
    mock[AtlassianHostRepository]
  val mockForgeInstallationRepository: ForgeInstallationRepository =
    mock[ForgeInstallationRepository]

  "AssociateAtlassianHostUserAction" when {

    "AssociateAtlassianHostUserActionRefiner" should {

      val refiner: AssociateAtlassianHostUserActionRefiner =
        AssociateAtlassianHostUserActionRefiner(
          mockHostRepository,
          mockForgeInstallationRepository
        )

      "successfully refine to ForgeRemoteAssociateAtlassianHostUserRequest" in {
        forAll(
          forgeInvocationTokenGen(fakeForgeInvocationContext, keyId, privateKey)
        ) { invocationToken =>
          val forgeRemoteCredentials = ForgeRemoteCredentials(
            "fake-trace-id",
            "fake-span-id",
            invocationToken,
            None,
            None
          )

          val forgeRemoteRequest =
            ForgeRemoteRequest(forgeRemoteCredentials, FakeRequest())

          val forgeRemoteContextRequest =
            ForgeRemoteContextRequest(
              ForgeRemoteContext(
                fakeForgeInvocationContext,
                forgeRemoteCredentials.traceId,
                forgeRemoteCredentials.spanId,
              ),
              forgeRemoteRequest
            )

          val fakeClientKey = "fake-client-key"
          val fakeForgeInstallation = DefaultForgeInstallation(
            fakeForgeInvocationContext.app.installationId,
            fakeClientKey
          )
          val mockHost = mock[AtlassianHost]

          (mockForgeInstallationRepository.findByInstallationId _)
            .expects(fakeForgeInvocationContext.app.installationId)
            .returning(Future.successful(Some(fakeForgeInstallation)))

          (mockHostRepository.findByClientKey _)
            .expects(fakeClientKey)
            .returning(Future.successful(Some(mockHost)))

          val result = await {
            refiner.refine(forgeRemoteContextRequest)
          }
          result mustBe Right(
            ForgeRemoteAssociateAtlassianHostUserRequest(
              DefaultAtlassianHostUser(
                mockHost,
                fakeForgeInvocationContext.principal
              ),
              forgeRemoteContextRequest
            )
          )
        }
      }

      "fail to refine if no Connect mapping exists" in {
        forAll(
          forgeInvocationTokenGen(fakeForgeInvocationContext, keyId, privateKey)
        ) { invocationToken =>
          val forgeRemoteCredentials = ForgeRemoteCredentials(
            "fake-trace-id",
            "fake-span-id",
            invocationToken,
            None,
            None
          )

          val forgeRemoteRequest =
            ForgeRemoteRequest(forgeRemoteCredentials, FakeRequest())

          val forgeRemoteContextRequest =
            ForgeRemoteContextRequest(
              ForgeRemoteContext(
                fakeForgeInvocationContext,
                forgeRemoteCredentials.traceId,
                forgeRemoteCredentials.spanId,
              ),
              forgeRemoteRequest
            )

          (mockForgeInstallationRepository.findByInstallationId _)
            .expects(fakeForgeInvocationContext.app.installationId)
            .returning(Future.successful(None))

          val result = await {
            refiner.refine(forgeRemoteContextRequest)
          }
          result mustBe Left(BadRequest(s"Missing Connect mapping"))
        }
      }

      "fail to refine if Connect installation is missing" in {
        forAll(
          forgeInvocationTokenGen(fakeForgeInvocationContext, keyId, privateKey)
        ) { invocationToken =>
          val forgeRemoteCredentials = ForgeRemoteCredentials(
            "fake-trace-id",
            "fake-span-id",
            invocationToken,
            None,
            None
          )

          val forgeRemoteRequest =
            ForgeRemoteRequest(forgeRemoteCredentials, FakeRequest())

          val forgeRemoteContextRequest =
            ForgeRemoteContextRequest(
              ForgeRemoteContext(
                fakeForgeInvocationContext,
                forgeRemoteCredentials.traceId,
                forgeRemoteCredentials.spanId,
              ),
              forgeRemoteRequest
            )

          val fakeClientKey = "fake-client-key"
          val fakeForgeInstallation = DefaultForgeInstallation(
            fakeForgeInvocationContext.app.installationId,
            fakeClientKey
          )

          (mockForgeInstallationRepository.findByInstallationId _)
            .expects(fakeForgeInvocationContext.app.installationId)
            .returning(Future.successful(Some(fakeForgeInstallation)))

          (mockHostRepository.findByClientKey _)
            .expects(fakeClientKey)
            .returning(Future.successful(None))

          val result = await {
            refiner.refine(forgeRemoteContextRequest)
          }
          result mustBe Left(BadRequest(s"Missing Connect installation"))
        }
      }
    }

    "AssociateMaybeAtlassianHostUserActionRefiner" should {

      val refiner: AssociateMaybeAtlassianHostUserActionRefiner =
        AssociateMaybeAtlassianHostUserActionRefiner(
          mockHostRepository,
          mockForgeInstallationRepository
        )

      "successfully refine to ForgeRemoteAssociateMaybeAtlassianHostUserRequest" in {
        forAll(
          forgeInvocationTokenGen(fakeForgeInvocationContext, keyId, privateKey)
        ) { invocationToken =>
          val forgeRemoteCredentials = ForgeRemoteCredentials(
            "fake-trace-id",
            "fake-span-id",
            invocationToken,
            None,
            None
          )

          val forgeRemoteRequest =
            ForgeRemoteRequest(forgeRemoteCredentials, FakeRequest())

          val forgeRemoteContextRequest =
            ForgeRemoteContextRequest(
              ForgeRemoteContext(
                fakeForgeInvocationContext,
                forgeRemoteCredentials.traceId,
                forgeRemoteCredentials.spanId,
              ),
              forgeRemoteRequest
            )

          val fakeClientKey = "fake-client-key"
          val fakeForgeInstallation = DefaultForgeInstallation(
            fakeForgeInvocationContext.app.installationId,
            fakeClientKey
          )
          val mockHost = mock[AtlassianHost]

          (mockForgeInstallationRepository.findByInstallationId _)
            .expects(fakeForgeInvocationContext.app.installationId)
            .returning(Future.successful(Some(fakeForgeInstallation)))

          (mockHostRepository.findByClientKey _)
            .expects(fakeClientKey)
            .returning(Future.successful(Some(mockHost)))

          val result = await {
            refiner.refine(forgeRemoteContextRequest)
          }
          result mustBe Right(
            ForgeRemoteAssociateMaybeAtlassianHostUserRequest(
              Some(
                DefaultAtlassianHostUser(
                  mockHost,
                  fakeForgeInvocationContext.principal
                )
              ),
              forgeRemoteContextRequest
            )
          )
        }
      }

      "fail to refine if no Connect mapping exists" in {
        forAll(
          forgeInvocationTokenGen(fakeForgeInvocationContext, keyId, privateKey)
        ) { invocationToken =>
          val forgeRemoteCredentials = ForgeRemoteCredentials(
            "fake-trace-id",
            "fake-span-id",
            invocationToken,
            None,
            None
          )

          val forgeRemoteRequest =
            ForgeRemoteRequest(forgeRemoteCredentials, FakeRequest())

          val forgeRemoteContextRequest =
            ForgeRemoteContextRequest(
              ForgeRemoteContext(
                fakeForgeInvocationContext,
                forgeRemoteCredentials.traceId,
                forgeRemoteCredentials.spanId,
              ),
              forgeRemoteRequest
            )

          (mockForgeInstallationRepository.findByInstallationId _)
            .expects(fakeForgeInvocationContext.app.installationId)
            .returning(Future.successful(None))

          val result = await {
            refiner.refine(forgeRemoteContextRequest)
          }
          result mustBe Left(BadRequest(s"Missing Connect mapping"))
        }
      }

      "successfully refine to ForgeRemoteAssociateMaybeAtlassianHostUserRequest with no host if Connect installation is missing" in {
        forAll(
          forgeInvocationTokenGen(fakeForgeInvocationContext, keyId, privateKey)
        ) { invocationToken =>
          val forgeRemoteCredentials = ForgeRemoteCredentials(
            "fake-trace-id",
            "fake-span-id",
            invocationToken,
            None,
            None
          )

          val forgeRemoteRequest =
            ForgeRemoteRequest(forgeRemoteCredentials, FakeRequest())

          val forgeRemoteContextRequest =
            ForgeRemoteContextRequest(
              ForgeRemoteContext(
                fakeForgeInvocationContext,
                forgeRemoteCredentials.traceId,
                forgeRemoteCredentials.spanId,
              ),
              forgeRemoteRequest
            )

          val fakeClientKey = "fake-client-key"
          val fakeForgeInstallation = DefaultForgeInstallation(
            fakeForgeInvocationContext.app.installationId,
            fakeClientKey
          )

          (mockForgeInstallationRepository.findByInstallationId _)
            .expects(fakeForgeInvocationContext.app.installationId)
            .returning(Future.successful(Some(fakeForgeInstallation)))

          (mockHostRepository.findByClientKey _)
            .expects(fakeClientKey)
            .returning(Future.successful(None))

          val result = await {
            refiner.refine(forgeRemoteContextRequest)
          }
          result mustBe Right(
            ForgeRemoteAssociateMaybeAtlassianHostUserRequest(
              None,
              forgeRemoteContextRequest
            )
          )

        }
      }
    }

  }

}
