package io.toolsplus.atlassian.connect.play.api.models

/**
  * Authentication principal for requests coming from an Atlassian host
  * application in which the add-on is installed.
  */
trait AtlassianHostUser {

  /** Host from which the request originated.
    *
    * @return Host associated with this operation.
    */
  def host: AtlassianHost

  /** Key of the user on whose behalf a request was made.
    *
    * @return Key of the user associated with this request.
    */
  def userKey: Option[String]

  /** Configures this host user to act as of this host user.
    *
    * @param userKey Key of the user to act as
    * @return Same host user with user key set to given host user.
    */
  def actAs(userKey: String): AtlassianHostUser
}

object AtlassianHostUser {

  object Implicits {

    import scala.language.implicitConversions

    /**
      * Implicitly convert an instance of [[AtlassianHostUser]] to an
      * instance of [[AtlassianHost]].
      *
      * @param hostUser Atlassian host user instance.
      * @return Underlying Atlassian host instance.
      */
    implicit def hostUserToHost(implicit hostUser: AtlassianHostUser): AtlassianHost =
      hostUser.host

  }

}
