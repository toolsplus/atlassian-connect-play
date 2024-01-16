package io.toolsplus.atlassian.connect.play.auth.frc.jwt

import com.nimbusds.jose.jwk.source.{JWKSource, JWKSourceBuilder}
import com.nimbusds.jose.util.DefaultResourceRetriever
import io.toolsplus.atlassian.connect.play.models.AtlassianForgeProperties

import java.net.URL
import javax.inject.Inject
import ForgeRemoteJWKSourceProvider._

class ForgeRemoteJWKSourceProvider @Inject()(
    forgeProperties: AtlassianForgeProperties) {

  /**
    * Selects the JWK source based on the API base URL declared in the Forge invocation context.
    *
    * @param context Forge invocation context associated with the request
    * @return JWK source based on the given context
    */
  def getJWKSource(
      context: ForgeInvocationContext): JWKSource[ForgeInvocationContext] = {
    val jwkSetUrl =
      if (context.app.apiBaseUrl.startsWith("https://api.stg.atlassian.com/"))
        forgeProperties.forgeRemoteJWKSetStagingUrl
      else {
        forgeProperties.forgeRemoteJWKSetProductionUrl
      }

    JWKSourceBuilder
      .create[ForgeInvocationContext](
        new URL(jwkSetUrl),
        new DefaultResourceRetriever(remoteJWKSetHttpConnectTimeoutMs,
                                     remoteJWKSetHttpReadTimeoutMs,
                                     remoteJWKSetHttpEntitySizeLimitByte))
      .retrying(true)
      .build()
  }

}

object ForgeRemoteJWKSourceProvider {
  private val remoteJWKSetHttpConnectTimeoutMs: Int = 1000
  private val remoteJWKSetHttpReadTimeoutMs: Int = 1000
  private val remoteJWKSetHttpEntitySizeLimitByte: Int = 100 * 1024
}
