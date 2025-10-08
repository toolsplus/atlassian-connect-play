package io.toolsplus.atlassian.connect.play.auth.frc.jwt

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.auth.frc.{
  ForgeRemoteContext,
  ForgeRemoteCredentials
}
import io.toolsplus.atlassian.connect.play.auth.jwt.InvalidJwtError
import io.toolsplus.atlassian.connect.play.models.AtlassianForgeProperties
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import org.scalacheck.Shrink
import org.scalatest.EitherValues
import play.api.Configuration

import java.security.interfaces.RSAPublicKey
import java.security.{KeyPair, PrivateKey}

class ForgeRemoteJwtAuthenticationProviderSpec
    extends TestSpec
    with EitherValues
    with ForgeInvocationTokenGen {
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

  val keyId: String = "0e50fccb-239d-4991-a5db-dc850ba3f236"
  val keyPair: KeyPair = JwtTestHelper.generateKeyPair()
  val publicKey: RSAPublicKey = keyPair.getPublic.asInstanceOf[RSAPublicKey]
  val privateKey: PrivateKey = keyPair.getPrivate

  val mockForgeJWSVerificationKeySelector: ForgeJWSVerificationKeySelector =
    mock[ForgeJWSVerificationKeySelector]

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
        Some(License(true))
      ),
      None,
      None
    )

  val authenticationProvider =
    new ForgeRemoteJwtAuthenticationProvider(
      forgeProperties,
      mockForgeJWSVerificationKeySelector
    )

  "Given a ForgeRemoteJwtAuthenticationProvider" when {

    "authenticate" should {

      def forgeRemoteCredentials(invocationToken: RawJwt) =
        ForgeRemoteCredentials(
          "fake-trace-id",
          "fake-span-id",
          invocationToken,
          None,
          None
        )

      "successfully authenticate Forge Remote credentials" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(
          forgeInvocationTokenGen(fakeForgeInvocationContext, keyId, privateKey)
        ) { invocationToken =>
          (mockForgeJWSVerificationKeySelector.selectJWSKeys _)
            .expects(*, fakeForgeInvocationContext)
            .returning(java.util.List.of(publicKey))

          val result = authenticationProvider
            .authenticate(forgeRemoteCredentials(invocationToken))

          result.value mustBe a[ForgeRemoteContext]
        }
      }

      "fail if FIT is not a JWT" in {
        val result = authenticationProvider
          .authenticate(forgeRemoteCredentials("not-a-jwt"))
        result.left.value mustBe a[InvalidJwtError]
      }

      "fail if FIT is a JWT but does not contain the FIT payload" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(signedAsymmetricJwtStringGen(keyId, privateKey)) {
          notAnInvocationToken =>
            val result = authenticationProvider
              .authenticate(forgeRemoteCredentials(notAnInvocationToken))

            result.left.value mustBe a[InvalidJwtError]
        }
      }

      "fail if FIT is invalid" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        val anotherKeyPair: KeyPair = JwtTestHelper.generateKeyPair()
        forAll(
          forgeInvocationTokenGen(
            fakeForgeInvocationContext,
            keyId,
            anotherKeyPair.getPrivate
          )
        ) {

          (mockForgeJWSVerificationKeySelector.selectJWSKeys _)
            .expects(*, fakeForgeInvocationContext)
            .returning(java.util.List.of(publicKey))

          invocationToken =>
            val result = authenticationProvider
              .authenticate(forgeRemoteCredentials(invocationToken))

            result.left.value mustBe a[InvalidJwtError]
        }
      }

    }

  }

}
