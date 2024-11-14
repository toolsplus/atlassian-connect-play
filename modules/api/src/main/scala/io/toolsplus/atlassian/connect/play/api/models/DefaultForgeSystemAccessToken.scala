package io.toolsplus.atlassian.connect.play.api.models
import java.time.Instant

/**
  * Default case class implementation of [[ForgeSystemAccessToken]]
  */
case class DefaultForgeSystemAccessToken(installationId: String,
                                         apiBaseUrl: String,
                                         accessToken: String,
                                         expirationTime: Instant)
    extends ForgeSystemAccessToken
