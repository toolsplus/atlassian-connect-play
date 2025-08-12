package io.toolsplus.atlassian.connect.play.generators

import com.fortysevendeg.scalacheck.datetime.GenDateTime.genDateTimeWithinRange
import io.toolsplus.atlassian.connect.play.api.models.{
  DefaultAtlassianHost,
  DefaultAtlassianHostUser
}
import org.scalacheck.Gen
import org.scalacheck.Gen._
import com.fortysevendeg.scalacheck.datetime.instances.jdk8._

import java.time.{Duration, Instant}

trait AtlassianHostGen extends SecurityContextGen {

  def atlassianHostGen: Gen[DefaultAtlassianHost] =
    for {
      securityContext <- securityContextGen
      installed <- oneOf(true, false)
      ttl <-
        if (installed) const(None)
        else
          genDateTimeWithinRange(Instant.now(), Duration.ofDays(30))
            .map(Some(_))
    } yield {
      DefaultAtlassianHost(
        securityContext.clientKey,
        securityContext.key,
        securityContext.oauthClientId,
        securityContext.installationId,
        securityContext.sharedSecret,
        securityContext.baseUrl,
        securityContext.displayUrl,
        securityContext.displayUrlServicedeskHelpCenter,
        securityContext.productType,
        securityContext.description,
        securityContext.serviceEntitlementNumber,
        securityContext.entitlementId,
        securityContext.entitlementNumber,
        installed,
        ttl
      )
    }

  def atlassianHostUserGen: Gen[DefaultAtlassianHostUser] =
    for {
      atlassianHost <- atlassianHostGen
      userAccountId <- option(alphaStr)
    } yield DefaultAtlassianHostUser(atlassianHost, userAccountId)

}
