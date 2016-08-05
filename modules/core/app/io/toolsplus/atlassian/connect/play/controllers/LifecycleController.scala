package io.toolsplus.atlassian.connect.play.controllers

import com.google.inject.Inject
import io.circe.generic.auto._
import io.toolsplus.atlassian.connect.play.actions.JwtAuthenticationActions
import io.toolsplus.atlassian.connect.play.models.{AddonProperties, GenericEvent, InstalledEvent}
import io.toolsplus.atlassian.connect.play.services._
import play.api.libs.circe.Circe
import play.api.mvc.Controller

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Controller that handles the add-on install and uninstall lifecycle
  * callbacks.
  */
class LifecycleController @Inject()(
    lifecycleService: LifecycleService,
    jwtAuthenticationActions: JwtAuthenticationActions,
    addonProperties: AddonProperties)
    extends Controller
    with Circe {

  import jwtAuthenticationActions.Implicits._

  def installed = {
    jwtAuthenticationActions.withOptionalAtlassianHostUser.async(
      circe.json[InstalledEvent]) { implicit request =>
      lifecycleService.installed(request.body).value map {
        case Right(_) => Ok
        case Left(e) =>
          e match {
            case MissingAtlassianHostError => BadRequest
            case InvalidLifecycleEventTypeError => BadRequest
            case HostForbiddenError => Forbidden
            case MissingJwtError =>
              Unauthorized.withHeaders(
                WWW_AUTHENTICATE -> s"""JWT realm="${addonProperties.key}"""")
          }
      }
    }
  }

  def uninstalled =
    jwtAuthenticationActions.withAtlassianHostUser.async(
      circe.json[GenericEvent]) { implicit request =>
      lifecycleService.uninstalled(request.body).value map {
        case Right(_) => NoContent
        case Left(e) =>
          e match {
            case MissingAtlassianHostError => NoContent
            case InvalidLifecycleEventTypeError => BadRequest
            case HostForbiddenError => Forbidden
            case MissingJwtError =>
              Unauthorized.withHeaders(
                WWW_AUTHENTICATE -> s"""JWT realm="${addonProperties.key}"""")
          }
      }
    }

}
