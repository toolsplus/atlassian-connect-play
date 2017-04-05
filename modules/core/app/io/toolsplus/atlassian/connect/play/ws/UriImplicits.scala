package io.toolsplus.atlassian.connect.play.ws

import java.net.URI

import com.netaporter.uri.Uri

import scala.util.{Failure, Success, Try}

object UriImplicits {

  implicit class UriHelpers(uri: Uri) {

    def isAbsolute: Boolean = uri.toURI.isAbsolute

    def baseUrl: Option[String] =
      Try {
        new URI(uri.toURI.getScheme, uri.toURI.getAuthority, null, null, null).toString
      } match {
        case Success(url) => if (url.isEmpty) None else Some(url)
        case Failure(_) => None
      }

    def append(other: Uri): Uri = Uri.parse(s"$uri$other")

    def asRelativeUri: Uri =
      uri.copy(scheme = None,
               user = None,
               password = None,
               host = None,
               port = None)

  }

}
