package io.toolsplus.atlassian.connect.play.request.ws

import java.net.URI
import io.lemonlabs.uri.Url
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.auth.jwt.symmetric.JwtGenerator
import io.toolsplus.atlassian.connect.play.request.ws.jwt.JwtSignatureCalculator

import javax.inject.Inject
import play.api.libs.ws.{WSClient, WSRequest, WSSignatureCalculator}

/**
  * A helper class for obtaining pre-configured WSRequests to make authenticated requests to Atlassian hosts.
  *
  * To make requests using JWT, the app must specify the authentication type `jwt` in its app descriptor.
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
class PlayWsAtlassianConnectHttpClient @Inject()(ws: WSClient,
                                                 jwtGenerator: JwtGenerator) {

  def authenticatedAsAddon(url: String)(
      implicit host: AtlassianHost): WSRequest =
    request(url, JwtSignatureCalculator(jwtGenerator))

  private def request(url: String, signatureCalculator: WSSignatureCalculator)(
      implicit host: AtlassianHost) = {
    val requestUri = URI.create(url)
    val absoluteUrl =
      if (!requestUri.isAbsolute) absoluteRequestUrl(requestUri, host).toString
      else url
    ws.url(absoluteUrl).sign(signatureCalculator)
  }

  private def absoluteRequestUrl(requestUri: URI, host: AtlassianHost): URI = {
    val baseUrl = Url.parse(host.baseUrl)
    val requestUrl = Url.parse(requestUri.toString)
    URI.create(
      baseUrl
        .withPath(baseUrl.path.addParts(requestUrl.path.parts))
        .withQueryString(requestUrl.query)
        .withFragment(requestUrl.fragment)
        .toString)
  }
}
