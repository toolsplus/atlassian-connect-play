package io.toolsplus.atlassian.connect.play.api.events

import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost

/**
  * The base event.
  */
trait AppEvent

/**
  * An event which will be published after a new host has been installed.
  *
  * @param host The newly installed Atlassian host.
  */
case class AppInstalledEvent(host: AtlassianHost) extends AppEvent

/**
  * An event which will be published after a host has uninstalled.
  *
  * @param host The uninstalled Atlassian host.
  */
case class AppUninstalledEvent(host: AtlassianHost) extends AppEvent