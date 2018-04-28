package io.toolsplus.atlassian.connect.play.api.models

/** Standard case class implementation of [[AtlassianHostUser]]
  *
  * @param host    Atlassian host associated with this operation.
  * @param userKey Key of user associated with this operation.
  */
case class StandardAtlassianHostUser(host: AtlassianHost, userKey: Option[String]) extends AtlassianHostUser
