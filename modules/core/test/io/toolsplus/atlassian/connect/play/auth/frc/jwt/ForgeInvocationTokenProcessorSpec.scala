package io.toolsplus.atlassian.connect.play.auth.frc.jwt

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.BadJWTException
import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import org.scalacheck.Shrink

import java.security.interfaces.RSAPublicKey
import java.security.{KeyPair, PrivateKey}
import java.time.ZonedDateTime
import java.util.Date
import scala.util.Try

class ForgeInvocationTokenProcessorSpec
    extends TestSpec
    with ForgeInvocationTokenGen {
  val appId = "fake-app-id"

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

  "Given a ForgeInvocationTokenProcessor" when {

    val processor =
      ForgeInvocationTokenProcessor.create(
        appId,
        mockForgeJWSVerificationKeySelector
      )

    "processing JWTs" should {

      "successfully validate Forge Invocation Tokens" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(
          forgeInvocationTokenGen(fakeForgeInvocationContext, keyId, privateKey)
        ) { jwt =>
          (mockForgeJWSVerificationKeySelector.selectJWSKeys _)
            .expects(*, fakeForgeInvocationContext)
            .returning(java.util.List.of(publicKey))

          processor
            .process(jwt, fakeForgeInvocationContext) mustBe a[JWTClaimsSet]
        }
      }

      "fail if issuer is not 'forge/invocation-token'" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(
          signedAsymmetricJwtStringGen(
            keyId,
            privateKey,
            Seq(
              "iss" -> "invalid/issuer",
              "aud" -> appId,
              "jti" -> "d8a496253ec8c18a54631e4c82cbedd5d0ae8570",
              "nbf" -> Date.from(ZonedDateTime.now.minusMinutes(5).toInstant)
            )
          )
        ) { jwt =>
          (mockForgeJWSVerificationKeySelector.selectJWSKeys _)
            .expects(*, fakeForgeInvocationContext)
            .returning(java.util.List.of(publicKey))

          val result = Try(processor.process(jwt, fakeForgeInvocationContext))

          result.failed.get mustBe a[BadJWTException]
          result.failed.get.getMessage must include(
            "JWT iss claim has value invalid/issuer, must be forge/invocation-token"
          )
        }
      }

      "fail if audience is not matching the app id" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        val notTheAppId = "not-the-app-id"
        forAll(
          signedAsymmetricJwtStringGen(
            keyId,
            privateKey,
            Seq(
              "iss" -> "forge/invocation-token",
              "aud" -> notTheAppId,
              "jti" -> "d8a496253ec8c18a54631e4c82cbedd5d0ae8570",
              "nbf" -> Date.from(ZonedDateTime.now.minusMinutes(5).toInstant)
            )
          )
        ) { jwt =>
          (mockForgeJWSVerificationKeySelector.selectJWSKeys _)
            .expects(*, fakeForgeInvocationContext)
            .returning(java.util.List.of(publicKey))

          val result = Try(processor.process(jwt, fakeForgeInvocationContext))

          result.failed.get mustBe a[BadJWTException]
          result.failed.get.getMessage must include(
            s"JWT aud claim has value [$notTheAppId], must be [$appId]"
          )
        }

      }

      "fail if required claims are missing" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(
          signedAsymmetricJwtStringGen(
            keyId,
            privateKey,
            Seq(
              "iss" -> "forge/invocation-token",
              "aud" -> appId
            )
          )
        ) { jwt =>
          (mockForgeJWSVerificationKeySelector.selectJWSKeys _)
            .expects(*, fakeForgeInvocationContext)
            .returning(java.util.List.of(publicKey))

          val result = Try(processor.process(jwt, fakeForgeInvocationContext))

          result.failed.get mustBe a[BadJWTException]
          result.failed.get.getMessage must include(
            "JWT missing required claims: [jti, nbf]"
          )
        }

      }

    }

  }

}
