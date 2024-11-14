package io.toolsplus.atlassian.connect.play.auth.frc.jwt

import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.{JWK, JWKSet, RSAKey}
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper

import java.security.KeyPair
import java.security.interfaces.RSAPublicKey
import scala.jdk.CollectionConverters._

class ForgeJWSVerificationKeySelectorSpec extends TestSpec {
  val keyPair1: KeyPair = JwtTestHelper.generateKeyPair()
  val publicKey1: RSAPublicKey = keyPair1.getPublic.asInstanceOf[RSAPublicKey]

  val keyPair2: KeyPair = JwtTestHelper.generateKeyPair()
  val publicKey2: RSAPublicKey = keyPair1.getPublic.asInstanceOf[RSAPublicKey]

  val fakeForgeInvocationContext: ForgeInvocationContext =
    ForgeInvocationContext(
      App(
        "fake-installation-id",
        "fake-api-base-url",
        "fake-id",
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

  val jwkSourceProvider: ForgeRemoteJWKSourceProvider =
    mock[ForgeRemoteJWKSourceProvider]

  "Given a ForgeJWSVerificationKeySelector" when {

    val keySelector = new ForgeJWSVerificationKeySelector(jwkSourceProvider)

    "selecting JWS keys" should {

      "return matching keys" in {
        (jwkSourceProvider.getJWKSource _)
          .expects(*)
          .returning(
            new ImmutableJWKSet(
              new JWKSet(
                Seq[JWK](
                  new RSAKey.Builder(publicKey1).build(),
                  new RSAKey.Builder(publicKey2).build()
                ).asJava
              )
            )
          )

        val jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).build()
        val result =
          keySelector.selectJWSKeys(jwsHeader, fakeForgeInvocationContext)
        result.size() mustBe 2
      }

      "return empty key set if JWS algo is not RS256" in {
        val jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build()
        val result =
          keySelector.selectJWSKeys(jwsHeader, fakeForgeInvocationContext)
        result.size() mustBe 0
      }
    }

  }
}
