package io.toolsplus.atlassian.connect.play.generators

import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import org.scalacheck.Gen
import org.scalacheck.Gen._

case class SecurityContext(key: String,
                           clientKey: ClientKey,
                           oauthClientId: Option[String],
                           sharedSecret: String,
                           baseUrl: String,
                           displayUrl: String,
                           displayUrlServicedeskHelpCenter: String,
                           productType: String,
                           description: String,
                           serviceEntitlementNumber: Option[String],
                           entitlementId: Option[String],
                           entitlementNumber: Option[String])

/**
 * Security context generator generates a security context as provided in the
 * Atlassian Connect installed event. It can be used to generate objects of
 * type [[io.toolsplus.atlassian.connect.play.models.LifecycleEvent]] or
 * [[io.toolsplus.atlassian.connect.play.api.models.AtlassianHost]].
 */
trait SecurityContextGen {

  def alphaNumStr: Gen[String] =
    listOf(alphaNumChar).map(_.mkString)

  def clientKeyGen: Gen[ClientKey] = alphaNumStr

  def productTypeGen: Gen[String] = oneOf("jira", "confluence")

  def hostBaseUrlGen: Gen[String] = alphaStr.suchThat(_.nonEmpty).map(hostName => s"https://$hostName.atlassian.net")

  def securityContextGen: Gen[SecurityContext] =
    for {
      key <- alphaStr
      clientKey <- clientKeyGen
      oauthClientId <- option(alphaNumStr)
      sharedSecret <- alphaNumStr.suchThat(s => s.length >= 32 && s.nonEmpty)
      baseUrl <- hostBaseUrlGen
      productType <- productTypeGen
      description <- alphaStr
      serviceEntitlementNumber <- option(numStr)
      entitlementId <- option(alphaNumStr)
      entitlementNumber <- option(alphaNumStr)

    } yield
      SecurityContext(key,
        clientKey,
        oauthClientId,
        sharedSecret,
        baseUrl,
        baseUrl,
        baseUrl,
        productType,
        description,
        serviceEntitlementNumber,
        entitlementId,
        entitlementNumber)

}
