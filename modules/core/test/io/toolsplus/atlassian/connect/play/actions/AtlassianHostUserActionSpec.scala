package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.DefaultAtlassianHostUser
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.auth.jwt.{CanonicalHttpRequestQshProvider, CanonicalPlayHttpRequest, ContextQshProvider, JwtAuthenticationProvider, JwtCredentials}
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import org.scalacheck.Gen.alphaStr
import org.scalacheck.Shrink
import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.BodyParsers
import play.api.mvc.Results.Unauthorized
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AtlassianHostUserActionSpec
    extends TestSpec
    with GuiceOneAppPerSuite
    with EitherValues {

  val config: Configuration = app.configuration

  val parser: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]

  val hostRepository: AtlassianHostRepository = mock[AtlassianHostRepository]
  val jwtAuthenticationProvider =
    new JwtAuthenticationProvider(hostRepository)

  val jwtActionRefiner = new JwtActionRefiner()
  val atlassianHostUserActionRefinerFactory =
    new AtlassianHostUserActionRefinerFactory(jwtAuthenticationProvider)

  val atlassianHostUserAction =
    new AtlassianHostUserAction(parser,
                                jwtActionRefiner,
                                atlassianHostUserActionRefinerFactory)

  "JwtActionRefiner" when {

    "refining a standard Request" should {

      "successfully refine request to JwtRequest" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
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
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
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

      "successfully refine JwtRequest with context QSH claim to AtlassianHostUserRequest" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = CanonicalPlayHttpRequest(request)
            val qsh = ContextQshProvider.qsh
            val customClaims =
              Seq("iss" -> host.clientKey, "sub" -> subject, "qsh" -> qsh)
            forAll(signedJwtStringGen(host.sharedSecret, customClaims)) { jwt =>
              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)
              val hostUser =
                DefaultAtlassianHostUser(host, None, Option(subject))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                atlassianHostUserActionRefinerFactory
                  .withQshFrom(ContextQshProvider)
                  .refine(jwtRequest)
              }
              result mustBe Right(
                AtlassianHostUserRequest(hostUser, jwtRequest))
            }
        }
      }

      /*
       * This is to test that the JwtReader accepts JWT tokens without any QSH claim
       *
       * At the latest after Atlassian has rolled out the change to include qsh in all JWTs (in particular context JWTs)
       * atlassian-jwt should be updated to not accept JWTs without qsh claims and this test should then fail.
       *
       * https://community.developer.atlassian.com/t/advance-notice-of-vulnerability-bypass-connect-app-qsh-verification-via-context-jwts/46659/10?u=tbinna
       */
      "successfully refine JwtRequest with without any QSH claim and ContextQshProvider to AtlassianHostUserRequest" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = CanonicalPlayHttpRequest(request)
            val customClaims =
              Seq("iss" -> host.clientKey, "sub" -> subject)
            forAll(signedJwtStringGen(host.sharedSecret, customClaims)) { jwt =>
              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)
              val hostUser =
                DefaultAtlassianHostUser(host, None, Option(subject))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                atlassianHostUserActionRefinerFactory
                  .withQshFrom(ContextQshProvider)
                  .refine(jwtRequest)
              }
              result mustBe Right(
                AtlassianHostUserRequest(hostUser, jwtRequest))
            }
        }
      }

      "fail to refine JwtRequest with HTTP request QSH claim and ContextQshProvider" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = CanonicalPlayHttpRequest(request)
            val qsh = CanonicalHttpRequestQshProvider.qsh(canonicalHttpRequest)
            val customClaims =
              Seq("iss" -> host.clientKey, "sub" -> subject, "qsh" -> qsh)
            forAll(signedJwtStringGen(host.sharedSecret, customClaims)) { jwt =>
              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result =
                atlassianHostUserActionRefinerFactory
                  .withQshFrom(ContextQshProvider)
                  .refine(jwtRequest)

              status(result.map(_.left.value)) mustBe UNAUTHORIZED
              contentAsString(result.map(_.left.value)) startsWith "JWT validation failed"
            }
        }
      }

      "successfully refine JwtRequest with HTTP request QSH claim to AtlassianHostUserRequest" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = CanonicalPlayHttpRequest(request)
            val qsh = CanonicalHttpRequestQshProvider.qsh(canonicalHttpRequest)
            val customClaims =
              Seq("iss" -> host.clientKey, "sub" -> subject, "qsh" -> qsh)
            forAll(signedJwtStringGen(host.sharedSecret, customClaims)) { jwt =>
              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)
              val hostUser =
                DefaultAtlassianHostUser(host, None, Option(subject))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                atlassianHostUserActionRefinerFactory
                  .withQshFrom(CanonicalHttpRequestQshProvider)
                  .refine(jwtRequest)
              }
              result mustBe Right(
                AtlassianHostUserRequest(hostUser, jwtRequest))
            }
        }
      }

      /*
       * This is to test that the JwtReader accepts JWT tokens without any QSH claim
       *
       * At the latest after Atlassian has rolled out the change to include qsh in all JWTs (in particular context JWTs)
       * atlassian-jwt should be updated to not accept JWTs without qsh claims and this test should then fail.
       *
       * https://community.developer.atlassian.com/t/advance-notice-of-vulnerability-bypass-connect-app-qsh-verification-via-context-jwts/46659/10?u=tbinna
       */
      "successfully refine JwtRequest with without any QSH claim and CanonicalHttpRequestQshProvider to AtlassianHostUserRequest" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = CanonicalPlayHttpRequest(request)
            val customClaims =
              Seq("iss" -> host.clientKey, "sub" -> subject)
            forAll(signedJwtStringGen(host.sharedSecret, customClaims)) { jwt =>
              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)
              val hostUser =
                DefaultAtlassianHostUser(host, None, Option(subject))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                atlassianHostUserActionRefinerFactory
                  .withQshFrom(CanonicalHttpRequestQshProvider)
                  .refine(jwtRequest)
              }
              result mustBe Right(
                AtlassianHostUserRequest(hostUser, jwtRequest))
            }
        }
      }

      "fail to refine JwtRequest with context QSH claim and CanonicalHttpRequestQshProvider" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = CanonicalPlayHttpRequest(request)
            val qsh = ContextQshProvider.qsh
            val customClaims =
              Seq("iss" -> host.clientKey, "sub" -> subject, "qsh" -> qsh)
            forAll(signedJwtStringGen(host.sharedSecret, customClaims)) { jwt =>
              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result =
                atlassianHostUserActionRefinerFactory
                  .withQshFrom(CanonicalHttpRequestQshProvider)
                  .refine(jwtRequest)

              status(result.map(_.left.value)) mustBe UNAUTHORIZED
              contentAsString(result.map(_.left.value)) startsWith "JWT validation failed"
            }
        }
      }

      "fail to refine request if authentication fails" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
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
                atlassianHostUserActionRefinerFactory
                  .withQshFrom(ContextQshProvider)
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
