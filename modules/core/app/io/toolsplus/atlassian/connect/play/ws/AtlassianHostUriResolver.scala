package io.toolsplus.atlassian.connect.play.ws

import javax.inject.Inject

import com.netaporter.uri.Uri
import UriImplicits._
import com.netaporter.uri.config.UriConfig
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * A helper class for resolving URLs relative to the base URL of an [[io.toolsplus.atlassian.connect.play.api.models.AtlassianHost]].
  */
class AtlassianHostUriResolver @Inject()(
    hostRepository: AtlassianHostRepository) {

  def hostFromRequestUrl(uri: Uri): Future[Option[AtlassianHost]] = {
    if (uri.isAbsolute) {
      uri.baseUrl match {
        case Some(url) =>
          hostRepository.findByBaseUrl(url).flatMap {
            case result @ Some(_) => Future.successful(result)
            case None => findByBaseUrlWithFirstPathElement(uri)
          }
        case None => Future.successful(None)
      }
    } else Future.successful(None)
  }

  private def findByBaseUrlWithFirstPathElement(
      uri: Uri): Future[Option[AtlassianHost]] = {
    baseUrlWithFirstPathElement(uri) match {
      case Some(url) => hostRepository.findByBaseUrl(url)
      case None => Future.successful(None)
    }
  }

  private def baseUrlWithFirstPathElement(uri: Uri): Option[String] = {
    (uri.baseUrl, uri.pathParts.headOption) match {
      case (Some(url), Some(pathElement)) =>
        Some(s"$url/${pathElement.partToString(UriConfig.default)}")
      case _ => None
    }
  }

}

object AtlassianHostUriResolver {

  def isRequestToHost(requestUri: Uri, host: AtlassianHost) = {
    val hostBaseUri = Uri.parse(host.baseUrl)
    !hostBaseUri.toURI.relativize(requestUri.toURI).isAbsolute
  }
}
