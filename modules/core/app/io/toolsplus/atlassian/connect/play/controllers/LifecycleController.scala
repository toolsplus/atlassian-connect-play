package io.toolsplus.atlassian.connect.play.controllers

import com.google.inject.Inject
import io.circe.generic.auto._
import io.toolsplus.atlassian.connect.play.actions.AtlassianHostUserAction
import io.toolsplus.atlassian.connect.play.api.models.AppProperties
import io.toolsplus.atlassian.connect.play.auth.jwt.CanonicalHttpRequestQshProvider
import io.toolsplus.atlassian.connect.play.auth.jwt.asymmetric.AsymmetricJwtAuthenticationProvider
import io.toolsplus.atlassian.connect.play.models.{GenericEvent, InstalledEvent}
import io.toolsplus.atlassian.connect.play.services._
import play.api.libs.circe.Circe
import play.api.mvc.{Action, InjectedController}

import scala.concurrent.ExecutionContext

/**
  * Controller that handles the app install and uninstall lifecycle
  * callbacks.
  */
class LifecycleController @Inject()(
    lifecycleService: LifecycleService,
    atlassianHostUserAction: AtlassianHostUserAction,
    asymmetricJwtAuthenticationProvider: AsymmetricJwtAuthenticationProvider,
    appProperties: AppProperties,
    implicit val executionContext: ExecutionContext)
    extends InjectedController
    with Circe {

  import atlassianHostUserAction.Implicits._

  def installed: Action[InstalledEvent] = {
    atlassianHostUserAction
      .authenticateWith(asymmetricJwtAuthenticationProvider,
             CanonicalHttpRequestQshProvider)
      .async(circe.json[InstalledEvent]) { implicit request =>
        lifecycleService.installed(request.body).value map {
          case Right(_) => Ok
          case Left(e) =>
            e match {
              case MissingAtlassianHostError      => BadRequest
              case InvalidLifecycleEventTypeError => BadRequest
              case HostForbiddenError             => Forbidden
              case MissingJwtError =>
                Unauthorized.withHeaders(
                  WWW_AUTHENTICATE -> s"""JWT realm="${appProperties.key}"""")
            }
        }
      }
  }

  def uninstalled: Action[GenericEvent] =
    atlassianHostUserAction
      .authenticateWith(asymmetricJwtAuthenticationProvider, CanonicalHttpRequestQshProvider)
      .async(circe.json[GenericEvent]) { implicit request =>
        lifecycleService.uninstalled(request.body).value map {
          case Right(_) => NoContent
          case Left(e) =>
            e match {
              case MissingAtlassianHostError      => NoContent
              case InvalidLifecycleEventTypeError => BadRequest
              case HostForbiddenError             => Forbidden
              case MissingJwtError =>
                Unauthorized.withHeaders(
                  WWW_AUTHENTICATE -> s"""JWT realm="${appProperties.key}"""")
            }
        }
      }

}
