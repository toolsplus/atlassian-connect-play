package io.toolsplus.atlassian.connect.play.api.models

/**
  * Specifies the authentication scheme that the app uses to communicate with Atlassian products for the host site.
  */
sealed trait AuthenticationType {
  def identifier: String
}

/**
  * Default JWT authentication scheme for Connect apps
  */
case object JwtAuthenticationType extends AuthenticationType {
  override val identifier = "jwt"
}

/**
  * OAuth 2.0 authentication scheme for Connect-on-Forge apps
  */
case object OAuth2AuthenticationType extends AuthenticationType {
  override val identifier = "oauth2"
}
