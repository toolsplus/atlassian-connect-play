package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.api.models.AtlassianHostUser
import play.api.mvc.{Request, WrappedRequest}

abstract class AtlassianHostUserRequest[A](request: Request[A])
    extends WrappedRequest[A](request) {
  def hostUser: AtlassianHostUser
}
