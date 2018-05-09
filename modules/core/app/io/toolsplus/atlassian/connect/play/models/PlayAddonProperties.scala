package io.toolsplus.atlassian.connect.play.models

import io.toolsplus.atlassian.connect.play.api.models.AppProperties
import javax.inject.{Inject, Singleton}
import io.toolsplus.atlassian.connect.play.api.models.Predefined.AppKey
import play.api.Configuration

/** Class containing add-on properties such as add-on key, name, base url,
  * etc.
  *
  * All values are read lazily from Play config files, hence they will be
  * cached for the lifetime of the Play app.
  */
@Singleton
class PlayAddonProperties @Inject()(config: Configuration) extends AppProperties {

  override lazy val key: AppKey = addonConfig.get[String]("key")

  override lazy val name: String = addonConfig.get[String]("name")

  override lazy val baseUrl: String = addonConfig.get[String]("baseUrl")

  private lazy val addonConfig = config.get[Configuration]("addon")

}
