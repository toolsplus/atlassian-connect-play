package io.toolsplus.atlassian.connect.play.actions

import cats.data.ValidatedNec
import cats.implicits._
import io.toolsplus.atlassian.connect.play.auth.frc.ForgeRemoteCredentials
import play.api.http.HeaderNames
import play.api.mvc.Request

object ForgeRemoteCredentialsExtractor {

  /**
    * Attempts to extract Forge Remote credentials from the given request. .
    *
    * @param request Request from which to extract credentials
    * @return Either unverified Forge Remote credentials or a list of errors
    */
  def extract[A](
      request: Request[A]): ValidatedNec[Exception, ForgeRemoteCredentials] = {
    (validateTraceId(request),
     validateSpanId(request),
     validateForgeInvocationToken(request)).mapN(
      (traceId, spanId, forgeInvocationToken) =>
        ForgeRemoteCredentials(traceId,
                               spanId,
                               forgeInvocationToken,
                               request.headers
                                 .get("x-forge-oauth-system"),
                               request.headers
                                 .get("x-forge-oauth-user")))
  }

  private def validateTraceId[A](
      request: Request[A]): ValidatedNec[Exception, String] =
    request.headers
      .get("x-b3-traceid") match {
      case Some(traceId) => traceId.validNec
      case None =>
        new Exception(s"Missing 'x-b3-traceid' header").invalidNec
    }

  private def validateSpanId[A](
      request: Request[A]): ValidatedNec[Exception, String] =
    request.headers
      .get("x-b3-spanid") match {
      case Some(spanId) => spanId.validNec
      case None =>
        new Exception(s"Missing 'x-b3-spanid' header").invalidNec
    }

  private def validateForgeInvocationToken[A](
      request: Request[A]): ValidatedNec[Exception, String] =
    request.headers
      .get(HeaderNames.AUTHORIZATION) match {
      case Some(fitHeader) =>
        val authorizationHeaderPrefix = "bearer "
        fitHeader match {
          case header
              if header.toLowerCase.startsWith(authorizationHeaderPrefix) =>
            header.substring(authorizationHeaderPrefix.length).trim.validNec
          case _ =>
            new Exception(s"Invalid Forge Invocation Token header").invalidNec
        }
      case None =>
        new Exception(s"Missing Forge Invocation Token header").invalidNec
    }
}
