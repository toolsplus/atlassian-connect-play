package io.toolsplus.atlassian.connect.play.api.models

/** Authentication principal for requests coming from an Atlassian host
  * application in which the add-on is installed.
  */
trait AtlassianHostUser {

  /** Host from which the request originated.
    *
    * @return
    *   Host associated with this operation.
    */
  def host: AtlassianHost

  /** Atlassian Account ID of the user on whose behalf a request was made.
    *
    * @return
    *   Atlassian Account ID
    */
  def userAccountId: Option[String]
}

object AtlassianHostUser {

  object Implicits {

    /** Implicitly convert an instance of [[AtlassianHostUser]] to an instance
      * of [[AtlassianHost]].
      *
      * @param hostUser
      *   Atlassian host user instance.
      * @return
      *   Underlying Atlassian host instance.
      */
    implicit def hostUserToHost(implicit
        hostUser: AtlassianHostUser
    ): AtlassianHost =
      hostUser.host

  }

}
