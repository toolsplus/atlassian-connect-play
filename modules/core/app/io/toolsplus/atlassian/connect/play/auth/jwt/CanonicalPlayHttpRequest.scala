package io.toolsplus.atlassian.connect.play.auth.jwt

import io.toolsplus.atlassian.jwt.api.CanonicalHttpRequest
import play.api.mvc.Request

case class CanonicalPlayHttpRequest[A](request: Request[A])
    extends CanonicalHttpRequest {
  override def method: String = request.method

  override def relativePath: String = request.path

  override def parameterMap: Map[String, Seq[String]] = request.queryString
}
