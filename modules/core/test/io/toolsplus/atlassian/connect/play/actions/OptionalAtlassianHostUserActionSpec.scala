package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.DefaultAtlassianHostUser
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.auth.jwt._
import io.toolsplus.atlassian.connect.play.models.AtlassianConnectProperties
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import org.scalacheck.Gen.alphaStr
import org.scalacheck.Shrink
import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames
import play.api.mvc.BodyParsers
import play.api.mvc.Results.Unauthorized
import play.api.test.FakeRequest
import play.test.Helpers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OptionalAtlassianHostUserActionSpec
    extends TestSpec
    with GuiceOneAppPerSuite
    with EitherValues {

  val config = app.configuration

  val parser = app.injector.instanceOf[BodyParsers.Default]

  val hostRepository = mock[AtlassianHostRepository]
  val connectProperties = new AtlassianConnectProperties(config)
  val jwtAuthenticationProvider =
    new JwtAuthenticationProvider(hostRepository)

  val maybeJwtActionTransformer = new MaybeJwtActionTransformer()
  val maybeAtlassianHostUserActionRefinerFactory =
    new MaybeAtlassianHostUserActionRefinerFactory(jwtAuthenticationProvider,
                                                   connectProperties)

  val optionalAtlassianHostUserAction =
    new OptionalAtlassianHostUserAction(
      parser,
      maybeJwtActionTransformer,
      maybeAtlassianHostUserActionRefinerFactory)

  "MaybeJwtActionRefiner" when {

    "refining a standard Request" should {

      "successfully refine request to MaybeJwtRequest including token" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(signedJwtStringGen(), playRequestGen) { (rawJwt, request) =>
          val jwtHeader = HeaderNames.AUTHORIZATION -> s"${JwtExtractor.AuthorizationHeaderPrefix} $rawJwt"
          val jwtRequest = request.withHeaders(jwtHeader)
          val jwtCredentials =
            JwtCredentials(rawJwt, CanonicalPlayHttpRequest(jwtRequest))
          val result = await {
            maybeJwtActionTransformer.refine(jwtRequest)
          }
          result mustBe Right(MaybeJwtRequest(Some(jwtCredentials), jwtRequest))
        }
      }

      "successfully refine request to MaybeJwtRequest without a token" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen) { request =>
          val result = await {
            maybeJwtActionTransformer.refine(request)
          }
          result mustBe Right(MaybeJwtRequest(None, request))
        }
      }

    }

  }

  "MaybeAtlassianHostUserActionRefiner" when {

    "refining a MaybeJwtRequest" should {

      "successfully refine request with context QSH to MaybeAtlassianHostUserRequest" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            forAll(jwtCredentialsGen(host, subject)) { credentials =>
              val jwtRequest = MaybeJwtRequest(Some(credentials), request)
              val hostUser =
                DefaultAtlassianHostUser(host, None, Option(subject))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                maybeAtlassianHostUserActionRefinerFactory
                  .withQshFrom(ContextQshProvider)
                  .refine(jwtRequest)
              }
              result mustBe Right(
                MaybeAtlassianHostUserRequest(Some(hostUser), jwtRequest))
            }
        }
      }

      "successfully refine request with HTTP request QSH to MaybeAtlassianHostUserRequest" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            forAll(jwtCredentialsGen(host, subject)) { credentials =>
              val jwtRequest = MaybeJwtRequest(Some(credentials), request)
              val hostUser =
                DefaultAtlassianHostUser(host, None, Option(subject))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                maybeAtlassianHostUserActionRefinerFactory
                  .withQshFrom(CanonicalHttpRequestQshProvider)
                  .refine(jwtRequest)
              }
              result mustBe Right(
                MaybeAtlassianHostUserRequest(Some(hostUser), jwtRequest))
            }
        }
      }

      "successfully refine request for an unknown host if it is an 'uninstalled' request" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(atlassianHostGen, alphaStr) { (host, subject) =>
          forAll(jwtCredentialsGen(host, subject)) { credentials =>
            val jwtHeader = HeaderNames.AUTHORIZATION -> s"${JwtExtractor.AuthorizationHeaderPrefix} ${credentials.rawJwt}"
            val request =
              FakeRequest(Helpers.POST, "/uninstalled").withHeaders(jwtHeader)
            val jwtRequest = MaybeJwtRequest(Some(credentials), request)

            (hostRepository
              .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
              .successful(None)

            val result = await {
              maybeAtlassianHostUserActionRefinerFactory
                .withQshFrom(CanonicalHttpRequestQshProvider)
                .refine(jwtRequest)
            }
            result mustBe Right(MaybeAtlassianHostUserRequest(None, jwtRequest))
          }
        }
      }

      "successfully refine request to MaybeAtlassianHostUserRequest without a token" in {
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(playRequestGen) { request =>
          val jwtRequest = MaybeJwtRequest(None, request)

          val result = await {
            maybeAtlassianHostUserActionRefinerFactory
              .withQshFrom(CanonicalHttpRequestQshProvider)
              .refine(jwtRequest)
          }
          result mustBe Right(MaybeAtlassianHostUserRequest(None, jwtRequest))
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
                MaybeJwtRequest(Some(invalidCredentials), request)

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                maybeAtlassianHostUserActionRefinerFactory
                  .withQshFrom(CanonicalHttpRequestQshProvider)
                  .refine(jwtRequest)
              }
              val expectedMessage =
                s"JWT validation failed: ${invalidCredentials.rawJwt}"
              result.left.value mustBe Unauthorized(expectedMessage)
            }
        }
      }

    }

  }

  "OptionalAtlassianHostUserAction" when {

    "importing Implicits object members" should {

      "implicitly convert a MaybeHostUserRequest to a Option of AtlassianHostUser" in {
        forAll(atlassianHostUserGen, playRequestGen, jwtCredentialsGen()) {
          (hostUser, request, credentials) =>
            val jwtRequest = MaybeJwtRequest(Some(credentials), request)
            val maybeHostUserRequest =
              MaybeAtlassianHostUserRequest(Some(hostUser), jwtRequest)
            optionalAtlassianHostUserAction.Implicits
              .maybeHostUserRequestToMaybeHostUser(maybeHostUserRequest) mustBe Some(
              hostUser)
        }
      }

    }

  }

}
