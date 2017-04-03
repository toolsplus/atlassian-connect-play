package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.AtlassianHostUser
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.auth.jwt.{CanonicalPlayHttpRequest, JwtAuthenticationProvider, JwtCredentials}
import io.toolsplus.atlassian.connect.play.models.{AddonProperties, AtlassianConnectProperties}
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import org.scalacheck.Gen.alphaStr
import org.scalacheck.Shrink
import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames
import play.api.mvc.Results.Unauthorized
import play.api.test.FakeRequest
import play.test.Helpers

import scala.concurrent.Future

class JwtAuthenticationActionsSpec
    extends TestSpec
    with GuiceOneAppPerSuite
    with EitherValues {

  val config = app.configuration

  val hostRepository = mock[AtlassianHostRepository]
  val addonProperties = new AddonProperties(config)
  val connectProperties = new AtlassianConnectProperties(config)
  val jwtAuthenticationProvider =
    new JwtAuthenticationProvider(hostRepository, addonProperties)

  val $ =
    new JwtAuthenticationActions(jwtAuthenticationProvider, connectProperties)

  "JwtAuthenticationActions" when {

    "using a JwtExtractor" should {

      "successfully extract token from request header" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(signedJwtStringGen(), playRequestGen) { (rawJwt, request) =>
          val jwtHeader = HeaderNames.AUTHORIZATION -> s"${$.AUTHORIZATION_HEADER_PREFIX} $rawJwt"
          val jwtRequest = request.withHeaders(jwtHeader)
          val jwtCredentials =
            JwtCredentials(rawJwt, CanonicalPlayHttpRequest(jwtRequest))
          $.JwtExtractor.extractJwt(jwtRequest) mustBe Some(jwtCredentials)
        }
      }

      "successfully extract token from request query string" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(signedJwtStringGen()) { rawJwt =>
          val jwtQueryParams = Map("jwt" -> Seq(rawJwt))
          forAll(playRequestGen(jwtQueryParams)) { request =>
            val jwtCredentials =
              JwtCredentials(rawJwt, CanonicalPlayHttpRequest(request))
            $.JwtExtractor.extractJwt(request) mustBe Some(jwtCredentials)
          }
        }
      }

      "return None if request does not contain a token" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen) { request =>
          $.JwtExtractor.extractJwt(request) mustBe None
        }
      }

    }

    "using a JwtAction" should {

      "successfully refine request to JwtRequest" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(signedJwtStringGen(), playRequestGen) { (rawJwt, request) =>
          val jwtHeader = HeaderNames.AUTHORIZATION -> s"${$.AUTHORIZATION_HEADER_PREFIX} $rawJwt"
          val jwtRequest = request.withHeaders(jwtHeader)
          val jwtCredentials =
            JwtCredentials(rawJwt, CanonicalPlayHttpRequest(jwtRequest))
          val result = await {
            $.JwtAction.refine(jwtRequest)
          }
          result mustBe Right($.JwtRequest(jwtCredentials, jwtRequest))
        }
      }

      "fail to refine request if it does not contain a token" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen) { request =>
          val result = await {
            $.JwtAction.refine(request)
          }
          result mustBe Left(Unauthorized("No authentication token found"))
        }
      }

    }

    "using a MaybeJwtAction" should {

      "successfully refine request to MaybeJwtRequest including token" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(signedJwtStringGen(), playRequestGen) { (rawJwt, request) =>
          val jwtHeader = HeaderNames.AUTHORIZATION -> s"${$.AUTHORIZATION_HEADER_PREFIX} $rawJwt"
          val jwtRequest = request.withHeaders(jwtHeader)
          val jwtCredentials =
            JwtCredentials(rawJwt, CanonicalPlayHttpRequest(jwtRequest))
          val result = await {
            $.MaybeJwtAction.refine(jwtRequest)
          }
          result mustBe Right(
            $.MaybeJwtRequest(Some(jwtCredentials), jwtRequest))
        }
      }

      "successfully refine request to MaybeJwtRequest without a token" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen) { request =>
          val result = await {
            $.MaybeJwtAction.refine(request)
          }
          result mustBe Right($.MaybeJwtRequest(None, request))
        }
      }

    }

    "using a AtlassianHostUserAction" should {

      "successfully refine request to AtlassianHostUserRequest" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            forAll(jwtCredentialsGen(host, subject)) { credentials =>
              val jwtRequest = $.JwtRequest(credentials, request)
              val hostUser = AtlassianHostUser(host, Option(subject))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                $.AtlassianHostUserAction.refine(jwtRequest)
              }
              result mustBe Right(
                $.AtlassianHostUserRequest(hostUser, jwtRequest))
            }
        }
      }

      "fail to refine request if authentication fails" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            forAll(jwtCredentialsGen(host, subject)) { credentials =>
              val invalidCredentials =
                credentials.copy(rawJwt = credentials.rawJwt.dropRight(5))
              val jwtRequest = $.JwtRequest(invalidCredentials, request)

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                $.AtlassianHostUserAction.refine(jwtRequest)
              }
              val expectedMessage =
                s"JWT validation failed: ${invalidCredentials.rawJwt}"
              result.left.value mustBe Unauthorized(expectedMessage)
            }
        }
      }

    }

    "using a MaybeAtlassianHostUserAction" should {

      "successfully refine request to MaybeAtlassianHostUserRequest" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            forAll(jwtCredentialsGen(host, subject)) { credentials =>
              val jwtRequest = $.MaybeJwtRequest(Some(credentials), request)
              val hostUser = AtlassianHostUser(host, Option(subject))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                $.MaybeAtlassianHostUserAction.refine(jwtRequest)
              }
              result mustBe Right(
                $.MaybeAtlassianHostUserRequest(Some(hostUser), jwtRequest))
            }
        }
      }

      "successfully refine request for an unknown host if it is an 'uninstalled' request" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(atlassianHostGen, alphaStr) { (host, subject) =>
          forAll(jwtCredentialsGen(host, subject)) { credentials =>
            val jwtHeader = HeaderNames.AUTHORIZATION -> s"${$.AUTHORIZATION_HEADER_PREFIX} ${credentials.rawJwt}"
            val request =
              FakeRequest(Helpers.POST, "/uninstalled").withHeaders(jwtHeader)
            val jwtRequest = $.MaybeJwtRequest(Some(credentials), request)

            (hostRepository
              .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
              .successful(None)

            val result = await {
              $.MaybeAtlassianHostUserAction.refine(jwtRequest)
            }
            result mustBe Right(
              $.MaybeAtlassianHostUserRequest(None, jwtRequest))
          }
        }
      }

      "successfully refine request to MaybeAtlassianHostUserRequest without a token" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen) { request =>
          val jwtRequest = $.MaybeJwtRequest(None, request)

          val result = await {
            $.MaybeAtlassianHostUserAction.refine(jwtRequest)
          }
          result mustBe Right(
            $.MaybeAtlassianHostUserRequest(None, jwtRequest))
        }
      }

      "fail to refine request if authentication fails" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            forAll(jwtCredentialsGen(host, subject)) { credentials =>
              val invalidCredentials =
                credentials.copy(rawJwt = credentials.rawJwt.dropRight(5))
              val jwtRequest =
                $.MaybeJwtRequest(Some(invalidCredentials), request)

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                $.MaybeAtlassianHostUserAction.refine(jwtRequest)
              }
              val expectedMessage =
                s"JWT validation failed: ${invalidCredentials.rawJwt}"
              result.left.value mustBe Unauthorized(expectedMessage)
            }
        }
      }

    }

    "importing Implicits object members" should {

      "implicitly convert a HostUserRequest to a AtlassianHostUser" in {
        forAll(atlassianHostUserGen, playRequestGen, jwtCredentialsGen()) {
          (hostUser, request, credentials) =>
            val jwtRequest = $.JwtRequest(credentials, request)
            val hostUserRequest =
              $.AtlassianHostUserRequest(hostUser, jwtRequest)
            $.Implicits.hostUserRequestToHostUser(hostUserRequest) mustBe hostUser
        }
      }

      "implicitly convert a MaybeHostUserRequest to a Option of AtlassianHostUser" in {
        forAll(atlassianHostUserGen, playRequestGen, jwtCredentialsGen()) {
          (hostUser, request, credentials) =>
            val jwtRequest = $.MaybeJwtRequest(Some(credentials), request)
            val maybeHostUserRequest =
              $.MaybeAtlassianHostUserRequest(Some(hostUser), jwtRequest)
            $.Implicits.maybeHostUserRequestToMaybeHostUser(
              maybeHostUserRequest) mustBe Some(hostUser)
        }
      }

    }

  }

}
