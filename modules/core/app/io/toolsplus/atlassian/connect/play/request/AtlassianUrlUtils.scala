package io.toolsplus.atlassian.connect.play.request

import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost

import java.net.URI

object AtlassianUrlUtils {

  val PRODUCTION_API_GATEWAY_HOST_URL = "api.atlassian.com"
  val STAGING_API_GATEWAY_URL_HOST_URL = "api.stg.atlassian.com"
  val FEDRAMP_SANDBOX_API_GATEWAY_URL_HOST_URL = "api.atlassian-fex.com"

  val PRODUCTION_API_GATEWAY_URL_PREFIX =
    s"https://$PRODUCTION_API_GATEWAY_HOST_URL/ex"
  val STAGING_API_GATEWAY_URL_PREFIX =
    s"https://$STAGING_API_GATEWAY_URL_HOST_URL/ex"
  val FEDRAMP_SANDBOX_API_GATEWAY_URL_PREFIX =
    s"https://$FEDRAMP_SANDBOX_API_GATEWAY_URL_HOST_URL/ex"

  val STAGING_HOST_POSTFIX = ".jira-dev.com"
  val FEDRAMP_SANDBOX_HOST_POSTFIX = ".atlassian-fex.net"

  val PRODUCTION_AUTHORIZATION_SERVER_TOKEN_URL: URI =
    URI.create("https://auth.atlassian.com/oauth/token")
  val STAGING_AUTHORIZATION_SERVER_TOKEN_URL: URI =
    URI.create("https://auth.stg.atlassian.com/oauth/token")
  val FEDRAMP_SANDBOX_AUTHORIZATION_SERVER_TOKEN_URL: URI =
    URI.create("https://auth.atlassian-fex.com/oauth/token")

  def isStagingHost(host: AtlassianHost): Boolean =
    URI.create(host.baseUrl).getHost.endsWith(STAGING_HOST_POSTFIX)

  def isFedRAMPSandboxHost(host: AtlassianHost): Boolean =
    URI.create(host.baseUrl).getHost.endsWith(FEDRAMP_SANDBOX_HOST_POSTFIX)

  def authServerTokenUrl(host: AtlassianHost): URI = {
    case h if isFedRAMPSandboxHost(h) =>
      FEDRAMP_SANDBOX_AUTHORIZATION_SERVER_TOKEN_URL
    case h if isStagingHost(h) => STAGING_AUTHORIZATION_SERVER_TOKEN_URL
    case _                     => PRODUCTION_AUTHORIZATION_SERVER_TOKEN_URL
  }

  def apiGatewayUrl(host: AtlassianHost): String = host match {
    case h if isFedRAMPSandboxHost(h) => FEDRAMP_SANDBOX_API_GATEWAY_URL_PREFIX
    case h if isStagingHost(h)        => STAGING_API_GATEWAY_URL_PREFIX
    case _                            => PRODUCTION_API_GATEWAY_URL_PREFIX
  }

}
