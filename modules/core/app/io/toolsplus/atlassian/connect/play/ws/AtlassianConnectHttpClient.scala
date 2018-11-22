package io.toolsplus.atlassian.connect.play.ws

import javax.inject.Inject

import com.netaporter.uri.Uri
import UriImplicits._
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.auth.jwt.JwtGenerator
import io.toolsplus.atlassian.connect.play.ws.jwt.JwtSignatureCalculator
import play.api.libs.ws.{WSClient, WSRequest, WSSignatureCalculator}

/**
  * A helper class for obtaining pre-configured WSRequests to make authenticated requests to Atlassian hosts.
  *
  * ==JWT==
  *
  * To make requests using JWT, the add-on must specify the authentication type `jwt` in its add-on descriptor.
  *
  * To obtain a WSRequest using JWT authentication, use authenticatedAsAddon():
  * {{{
  * class MyRestClient @Inject()(httpClient: AtlassianConnectHttpClient) {
  *
  *   def fetchIssue(issueKey: String): Future[WSResponse] = {
  *      httpClient.authenticatedAsAddon(s"/rest/api/2/issue/{issueKey}").get
  *   }
  * }
  * }}}
  */
class AtlassianConnectHttpClient @Inject()(ws: WSClient,
                                           jwtGenerator: JwtGenerator) {

  def authenticatedAsAddon(url: String)(
      implicit host: AtlassianHost): WSRequest =
    request(url, JwtSignatureCalculator(jwtGenerator))

  private def request(url: String, signatureCalculator: WSSignatureCalculator)(
      implicit host: AtlassianHost) = {
    val requestUri = Uri.parse(url)
    val absoluteUrl =
      if (!requestUri.isAbsolute) absoluteRequestUrl(requestUri, host).toString
      else url
    ws.url(absoluteUrl).sign(signatureCalculator)
  }

  private def absoluteRequestUrl(requestUri: Uri, host: AtlassianHost) = {
    val baseUri = Uri.parse(host.baseUrl)
    val fullRelativeUri = Uri(
      pathParts = baseUri.pathParts ++ requestUri.pathParts)
    Uri(baseUri.toURI.resolve(fullRelativeUri.toURI))
  }

}
