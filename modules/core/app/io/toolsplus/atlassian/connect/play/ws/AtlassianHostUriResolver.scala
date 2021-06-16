package io.toolsplus.atlassian.connect.play.ws

import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost

import java.net.URI
import scala.util.{Failure, Success, Try}

object AtlassianHostUriResolver {

  def baseUrl(uri: URI): Option[String] = {
    Try {
      new URI(uri.getScheme, uri.getAuthority, null, null, null).toString
    } match {
      case Success(url) => if (url.isEmpty) None else Some(url)
      case Failure(_)   => None
    }
  }

  def isRequestToHost(requestUri: URI, host: AtlassianHost): Boolean = {
    val hostBaseUri = URI.create(host.baseUrl)
    !hostBaseUri.relativize(requestUri).isAbsolute
  }
}
