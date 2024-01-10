package io.toolsplus.atlassian.connect.play.request.sttp.oauth2.model

import sttp.client3.SttpBackend
import sttp.model.Uri

trait AccessTokenProvider {

  /**
    * Request new token with given scope from OAuth2 provider.
    */
  def requestToken[F[_]](tokenUrl: Uri,
                         clientId: String,
                         clientSecret: String,
                         scope: Option[String] = None)(
      backend: SttpBackend[F, Any]
  ): F[AccessTokenResponse]
}
