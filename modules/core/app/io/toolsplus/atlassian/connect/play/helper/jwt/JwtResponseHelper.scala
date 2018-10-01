package io.toolsplus.atlassian.connect.play.helper.jwt

import io.toolsplus.atlassian.connect.play.api.models.AtlassianHostUser
import io.toolsplus.atlassian.connect.play.auth.jwt.SelfAuthenticationTokenGenerator
import io.toolsplus.atlassian.jwt.JwtSigningError
import play.api.mvc.Result
import play.api.mvc.Results._

/** Response helper to sign results with updated JWT header.
  *
  * Requires a [[SelfAuthenticationTokenGenerator]] in context.
  */
@deprecated(
  "SelfAuthenticationTokens are deprecated. Use AP.context.getToken() to generate tokens and sign requests from client side code.",
  "0.1.9")
trait JwtResponseHelper {

  def selfAuthenticationTokenGenerator: SelfAuthenticationTokenGenerator
  def jwtResponseHeaderName: String = "Set-Authorization"

  def selfAuthenticationErrorResult =
    InternalServerError("Failed to generate self-authenticated JWT")

  /** Signs the given result using a JWT.
    *
    * Sings the result by adding an [[jwtResponseHeaderName]] header of the format
    * 'JWT $token'.
    *
    * @param result Result to sign
    * @param hostUser Atlassian host user associated with this operation
    * @return Either the signed result or a JWT signing error
    */
  def withJwtResponseHeader(result: Result)(
      implicit hostUser: AtlassianHostUser): Either[JwtSigningError, Result] = {
    selfAuthenticationTokenGenerator
      .createSelfAuthenticationToken(hostUser)
      .map { token => result.withHeaders(jwtResponseHeaderName -> s"JWT $token")
      }
  }

}
