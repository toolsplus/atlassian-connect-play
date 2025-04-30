package io.toolsplus.atlassian.connect.play.auth.frc.jwt

import com.nimbusds.jose.jwk.source.JWKSource
import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.models.AtlassianForgeProperties
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import play.api.Configuration

import java.security.KeyPair
import java.security.interfaces.RSAPublicKey

class ForgeRemoteJWKSourceProviderSpec extends TestSpec {
  val jwkSetStagingUrl =
    "https://forge.cdn.stg.atlassian-dev.net/.well-known/jwks.json"
  val jwkSetProductionUrl =
    "https://forge.cdn.prod.atlassian-dev.net/.well-known/jwks.json"

  val config: Configuration =
    Configuration.from(
      Map(
        "atlassian.forge" -> Map(
          "appId" -> "fake-app-id",
          "remote.jwkSetStagingUrl" -> jwkSetStagingUrl,
          "remote.jwkSetProductionUrl" -> jwkSetProductionUrl
        )
      ))
  val forgeProperties = new AtlassianForgeProperties(config)

  val keyPair1: KeyPair = JwtTestHelper.generateKeyPair()
  val publicKey1: RSAPublicKey = keyPair1.getPublic.asInstanceOf[RSAPublicKey]

  val keyPair2: KeyPair = JwtTestHelper.generateKeyPair()
  val publicKey2: RSAPublicKey = keyPair1.getPublic.asInstanceOf[RSAPublicKey]

  val jwkSourceProvider: ForgeRemoteJWKSourceProvider =
    mock[ForgeRemoteJWKSourceProvider]

  val fakeForgeInvocationContext: ForgeInvocationContext =
    ForgeInvocationContext(
      App("fake-installation-id",
          "fake-api-base-url",
          "fake-id",
          "fake-app-version",
          Environment("fake-type", "fake-id"),
          Module("fake-type", "fake-key"),
          None),
      None,
      None
    )

  "Given a ForgeRemoteJWKSourceProvider" when {

    val jwkSourceProvider = new ForgeRemoteJWKSourceProvider(forgeProperties)

    "getting JWK source" should {

      "get staging JWK source" in {
        val result = jwkSourceProvider.getJWKSource(
          fakeForgeInvocationContext.copy(app = fakeForgeInvocationContext.app
            .copy(apiBaseUrl = "https://api.stg.atlassian.com/fake/url")))
        result mustBe a[JWKSource[_]]
      }

      "get production JWK source" in {
        val result = jwkSourceProvider.getJWKSource(
          fakeForgeInvocationContext.copy(app = fakeForgeInvocationContext.app
            .copy(apiBaseUrl = "https://api.atlassian.com/fake/url")))
        result mustBe a[JWKSource[_]]
      }
    }

  }
}
