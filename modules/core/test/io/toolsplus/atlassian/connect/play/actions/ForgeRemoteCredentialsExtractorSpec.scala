package io.toolsplus.atlassian.connect.play.actions

import cats.data.Validated.{Invalid, Valid}
import io.toolsplus.atlassian.connect.play.TestSpec
import play.api.http.HeaderNames
import play.api.test.FakeRequest

class ForgeRemoteCredentialsExtractorSpec
    extends TestSpec
    with ForgeRemoteRequestGen {

  "ForgeRemoteCredentialsExtractor" when {

    val fakeJwt = "fake-jwt"
    val fakeTraceId = "fake-trace-id"
    val fakeSpanId = "fake-span-id"

    val forgeInvocationTokenHeader = HeaderNames.AUTHORIZATION -> s"Bearer $fakeJwt"
    val traceHeader = "x-b3-traceid" -> fakeTraceId
    val spanHeader = "x-b3-spanid" -> fakeSpanId

    "trying to extract FRC credentials from request " should {

      "successfully extract credentials from request headers" in {
        forAll(forgeRemoteRequestGen) { request =>
          ForgeRemoteCredentialsExtractor.extract(request) mustBe a[Valid[_]]
        }
      }

      "fail if span invocation token is missing" in {
        val request = FakeRequest().withHeaders(
          spanHeader,
          traceHeader,
        )
        val result = ForgeRemoteCredentialsExtractor.extract(request)
        result mustBe a[Invalid[_]]
        result match {
          case Invalid(errors) => errors.length mustBe 1
          case _               => fail("Expect result to be of type Invalid")
        }
      }

      "fail if trace header is missing" in {
        val request = FakeRequest().withHeaders(
          forgeInvocationTokenHeader,
          spanHeader,
        )
        val result = ForgeRemoteCredentialsExtractor.extract(request)
        result mustBe a[Invalid[_]]
        result match {
          case Invalid(errors) => errors.length mustBe 1
          case _               => fail("Expect result to be of type Invalid")
        }
      }

      "fail if span header is missing" in {
        val request = FakeRequest().withHeaders(
          forgeInvocationTokenHeader,
          traceHeader,
        )
        val result = ForgeRemoteCredentialsExtractor.extract(request)
        result mustBe a[Invalid[_]]
        result match {
          case Invalid(errors) => errors.length mustBe 1
          case _               => fail("Expect result to be of type Invalid")
        }
      }

      "accumulate errors for missing headers" in {
        val result = ForgeRemoteCredentialsExtractor.extract(FakeRequest())
        result mustBe a[Invalid[_]]
        result match {
          case Invalid(errors) => errors.length mustBe 3
          case _               => fail("Expect result to be of type Invalid")
        }
      }

      "fail if invocation token header has wrong prefix" in {
        val request = FakeRequest().withHeaders(
          HeaderNames.AUTHORIZATION -> s"zBearer $fakeJwt",
          traceHeader,
          spanHeader
        )
        val result = ForgeRemoteCredentialsExtractor.extract(request)
        result mustBe a[Invalid[_]]
        result match {
          case Invalid(errors) => errors.length mustBe 1
          case _               => fail("Expect result to be of type Invalid")
        }
      }

    }
  }

}
