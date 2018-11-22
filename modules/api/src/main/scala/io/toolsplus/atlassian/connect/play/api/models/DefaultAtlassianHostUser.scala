package io.toolsplus.atlassian.connect.play.api.models

/** Default case class implementation of [[AtlassianHostUser]]
  *
  * @param host    Atlassian host associated with this operation.
  * @param userKey Key of user associated with this operation.
  */
case class DefaultAtlassianHostUser(host: AtlassianHost,
                                    userKey: Option[String],
                                    userAccountId: Option[String])
    extends AtlassianHostUser {

  /** Configures this host user to act as of this host user.
    *
    * @param userKey Key of the user to act as
    * @return Same host user with user key set to given host user.
    */
  override def actAs(userKey: String): AtlassianHostUser =
    this.copy(userKey = Option(userKey))
}
