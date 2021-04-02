package io.toolsplus.atlassian.connect.play.generators

import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import org.scalacheck.Gen
import org.scalacheck.Gen._

case class SecurityContext(key: String,
                           clientKey: ClientKey,
                           publicKey: String,
                           oauthClientId: Option[String],
                           sharedSecret: String,
                           serverVersion: String,
                           pluginsVersion: String,
                           baseUrl: String,
                           productType: String,
                           description: String,
                           serviceEntitlementNumber: Option[String])

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

  def pluginVersionGen: Gen[String] =
    listOfN(3, posNum[Int]).map(n => n.mkString("."))

  def productTypeGen: Gen[String] = oneOf("jira", "confluence")

  def hostBaseUrlGen: Gen[String] =  alphaStr.suchThat(_.nonEmpty).map(hostName => s"https://$hostName.atlassian.net")

  def securityContextGen: Gen[SecurityContext]=
    for {
      key <- alphaStr
      clientKey <- clientKeyGen
      publicKey <- alphaNumStr
      oauthClientId <- option(alphaNumStr)
      sharedSecret <- alphaNumStr.suchThat(s => s.length >= 32 && s.nonEmpty)
      serverVersion <- numStr
      pluginsVersion <- pluginVersionGen
      baseUrl <- hostBaseUrlGen
      productType <- productTypeGen
      description <- alphaStr
      serviceEntitlementNumber <- option(numStr)
    } yield
      SecurityContext(key,
       clientKey,
       publicKey,
       oauthClientId,
       sharedSecret,
       serverVersion,
       pluginsVersion,
        baseUrl,
       productType,
       description,
       serviceEntitlementNumber)

}
