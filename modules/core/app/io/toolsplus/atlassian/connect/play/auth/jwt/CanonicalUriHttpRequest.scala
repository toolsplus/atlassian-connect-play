package io.toolsplus.atlassian.connect.play.auth.jwt

import java.net.URI

import io.lemonlabs.uri.Url
import io.toolsplus.atlassian.jwt.api.CanonicalHttpRequest

case class CanonicalUriHttpRequest(httpMethod: String,
                                   requestUri: URI,
                                   contextPath: String)
    extends CanonicalHttpRequest {

  override def method: String = httpMethod

  /**
    * Removes the context path from the requestUri.
    *
    * For example, if the requestUri is /context/some/request/path and contextPath
    * is /context or https://xyz.atlassian.net/context then this method should return
    * /some/request/path.
    *
    * @return Relative path without the leading context path if it exists.
    */
  override def relativePath: String = {
    val contextPathToRemove = if ("/" == contextPath) "" else contextPath
    Option(requestUri.getPath)
      .filter(_.nonEmpty)
      .map(_.replaceFirst(s"^$contextPathToRemove", ""))
      .map(_.replaceFirst("/$", ""))
      .getOrElse("/")
  }

  override def parameterMap: Map[String, Seq[String]] = Url.parse(requestUri.toString).query.paramMap
}
