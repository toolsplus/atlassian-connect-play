package io.toolsplus.atlassian.connect.play.auth.frc.jwt

import io.toolsplus.atlassian.connect.play.generators.AtlassianConnectJwtGen
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import org.scalacheck.Gen

import java.security.PrivateKey
import java.time.ZonedDateTime
import java.util.Date

trait ForgeInvocationTokenGen extends AtlassianConnectJwtGen {

  def forgeInvocationTokenGen(
      invocationContext: ForgeInvocationContext,
      keyId: String,
      privateKey: PrivateKey,
      customClaims: Seq[(String, Any)] = Seq.empty
  ): Gen[RawJwt] =
    for {
      jti <- Gen.alphaNumStr
      jwt <- signedAsymmetricJwtStringGen(
        keyId,
        privateKey,
        Seq(
          "iss" -> "forge/invocation-token",
          "aud" -> invocationContext.app.id,
          "jti" -> jti,
          "nbf" -> Date
            .from(ZonedDateTime.now.minusMinutes(5).toInstant),
          "app" -> invocationContext.app
        ) ++ Seq("context" -> invocationContext.context,
                 "principal" -> invocationContext.principal)
          .filter(_._2.isDefined) ++ customClaims,
      )
    } yield jwt
}
