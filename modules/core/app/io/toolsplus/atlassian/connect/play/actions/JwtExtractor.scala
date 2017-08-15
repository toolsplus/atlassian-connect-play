package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.auth.jwt.{
  CanonicalPlayHttpRequest,
  JwtCredentials
}
import play.api.http.HeaderNames
import play.api.mvc.Request

object JwtExtractor {
  def extractJwt[A](request: Request[A]): Option[JwtCredentials] = {
    extractJwtFromHeader(request)
      .orElse(extractJwtFromParameter(request))
      .map(JwtCredentials(_, CanonicalPlayHttpRequest(request)))
  }

  private def extractJwtFromHeader[A](request: Request[A]): Option[String] = {
    request.headers
      .get(HeaderNames.AUTHORIZATION)
      .filter(header =>
        !header.isEmpty && header.startsWith(AUTHORIZATION_HEADER_PREFIX))
      .map(_.substring(AUTHORIZATION_HEADER_PREFIX.length).trim)
  }

  private def extractJwtFromParameter[A](request: Request[A]): Option[String] = {
    request.getQueryString(QUERY_PARAMETER_NAME).filter(!_.isEmpty)
  }

  private[actions] val AUTHORIZATION_HEADER_PREFIX = "JWT "
  private[actions] val QUERY_PARAMETER_NAME = "jwt"
}
