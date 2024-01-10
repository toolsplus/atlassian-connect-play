package io.toolsplus.atlassian.connect.play.request.sttp.oauth2

import io.toolsplus.atlassian.connect.play.request.AtlassianUrlUtils.{
  PRODUCTION_API_GATEWAY_HOST_URL,
  STAGING_API_GATEWAY_URL_HOST_URL,
  apiGatewayUrl
}
import io.toolsplus.atlassian.connect.play.request.sttp.oauth2.model.OAuth2ClientCredentialsAtlassianHost
import sttp.client3.UriContext
import sttp.model.Uri

object AtlassianHostUriResolver {

  /**
    * Resolves the given request URI to an absolute URI to the API gateway.
    *
    * If the given request URI is an absolute URI to an API gateway URL returns the same URI.
    *
    * The given host is expected to be an OAuth2 enabled installation and should have the cloud ID defined. If that is
    * not the case this will return an error.
    *
    * @param requestUri Request URI to resolve
    * @param host Atlassian host associated with this request
    * @return Absolute request URI pointing pointing to the API gateway or error if for whatever reason the given host
    *         does not have a cloudId.
    */
  def resolveToAbsoluteApiGatewayUri(
      requestUri: Uri,
      host: OAuth2ClientCredentialsAtlassianHost): Either[Exception, Uri] = {
    if (isApiGatewayRequest(requestUri)) Right(requestUri)
    else
      Right(
        uri"${apiGatewayUrl(host.toAtlassianHost)}/${host.productType}/${host.cloudId}"
          .addPath(requestUri.path)
          .withParams(requestUri.params)
          .fragment(requestUri.fragment)
      )
  }

  private def isApiGatewayRequest(uri: Uri): Boolean = {
    if (!uri.isAbsolute) {
      return false
    }
    uri.host match {
      case Some(h) if uri.isAbsolute =>
        // Unsure why this check that we copied from the Spring Boot implementation does not
        // include FEDRAMP_SANDBOX_API_GATEWAY_URL_HOST_URL?
        // https://bitbucket.org/atlassian/atlassian-connect-spring-boot/src/763ba5e5a3351e573c9a6d33472988c73391409c/atlassian-connect-spring-boot-core/src/main/java/com/atlassian/connect/spring/internal/request/AtlassianHostUriResolver.java#lines-40
        Seq(PRODUCTION_API_GATEWAY_HOST_URL, STAGING_API_GATEWAY_URL_HOST_URL)
          .contains(h)
      case _ => false
    }
  }

}
