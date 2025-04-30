package io.toolsplus.atlassian.connect.play.auth.frc.jwt

import io.toolsplus.atlassian.connect.play.generators.AtlassianConnectJwtGen
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import org.scalacheck.Gen
import scala.jdk.CollectionConverters._

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
          "app" -> toJson(invocationContext.app)
        ) ++ Seq("context" -> invocationContext.context,
                 "principal" -> invocationContext.principal)
          .filter(_._2.isDefined) ++ customClaims,
      )
    } yield jwt

  /**
    * Converts the app case class deeply to JSON entities.
    *
    * While the underlying GSon library manages to convert almost anything to JSON the result
    * is sometimes not clear, in particular when dealing with Option values.
    *
    * @see https://connect2id.com/products/nimbus-jose-jwt/examples/json-entity-mapping
    *
    * @param app App instance to convert
    */
  private def toJson(app: App) = {
    Map(
      "installationId" -> app.installationId,
      "apiBaseUrl" -> app.apiBaseUrl,
      "id" -> app.id,
      "appVersion" -> app.appVersion,
      "environment" -> Map(
        "type" -> app.environment.`type`,
        "id" -> app.environment.id
      ).asJava,
      "module" -> Map(
        "type" -> app.module.`type`,
        "key" -> app.module.key
      ).asJava,
      "license" -> (app.license match {
        case Some(license) =>
          Map(
            "isActive" -> license.isActive
          ).asJava
        case None => null
      })
    ).asJava
  }
}
