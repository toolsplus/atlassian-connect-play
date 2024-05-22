package io.toolsplus.atlassian.connect.play.actions

import org.scalacheck.Gen
import play.api.http.HeaderNames
import play.api.test.FakeRequest

trait ForgeRemoteRequestGen {

  def forgeRemoteRequestGen: Gen[FakeRequest[_]] =
    for {
      jwt <- Gen.alphaStr
      traceId <- Gen.alphaStr
      spanId <- Gen.alphaStr
      systemToken <- Gen.option(Gen.alphaStr)
      userToken <- Gen.option(Gen.alphaStr)
      tokensHeaders = Map("x-forge-oauth-system" -> systemToken,
        "x-forge-oauth-user" -> userToken).collect {
        case (key, Some(value)) => (key, value)
      }
    } yield
      FakeRequest().withHeaders(
        (Map(HeaderNames.AUTHORIZATION -> s"Bearer $jwt",
          "x-b3-traceid" -> traceId,
          "x-b3-spanid" -> spanId) ++ tokensHeaders).toSeq: _*)


}
