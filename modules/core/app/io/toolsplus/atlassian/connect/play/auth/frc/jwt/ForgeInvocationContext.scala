package io.toolsplus.atlassian.connect.play.auth.frc.jwt

import com.nimbusds.jose.proc.SecurityContext
import io.circe.JsonObject

/**
  * Environment provides information about the Forge environment the app is running in
  * @param `type` Forge environment type associated with a Forge Remote call, such as DEVELOPMENT, STAGING, PRODUCTION
  * @param id Forge environment id associated with a Forge Remote call
  */
final case class Environment(`type`: String, id: String)

/**
  * Module provides information about the module that initiated a Forge remote call.
  *
  * @param `type` Module type initiating the remote call, such as `xen:macro` for front-end invocations. Otherwise, it will be core:endpoint. To determine the type of module that specified the remote resolver, refer to `context.extension.type`
  * @param key Forge module key for this endpoint as specified in the manifest.yml
  */
final case class Module(`type`: String, key: String)

/**
  *
  * @param installationId Identifier for the specific installation of an app. This is the value that any remote storage should be keyed against.
  * @param apiBaseUrl Base URL where all product API requests should be routed
  * @param id Forge application ID matching the value in the Forge manifest.yml
  * @param version Forge application version being invoked
  * @param environment Information about the environment the app is running in
  * @param module Information about the module that initiated this remote call
  */
final case class App(installationId: String,
                     apiBaseUrl: String,
                     id: String,
                     version: Int,
                     environment: Environment,
                     module: Module)

/**
  * Forge invocation context represents the payload included in the Forge Invocation Token (FIT).
  *
  * The FIT payload includes details about the the invocation context of a Forge Remote call.
  *
  * @param app Details about the app and installation context
  * @param context Context depending on how the app is using Forge Remote
  * @param principal ID of the user who invoked the app. UI modules only
  *
  * @see https://developer.atlassian.com/platform/forge/forge-remote-overview/#the-forge-invocation-token--fit-
  */
final case class ForgeInvocationContext(app: App,
                                        context: Option[JsonObject],
                                        principal: Option[String])
    extends SecurityContext
