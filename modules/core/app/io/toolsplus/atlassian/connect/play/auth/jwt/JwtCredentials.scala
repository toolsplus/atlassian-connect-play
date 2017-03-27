package io.toolsplus.atlassian.connect.play.auth.jwt

import io.toolsplus.atlassian.jwt.api.CanonicalHttpRequest

/**
  * Authentication credentials representing an unverified JSON Web Token.
  */
case class JwtCredentials(rawJwt: String,
                          canonicalHttpRequest: CanonicalHttpRequest)
