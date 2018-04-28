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
}

object AtlassianHostUser {

  object Implicits {

    import scala.language.implicitConversions

    /**
      * Implicitly convert an instance of [[AtlassianHostUser]] to an
      * instance of [[AtlassianHost]].
      *
      * @param h Atlassian host user instance.
      * @return Underlying Atlassian host instance.
      */
    implicit def hostUserToHost(implicit h: AtlassianHostUser): AtlassianHost =
      h.host

  }

}
