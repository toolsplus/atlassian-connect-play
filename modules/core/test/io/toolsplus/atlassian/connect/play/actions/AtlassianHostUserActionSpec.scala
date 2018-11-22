package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.DefaultAtlassianHostUser
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.auth.jwt.{
  CanonicalPlayHttpRequest,
  JwtAuthenticationProvider,
  JwtCredentials
}
import io.toolsplus.atlassian.connect.play.models.PlayAddonProperties
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import org.scalacheck.Gen.alphaStr
import org.scalacheck.Shrink
import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames
import play.api.mvc.BodyParsers
import play.api.mvc.Results.Unauthorized

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AtlassianHostUserActionSpec
    extends TestSpec
    with GuiceOneAppPerSuite
    with EitherValues {

  val config = app.configuration

  val parser = app.injector.instanceOf[BodyParsers.Default]

  val hostRepository = mock[AtlassianHostRepository]
  val addonProperties = new PlayAddonProperties(config)
  val jwtAuthenticationProvider =
    new JwtAuthenticationProvider(hostRepository, addonProperties)

  val jwtActionRefiner = new JwtActionRefiner()
  val atlassianHostUserActionRefiner = new AtlassianHostUserActionRefiner(
    jwtAuthenticationProvider)

  val atlassianHostUserAction =
    new AtlassianHostUserAction(parser,
                                jwtActionRefiner,
                                atlassianHostUserActionRefiner)

  "JwtActionRefiner" when {

    "refining a standard Request" should {

      "successfully refine request to JwtRequest" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(signedJwtStringGen(), playRequestGen) { (rawJwt, request) =>
          val jwtHeader = HeaderNames.AUTHORIZATION -> s"${JwtExtractor.AuthorizationHeaderPrefix} $rawJwt"
          val jwtRequest = request.withHeaders(jwtHeader)
          val jwtCredentials =
            JwtCredentials(rawJwt, CanonicalPlayHttpRequest(jwtRequest))
          val result = await {
            jwtActionRefiner.refine(jwtRequest)
          }
          result mustBe Right(JwtRequest(jwtCredentials, jwtRequest))
        }
      }

      "fail to refine request if it does not contain a token" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen) { request =>
          val result = await {
            jwtActionRefiner.refine(request)
          }
          result mustBe Left(Unauthorized("No authentication token found"))
        }
      }

    }

  }

  "AtlassianHostUserActionRefiner" when {

    "refining a JwtRequest" should {

      "successfully refine JwtRequest to AtlassianHostUserRequest" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            forAll(jwtCredentialsGen(host, subject)) { credentials =>
              val jwtRequest = JwtRequest(credentials, request)
              val hostUser =
                DefaultAtlassianHostUser(host, None, Option(subject))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                atlassianHostUserActionRefiner.refine(jwtRequest)
              }
              result mustBe Right(
                AtlassianHostUserRequest(hostUser, jwtRequest))
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
              val jwtRequest = JwtRequest(invalidCredentials, request)

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                atlassianHostUserActionRefiner.refine(jwtRequest)
              }
              val expectedMessage =
                s"JWT validation failed: ${invalidCredentials.rawJwt}"
              result.left.value mustBe Unauthorized(expectedMessage)
            }
        }
      }

    }

  }

  "AtlassianHostUserAction" when {

    "importing Implicits object members" should {

      "implicitly convert a HostUserRequest to a AtlassianHostUser" in {
        forAll(atlassianHostUserGen, playRequestGen, jwtCredentialsGen()) {
          (hostUser, request, credentials) =>
            val jwtRequest = JwtRequest(credentials, request)
            val hostUserRequest =
              AtlassianHostUserRequest(hostUser, jwtRequest)
            atlassianHostUserAction.Implicits.hostUserRequestToHostUser(
              hostUserRequest) mustBe hostUser
        }
      }

    }

  }

}
