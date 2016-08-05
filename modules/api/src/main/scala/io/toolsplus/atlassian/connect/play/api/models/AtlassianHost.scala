package io.toolsplus.atlassian.connect.play.api.models

import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository

/** An Atlassian host in which the add-on is or has been installed. Hosts are
  * stored in [[AtlassianHostRepository]].
  *
  * During processing of a request from an Atlassian host, the details of the
  * host and of the user at the browser can be obtained from the [[AtlassianHostUser]].
  *
  * @param clientKey                Identifying key for the Atlassian product
  *                                 instance that the add-on was installed into.
  * @param key                      Add-on key that was installed into the
  *                                 Atlassian Product, as it appears in your
  *                                 add-on's descriptor.
  * @param publicKey                Public key for this Atlassian product
  *                                 instance.
  * @param oauthClientId            OAuth 2.0 client ID for the add-on used
  *                                 for OAuth 2.0 - JWT Bearer token
  *                                 authorization grant type.
  * @param sharedSecret             Secret to sign outgoing JWT tokens and
  *                                 validate incoming JWT tokens. Only sent
  *                                 on the installed event.
  * @param serverVersion            Host product's version.
  * @param pluginsVersion           Semver compliant version of Atlassian
  *                                 Connect which is running on the host server.
  * @param baseUrl                  URL prefix for this Atlassian product
  *                                 instance.
  * @param productType              Identifies the category of Atlassian
  *                                 product, e.g. jira or confluence.
  * @param description              Host product description.
  * @param serviceEntitlementNumber Service entitlement number (SEN) is the
  *                                 add-on license id. Only included during
  *                                 installation of a paid add-on.
  * @param installed                Indicates if the add-on is currently
  *                                 installed on the host. Upon uninstallation,
  *                                 the value of this flag will be set to false.
  */
case class AtlassianHost(clientKey: ClientKey,
                         key: String,
                         publicKey: String,
                         oauthClientId: Option[String],
                         sharedSecret: String,
                         serverVersion: String,
                         pluginsVersion: String,
                         baseUrl: String,
                         productType: String,
                         description: String,
                         serviceEntitlementNumber: Option[String],
                         installed: Boolean)
