package io.toolsplus.atlassian.connect.play.request.sttp.oauth2.model

import cats.data.ValidatedNec
import cats.implicits._
import io.toolsplus.atlassian.connect.play.api.models.{
  AtlassianHost,
  AuthenticationType,
  OAuth2AuthenticationType
}

sealed trait OAuth2ClientCredentialsAtlassianHostValidator {

  private type ValidationResult[A] = ValidatedNec[Exception, A]

  def validate(host: AtlassianHost)
    : ValidationResult[OAuth2ClientCredentialsAtlassianHost] = {
    (validateAuthenticationType(host.authenticationType),
     validateOAuthClientId(host.oauthClientId),
     validateCloudId(host.cloudId)).mapN(
      (_, oAuthClientId, cloudId) =>
        OAuth2ClientCredentialsAtlassianHost(
          host.clientKey,
          host.key,
          oAuthClientId,
          host.sharedSecret,
          cloudId,
          host.baseUrl,
          host.displayUrl,
          host.displayUrlServicedeskHelpCenter,
          host.productType,
          host.description,
          host.serviceEntitlementNumber,
          host.entitlementId,
          host.entitlementNumber,
          host.installed
      ))
  }

  private def validateAuthenticationType(authenticationType: AuthenticationType)
    : ValidationResult[AuthenticationType] =
    if (authenticationType == OAuth2AuthenticationType)
      OAuth2AuthenticationType.validNec
    else
      new Exception(
        s"Unexpected authentication type $authenticationType instead of $OAuth2AuthenticationType").invalidNec

  private def validateOAuthClientId(
      oauthClientId: Option[String]): ValidationResult[String] =
    oauthClientId match {
      case Some(id) => id.validNec
      case None     => new Exception("Undefined OAuth2 client id").invalidNec
    }

  private def validateCloudId(
      cloudId: Option[String]): ValidationResult[String] = cloudId match {
    case Some(id) => id.validNec
    case None     => new Exception("Undefined cloud id").invalidNec
  }

}

object OAuth2ClientCredentialsAtlassianHostValidator
    extends OAuth2ClientCredentialsAtlassianHostValidator
