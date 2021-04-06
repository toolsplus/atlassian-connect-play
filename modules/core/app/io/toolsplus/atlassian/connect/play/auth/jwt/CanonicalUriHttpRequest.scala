package io.toolsplus.atlassian.connect.play.auth.jwt

import java.net.URI

import io.lemonlabs.uri.Url
import io.toolsplus.atlassian.jwt.api.CanonicalHttpRequest

/**
  * HTTP request that can be signed for use as a JWT claim.
  *
  * @param httpMethod HTTP method (e.g. "GET", "POST" etc).
  * @param requestUri Request URI, either an absolute or relative request URI
  * @param contextPath Context path indicates the path part that belongs to the host base URL. This part should be
  *                    removed from the beginning of the provided request URI path to get the relative URI.
  */
case class CanonicalUriHttpRequest(private val httpMethod: String,
                                   private val requestUri: URI,
                                   private val contextPath: Option[String])
    extends CanonicalHttpRequest {

  override def method: String = httpMethod.toUpperCase

  /**
    * Removes the context path from the requestUri.
    *
    * For example, if the requestUri is /context/some/request/path and contextPath
    * is /context or https://xyz.atlassian.net/context then this method should return
    * /some/request/path.
    *
    * @return Relative path without the leading context path if it exists.
    */
  override def relativePath: String = Option(requestUri.getPath) match {
    case Some("") | Some("/") => "/"
    case maybePath =>
      val contextPathToRemove =
        contextPath.map(c => if (c == "/") "" else c).getOrElse("")
      maybePath
        .filter(_.nonEmpty)
        .map(
          path =>
            if (path.startsWith(contextPathToRemove))
              path.substring(contextPathToRemove.length)
            else path)
        .map(path => if (path.endsWith("/") && path != "/") path.dropRight(1) else path)
        .getOrElse("/")
  }

  override def parameterMap: Map[String, Seq[String]] =
    Url.parse(requestUri.toString).query.paramMap
}
