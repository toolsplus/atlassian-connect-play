package io.toolsplus.atlassian.connect.play.ws

import javax.inject.Inject

import com.netaporter.uri.Uri
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.ws.UriImplicits._

import scala.concurrent.Future

/**
  * A helper class for resolving URLs relative to the base URL of an AtlassianHost.
  */
class AtlassianHostUriResolver @Inject()(
    hostRepository: AtlassianHostRepository) {

  def hostFromRequestUrl(uri: Uri): Future[Option[AtlassianHost]] = {
    if (uri.isAbsolute) {
      uri.baseUrl match {
        case Some(url) => hostRepository.findByBaseUrl(url)
        case None => Future.successful(None)
      }
    } else Future.successful(None)
  }

}

object AtlassianHostUriResolver {

  def isRequestToHost(requestUri: Uri, host: AtlassianHost) = {
    val hostBaseUri = Uri.parse(host.baseUrl)
    !hostBaseUri.toURI.relativize(requestUri.toURI).isAbsolute
  }
}
