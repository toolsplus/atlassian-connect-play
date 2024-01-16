package io.toolsplus.atlassian.connect.play.auth.frc.jwt

import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jose.jwk.{JWKMatcher, JWKSelector, KeyConverter}
import com.nimbusds.jose.jwk.source.{JWKSource, JWKSourceBuilder}
import com.nimbusds.jose.proc.JWSKeySelector
import com.nimbusds.jose.util.DefaultResourceRetriever
import io.toolsplus.atlassian.connect.play.auth.frc.jwt.ForgeJWSVerificationKeySelector.{
  remoteJWKSetHttpConnectTimeoutMs,
  remoteJWKSetHttpEntitySizeLimitByte,
  remoteJWKSetHttpReadTimeoutMs
}
import io.toolsplus.atlassian.connect.play.models.AtlassianForgeProperties

import java.net.URL
import java.security.{Key, PublicKey}
import java.util.Collections
import javax.crypto.SecretKey
import scala.jdk.CollectionConverters._
import java.util
import javax.inject.Inject

/**
  * Forge Remote specific JWS key selector for verifying JWS objects, where the key candidates are
  * retrieved from a request specific [[JWKSource JSON Web Key (JWK) source]].
  *
  * This implementation follows exactly the implementation in [[com.nimbusds.jose.proc.JWSVerificationKeySelector]]
  * except that the key selector selects the [[JWKSource]] based on the associated [[ForgeInvocationContext]]
  * security context.
  */
class ForgeJWSVerificationKeySelector @Inject()(
    forgeProperties: AtlassianForgeProperties)
    extends JWSKeySelector[ForgeInvocationContext] {
  override def selectJWSKeys(
      header: JWSHeader,
      context: ForgeInvocationContext): util.List[_ <: Key] = {
    if (header.getAlgorithm != JWSAlgorithm.RS256) {
      return Collections.emptyList()
    }
    Option(JWKMatcher.forJWSHeader(header)) match {
      case Some(jwkMatcher) =>
        val jwkMatches =
          getJWKSource(context).get(new JWKSelector(jwkMatcher), context)
        KeyConverter
          .toJavaKeys(jwkMatches)
          .asScala
          // skip asymmetric private keys
          .filter(key =>
            key.isInstanceOf[PublicKey] || key
              .isInstanceOf[SecretKey])
          .asJava
      case None => Collections.emptyList()
    }
  }

  /**
    * Selects the JWK source based on the API base URL declared in the Forge invocation context.
    *
    * @param context Forge invocation context associated with the request
    * @return JWK source based on the given context
    */
  private def getJWKSource(
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

object ForgeJWSVerificationKeySelector {
  val remoteJWKSetHttpConnectTimeoutMs: Int = 1000
  val remoteJWKSetHttpReadTimeoutMs: Int = 1000
  val remoteJWKSetHttpEntitySizeLimitByte: Int = 100 * 1024
}
