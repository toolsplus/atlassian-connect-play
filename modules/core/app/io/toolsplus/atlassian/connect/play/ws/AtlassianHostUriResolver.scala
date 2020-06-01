package io.toolsplus.atlassian.connect.play.ws

import java.net.URI

import javax.inject.Inject
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * A helper class for resolving URLs relative to the base URL of an AtlassianHost.
  */
class AtlassianHostUriResolver @Inject()(
    hostRepository: AtlassianHostRepository) {

  def hostFromRequestUrl(uri: URI): Future[Option[AtlassianHost]] = {
    if (uri.isAbsolute) {
      AtlassianHostUriResolver.baseUrl(uri) match {
        case Some(url) => hostRepository.findByBaseUrl(url)
        case None      => Future.successful(None)
      }
    } else Future.successful(None)
  }
}

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
