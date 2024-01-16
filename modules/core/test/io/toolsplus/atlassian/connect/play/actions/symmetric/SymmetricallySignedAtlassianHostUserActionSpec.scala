package io.toolsplus.atlassian.connect.play.actions.symmetric

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.actions.{JwtActionRefiner, JwtRequest, symmetric}
import io.toolsplus.atlassian.connect.play.api.models.DefaultAtlassianHostUser
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.auth.jwt
import io.toolsplus.atlassian.connect.play.auth.jwt.symmetric.SymmetricJwtAuthenticationProvider
import io.toolsplus.atlassian.connect.play.auth.jwt.{CanonicalHttpRequestQshProvider, ContextQshProvider, JwtCredentials}
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import org.scalacheck.Gen.alphaStr
import org.scalacheck.Shrink
import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.BodyParsers
import play.api.mvc.Results.Unauthorized
import play.api.test.Helpers.{contentAsString, status}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SymmetricallySignedAtlassianHostUserActionSpec
    extends TestSpec
    with GuiceOneAppPerSuite
    with EitherValues {

  val jwtActionRefiner = new JwtActionRefiner()
  val parser: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]

  val hostRepository: AtlassianHostRepository = mock[AtlassianHostRepository]
  val symmetricJwtAuthenticationProvider =
    new SymmetricJwtAuthenticationProvider(hostRepository)

  val atlassianHostUserAction =
    new SymmetricallySignedAtlassianHostUserAction(
      parser,
      jwtActionRefiner,
      symmetricJwtAuthenticationProvider)

  "SymmetricallySignedSymmetricallySignedAtlassianHostUserActionRefiner" when {

    "refining a symmetrically signed JwtRequest with a context QSH claim" should {

      implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny

      val refiner =
        SymmetricallySignedAtlassianHostUserActionRefiner(
          symmetricJwtAuthenticationProvider,
          ContextQshProvider)
      val qsh = ContextQshProvider.qsh

      "successfully refine JwtRequest to AtlassianHostUserRequest" in {
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = jwt.CanonicalPlayHttpRequest(request)
            val defaultClaims =
              Seq("iss" -> host.clientKey, "sub" -> subject, "qsh" -> qsh)
            forAll(
              signedSymmetricJwtStringGen(host.sharedSecret, defaultClaims)) {
              jwt =>
                val jwtRequest =
                  JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)

                val hostUser =
                  DefaultAtlassianHostUser(host, Option(subject))

                (hostRepository
                  .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                  .successful(Some(host))

                val result = await {
                  refiner.refine(jwtRequest)
                }
                result mustBe Right(
                  ConnectAtlassianHostUserRequest(hostUser, jwtRequest))
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
      "successfully refine request without any QSH claim to AtlassianHostUserRequest" in {
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = jwt.CanonicalPlayHttpRequest(request)
            forAll(
              signedSymmetricJwtStringGen(host.sharedSecret,
                                          Seq("iss" -> host.clientKey,
                                              "sub" -> subject))) { jwt =>
              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)
              val hostUser =
                DefaultAtlassianHostUser(host, Option(subject))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                refiner.refine(jwtRequest)
              }
              result mustBe Right(
                ConnectAtlassianHostUserRequest(hostUser, jwtRequest))
            }
        }
      }

      "fail to refine JwtRequest with context QSH claim and CanonicalHttpRequestQshProvider" in {
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = jwt.CanonicalPlayHttpRequest(request)
            val defaultClaims =
              Seq("iss" -> host.clientKey, "sub" -> subject, "qsh" -> qsh)
            forAll(
              signedSymmetricJwtStringGen(host.sharedSecret, defaultClaims)) {
              jwt =>
                val jwtRequest =
                  JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)

                (hostRepository
                  .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                  .successful(Some(host))

                val result =
                  SymmetricallySignedAtlassianHostUserActionRefiner(
                    symmetricJwtAuthenticationProvider,
                    CanonicalHttpRequestQshProvider)
                    .refine(jwtRequest)

                status(result.map(_.left.value)) mustBe UNAUTHORIZED
                contentAsString(result.map(_.left.value)) startsWith "JWT validation failed"
            }
        }
      }

      "fail to refine symmetrically signed request if host is not installed" in {
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = jwt.CanonicalPlayHttpRequest(request)
            val defaultClaims =
              Seq("iss" -> host.clientKey, "sub" -> subject, "qsh" -> qsh)
            forAll(
              signedSymmetricJwtStringGen(host.sharedSecret, defaultClaims)) {
              jwt =>
                val jwtRequest =
                  JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)

                (hostRepository
                  .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                  .successful(None)

                val result = await {
                  refiner.refine(jwtRequest)
                }

                val expectedMessage =
                  s"JWT validation failed: Could not find an installed host for the provided client key: ${host.clientKey}"
                result.left.value mustBe Unauthorized(expectedMessage)
            }
        }
      }

      "fail to refine symmetrically signed request if authentication fails" in {
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            forAll(symmetricJwtCredentialsGen(host, subject)) { credentials =>
              val invalidCredentials =
                credentials.copy(rawJwt = credentials.rawJwt.dropRight(5))
              val jwtRequest = JwtRequest(invalidCredentials, request)

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                refiner.refine(jwtRequest)
              }
              val expectedMessage =
                s"JWT validation failed: ${invalidCredentials.rawJwt}"
              result.left.value mustBe Unauthorized(expectedMessage)
            }
        }
      }
    }

    "refining a symmetrically signed JwtRequest with a HTTP request QSH claim" should {
      implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny

      val refiner =
        SymmetricallySignedAtlassianHostUserActionRefiner(
          symmetricJwtAuthenticationProvider,
          CanonicalHttpRequestQshProvider)

      "successfully refine to AtlassianHostUserRequest" in {
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = jwt.CanonicalPlayHttpRequest(request)
            forAll(
              signedSymmetricJwtStringGen(
                host.sharedSecret,
                Seq("iss" -> host.clientKey,
                    "sub" -> subject,
                    "qsh" -> CanonicalHttpRequestQshProvider.qsh(
                      canonicalHttpRequest)))) { jwt =>
              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)
              val hostUser =
                DefaultAtlassianHostUser(host, Option(subject))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                refiner.refine(jwtRequest)
              }
              result mustBe Right(
                ConnectAtlassianHostUserRequest(hostUser, jwtRequest))
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
      "successfully refine request without any QSH claim to AtlassianHostUserRequest" in {
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = jwt.CanonicalPlayHttpRequest(request)
            forAll(
              signedSymmetricJwtStringGen(host.sharedSecret,
                                          Seq("iss" -> host.clientKey,
                                              "sub" -> subject))) { jwt =>
              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)
              val hostUser =
                DefaultAtlassianHostUser(host, Option(subject))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                refiner.refine(jwtRequest)
              }
              result mustBe Right(
                ConnectAtlassianHostUserRequest(hostUser, jwtRequest))
            }
        }
      }

      "fail to refine if QSH provider is ContextQshProvider" in {
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = jwt.CanonicalPlayHttpRequest(request)
            forAll(
              signedSymmetricJwtStringGen(
                host.sharedSecret,
                Seq("iss" -> host.clientKey,
                    "sub" -> subject,
                    "qsh" -> CanonicalHttpRequestQshProvider.qsh(
                      canonicalHttpRequest)))) { jwt =>
              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result =
                SymmetricallySignedAtlassianHostUserActionRefiner(
                  symmetricJwtAuthenticationProvider,
                  ContextQshProvider)
                  .refine(jwtRequest)

              status(result.map(_.left.value)) mustBe UNAUTHORIZED
              contentAsString(result.map(_.left.value)) startsWith "JWT validation failed"
            }
        }
      }

      "fail to refine symmetrically signed request if host is not installed" in {
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = jwt.CanonicalPlayHttpRequest(request)
            forAll(
              signedSymmetricJwtStringGen(
                host.sharedSecret,
                Seq("iss" -> host.clientKey,
                    "sub" -> subject,
                    "qsh" -> CanonicalHttpRequestQshProvider.qsh(
                      canonicalHttpRequest)))) { jwt =>
              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(None)

              val result = await {
                refiner.refine(jwtRequest)
              }

              val expectedMessage =
                s"JWT validation failed: Could not find an installed host for the provided client key: ${host.clientKey}"
              result.left.value mustBe Unauthorized(expectedMessage)
            }
        }
      }
    }
  }

  "SymmetricallySignedAtlassianHostUserAction" when {

    "importing Implicits object members" should {

      "implicitly convert a HostUserRequest to a AtlassianHostUser" in {
        forAll(atlassianHostUserGen,
          playRequestGen,
          symmetricJwtCredentialsGen()) {
          (hostUser, request, credentials) =>
            val jwtRequest = JwtRequest(credentials, request)
            val hostUserRequest =
              ConnectAtlassianHostUserRequest(hostUser, jwtRequest)
            atlassianHostUserAction.Implicits.hostUserRequestToHostUser(
              hostUserRequest) mustBe hostUser
        }
      }

    }

  }


}
