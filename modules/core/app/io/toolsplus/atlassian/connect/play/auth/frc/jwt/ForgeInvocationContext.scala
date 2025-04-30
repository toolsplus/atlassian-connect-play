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
  * Contains information about the license of the app. This field is only present for paid apps in the production environment.
  * `license` is undefined for free apps, apps in DEVELOPMENT and STAGING environments, and apps that are not listed on
  * the Atlassian Marketplace.
  *
  * @param isActive Specifies if the license is active.
  */
final case class License(isActive: Boolean)

/**
  *
  * @param installationId Identifier for the specific installation of an app. This is the value that any remote storage should be keyed against.
  * @param apiBaseUrl Base URL where all product API requests should be routed
  * @param id Forge application ID matching the value in the Forge manifest.yml
  * @param appVersion Forge application version being invoked
  * @param environment Information about the environment the app is running in
  * @param module Information about the module that initiated this remote call
  * @param license Information about the license associated with the app. This field is only present for paid apps in the production environment.
  */
final case class App(installationId: String,
                     apiBaseUrl: String,
                     id: String,
                     appVersion: String,
                     environment: Environment,
                     module: Module,
                     license: Option[License])

/**
  * Forge invocation context represents the payload included in the Forge Invocation Token (FIT).
  *
  * The FIT payload includes details about the invocation context of a Forge Remote call.
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
