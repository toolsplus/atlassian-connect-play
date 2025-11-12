package io.toolsplus.atlassian.connect.play.auth.frc.jwt

import com.nimbusds.jose.jwk.{JWKMatcher, JWKSelector, KeyConverter}
import com.nimbusds.jose.proc.JWSKeySelector
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}

import java.security.{Key, PublicKey}
import java.util
import java.util.Collections
import javax.crypto.SecretKey
import javax.inject.Inject
import scala.jdk.CollectionConverters._

/** Forge Remote specific JWS key selector for verifying JWS objects, where the
  * key candidates are retrieved from a request specific [[JWKSource]] JSON Web
  * Key (JWK) source.
  *
  * This implementation follows exactly the implementation in
  * [[com.nimbusds.jose.proc.JWSVerificationKeySelector]] except that the key
  * selector selects the [[JWKSource]] based on the associated
  * [[ForgeInvocationContext]] security context.
  */
class ForgeJWSVerificationKeySelector @Inject() (
    jwkSourceProvider: ForgeRemoteJWKSourceProvider
) extends JWSKeySelector[ForgeInvocationContext] {
  override def selectJWSKeys(
      header: JWSHeader,
      context: ForgeInvocationContext
  ): util.List[_ <: Key] = {
    if (header.getAlgorithm != JWSAlgorithm.RS256) {
      return Collections.emptyList()
    }
    Option(JWKMatcher.forJWSHeader(header)) match {
      case Some(jwkMatcher) =>
        val jwkMatches =
          jwkSourceProvider
            .getJWKSource(context)
            .get(new JWKSelector(jwkMatcher), context)
        KeyConverter
          .toJavaKeys(jwkMatches)
          .asScala
          // skip asymmetric private keys
          .filter(key =>
            key.isInstanceOf[PublicKey] || key
              .isInstanceOf[SecretKey]
          )
          .asJava
      case None => Collections.emptyList()
    }
  }
}
