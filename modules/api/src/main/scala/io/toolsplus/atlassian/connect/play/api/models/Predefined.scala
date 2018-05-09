package io.toolsplus.atlassian.connect.play.api.models

object Predefined {

  /** Type identifying key for the Atlassian product instance. */
  type ClientKey = String

  /** Type definition representing an Atlassian Connect add-on key.
    * It's just a String type but makes passing the plugin key implicitly possible.
    */
  type AppKey = String

}
