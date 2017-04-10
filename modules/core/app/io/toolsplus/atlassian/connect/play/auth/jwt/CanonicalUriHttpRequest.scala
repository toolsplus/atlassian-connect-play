package io.toolsplus.atlassian.connect.play.auth.jwt

import com.netaporter.uri.Uri
import io.toolsplus.atlassian.jwt.api.CanonicalHttpRequest

case class CanonicalUriHttpRequest(httpMethod: String,
                                   requestUri: Uri,
                                   contextPath: String)
    extends CanonicalHttpRequest {

  override def method = httpMethod

  override def relativePath = {
    val relPath = requestUri.path
      .replaceFirst(s"^${Uri.parse(contextPath)}", "")
      .replaceFirst("/$", "")
    if (relPath.isEmpty) "/" else Uri.parse(relPath).path
  }

  override def parameterMap = requestUri.query.paramMap
}
