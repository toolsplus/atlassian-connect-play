package io.toolsplus.atlassian.connect.play.auth.frc

/**
  * Authentication credentials representing an unverified Forge Remote invocation context.
  *
  * @param traceId Trace id is 64 or 128-bit in length and indicates the overall ID of the trace
  * @param spanId Span id is 64 or 128-bit in length and indicates the position of the current operation in the trace tree
  * @param forgeInvocationToken Forge Invocation Token (FIT) is the JWT token authenticating the Forge Remote call
  * @param appSystemToken App system token that allows the app to call Atlassian APIs
  * @param appUserToken App user token that allows the app to call Atlassian APIs
  *
  * @see https://developer.atlassian.com/platform/forge/forge-remote-overview/
  */
case class ForgeRemoteCredentials(traceId: String,
                                  spanId: String,
                                  forgeInvocationToken: String,
                                  appSystemToken: Option[String],
                                  appUserToken: Option[String])
