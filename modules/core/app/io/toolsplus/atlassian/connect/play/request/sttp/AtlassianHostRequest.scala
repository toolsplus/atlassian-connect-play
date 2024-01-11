package io.toolsplus.atlassian.connect.play.request.sttp

import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import sttp.client3.{Empty, RequestT, basicRequest}

object AtlassianHostRequest {

  private val atlassianHostTagKey = "ATLASSIAN_HOST"

  def atlassianHostRequest(implicit host: AtlassianHost)
  : RequestT[Empty, Either[String, String], Any] =
    basicRequest.tag(atlassianHostTagKey, host)

  implicit class RequestTExtensions[U[_], T, -R](r: RequestT[U, T, R]) {
    def atlassianHost: Either[Exception, AtlassianHost] =
      r.tag(atlassianHostTagKey) match {
        case Some(host) =>
          host match {
            case h: AtlassianHost => Right(h)
            case h =>
              Left(new Exception(
                s"Failed to extract Atlassian host from request: Invalid host type '${h.getClass.getName}', expected '${classOf[
                  AtlassianHost].getName}'"))
          }
        case None =>
          Left(new Exception(
            "Failed to extract Atlassian host from request: No host configured. Use `atlassianHostRequest` to create a request that is associated with a host"))
      }
  }
}

