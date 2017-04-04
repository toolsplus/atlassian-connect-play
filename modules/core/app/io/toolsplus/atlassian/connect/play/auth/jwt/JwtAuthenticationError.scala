package io.toolsplus.atlassian.connect.play.auth.jwt

sealed abstract class JwtAuthenticationError extends Exception {
  final override def fillInStackTrace(): Throwable = this
}

/**
  * Indicates that an authentication request is rejected because the
  * credentials are invalid.
  */
final case class JwtBadCredentialsError(message: String)
    extends JwtAuthenticationError {
  override def getMessage: String = message
}

final case class InvalidJwtError(message: String)
    extends JwtAuthenticationError {
  override def getMessage: String = message
}

final case class UnknownJwtIssuerError(issuer: String)
    extends JwtAuthenticationError {
  override def getMessage: String =
    s"Could not find an installed host for the provided client key: $issuer"
}
