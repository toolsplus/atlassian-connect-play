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
        !header.isEmpty && header.startsWith(AuthorizationHeaderPrefix))
      .map(_.substring(AuthorizationHeaderPrefix.length).trim)
  }

  private def extractJwtFromParameter[A](request: Request[A]): Option[String] = {
    request.getQueryString(QueryParameterName).filter(!_.isEmpty)
  }

  private[actions] val AuthorizationHeaderPrefix = "JWT "
  private[actions] val QueryParameterName = "jwt"
}
