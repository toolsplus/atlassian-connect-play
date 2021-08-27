package io.toolsplus.atlassian.connect.play.ws.jwt

import java.net.URI
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost
import io.toolsplus.atlassian.connect.play.auth.jwt.symmetric.JwtGenerator
import play.api.http.HeaderNames.{AUTHORIZATION, USER_AGENT}
import play.api.libs.ws.WSSignatureCalculator
import play.shaded.ahc.org.asynchttpclient.{Request, RequestBuilderBase, SignatureCalculator}

class JwtSignatureCalculator(host: AtlassianHost, jwtGenerator: JwtGenerator)
    extends WSSignatureCalculator
    with SignatureCalculator {

  override def calculateAndAddSignature(
      request: Request,
      requestBuilder: RequestBuilderBase[_]): Unit = {
    generateJwt(request, host).map { jwt =>
      request.getHeaders
        .set(USER_AGENT, JwtSignatureCalculator.userAgent)
        .set(AUTHORIZATION, s"JWT $jwt")
    }
  }

  private def generateJwt(request: Request, host: AtlassianHost) = {
    jwtGenerator.createJwtToken(request.getMethod,
                                URI.create(request.getUrl),
                                host)
  }
}

object JwtSignatureCalculator {

  val userAgent = "atlassian-connect-play"

  def apply(jwtGenerator: JwtGenerator)(
      implicit host: AtlassianHost): JwtSignatureCalculator = {
    new JwtSignatureCalculator(host, jwtGenerator)
  }
}
