package io.toolsplus.atlassian.connect.play.api.models

/** Default case class implementation of [[AtlassianHostUser]]
  *
  * @param host    Atlassian host associated with this operation.
  * @param userAccountId Atlassian user account id associated with this operation.
  */
case class DefaultAtlassianHostUser(host: AtlassianHost,
                                    userAccountId: Option[String])
    extends AtlassianHostUser
