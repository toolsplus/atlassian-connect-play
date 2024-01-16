package io.toolsplus.atlassian.connect.play.auth.frc

import io.toolsplus.atlassian.connect.play.auth.frc.jwt.ForgeInvocationContext

/**
  * Verified Forge Remote context including the invocation context and the credentials associated with the call.
  */
case class ForgeRemoteContext(invocationContext: ForgeInvocationContext,
                              credentials: ForgeRemoteCredentials)
