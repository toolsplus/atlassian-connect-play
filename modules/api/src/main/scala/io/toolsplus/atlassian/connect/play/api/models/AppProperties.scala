package io.toolsplus.atlassian.connect.play.api.models

import io.toolsplus.atlassian.connect.play.api.models.Predefined.AppKey

/** App properties available in all apps.
  */
trait AppProperties {

  /** Key of this app. */
  def key: AppKey

  /** Secret of this app. */
  def secret: String

  /** Name of this app. */
  def name: String

  /** Base URL of this app. */
  def baseUrl: String

}
