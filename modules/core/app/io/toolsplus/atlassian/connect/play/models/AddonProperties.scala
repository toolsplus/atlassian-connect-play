package io.toolsplus.atlassian.connect.play.models

import javax.inject.{Inject, Singleton}

import io.toolsplus.atlassian.connect.play.api.models.Predefined.AddonKey
import play.api.Configuration

/** Class containing add-on properties such as add-on key, name, base url,
  * etc.
  *
  * All values are read lazily from Play config files, hence they will be
  * cached for the lifetime of the Play app.
  */
@Singleton
class AddonProperties @Inject()(config: Configuration) {

  /** Key of this add-on. */
  lazy val key: AddonKey = addonConfig.get[String]("key")

  /** Name of this add-on. */
  lazy val name: String = addonConfig.get[String]("name")

  /** Base URL of this add-on. */
  lazy val baseUrl: String = addonConfig.get[String]("baseUrl")

  private lazy val addonConfig = config.get[Configuration]("addon")

}
