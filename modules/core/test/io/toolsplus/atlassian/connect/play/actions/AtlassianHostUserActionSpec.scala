package io.toolsplus.atlassian.connect.play.actions

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.models.DefaultAtlassianHostUser
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.auth.jwt
import io.toolsplus.atlassian.connect.play.auth.jwt.asymmetric.{
  AsymmetricJwtAuthenticationProvider,
  PublicKeyProvider
}
import io.toolsplus.atlassian.connect.play.auth.jwt.symmetric.SymmetricJwtAuthenticationProvider
import io.toolsplus.atlassian.connect.play.auth.jwt.{
  CanonicalHttpRequestQshProvider,
  CanonicalPlayHttpRequest,
  ContextQshProvider,
  JwtCredentials
}
import io.toolsplus.atlassian.connect.play.models.PlayAddonProperties
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
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

import java.security.interfaces.RSAPublicKey
import java.security.{KeyPair, PrivateKey}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AtlassianHostUserActionSpec
    extends TestSpec
    with GuiceOneAppPerSuite
    with EitherValues {

  val config: Configuration = app.configuration
  val appProperties = new PlayAddonProperties(config)

  val parser: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]

  val hostRepository: AtlassianHostRepository = mock[AtlassianHostRepository]
  val symmetricJwtAuthenticationProvider =
    new SymmetricJwtAuthenticationProvider(hostRepository)

  val keyId: String = "0e50fccb-239d-4991-a5db-dc850ba3f236"
  val keyPair: KeyPair = JwtTestHelper.generateKeyPair()
  val publicKey: RSAPublicKey = keyPair.getPublic.asInstanceOf[RSAPublicKey]
  val privateKey: PrivateKey = keyPair.getPrivate
  val publicKeyProvider: PublicKeyProvider = mock[PublicKeyProvider]
  val asymmetricJwtAuthenticationProvider =
    new AsymmetricJwtAuthenticationProvider(appProperties,
                                            publicKeyProvider,
                                            hostRepository)

  val jwtActionRefiner = new JwtActionRefiner()

  val atlassianHostUserAction =
    new AtlassianHostUserAction(parser, jwtActionRefiner)

  "JwtActionRefiner" when {

    "refining a standard Request" should {

      "successfully refine request to JwtRequest" in {
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(signedSymmetricJwtStringGen(), playRequestGen) {
          (rawJwt, request) =>
            val jwtHeader = HeaderNames.AUTHORIZATION -> s"${JwtExtractor.AuthorizationHeaderPrefix} $rawJwt"
            val jwtRequest = request.withHeaders(jwtHeader)
            val jwtCredentials =
              jwt.JwtCredentials(rawJwt, CanonicalPlayHttpRequest(jwtRequest))
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

    "refining a symmetrically signed JwtRequest with a context QSH claim" should {

      implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny

      val refiner =
        AtlassianHostUserActionRefiner(symmetricJwtAuthenticationProvider,
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
                AtlassianHostUserRequest(hostUser, jwtRequest))
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
                  AtlassianHostUserActionRefiner(
                    symmetricJwtAuthenticationProvider,
                    CanonicalHttpRequestQshProvider)
                    .refine(jwtRequest)

                status(result.map(_.left.value)) mustBe UNAUTHORIZED
                contentAsString(result.map(_.left.value)) startsWith "JWT validation failed"
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
        AtlassianHostUserActionRefiner(symmetricJwtAuthenticationProvider,
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
                AtlassianHostUserRequest(hostUser, jwtRequest))
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
                AtlassianHostUserActionRefiner(
                  symmetricJwtAuthenticationProvider,
                  ContextQshProvider)
                  .refine(jwtRequest)

              status(result.map(_.left.value)) mustBe UNAUTHORIZED
              contentAsString(result.map(_.left.value)) startsWith "JWT validation failed"
            }
        }
      }
    }

    "refining an asymmetrically signed JwtRequest with a context QSH claim" should {
      implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny

      val refiner =
        AtlassianHostUserActionRefiner(asymmetricJwtAuthenticationProvider,
                                       ContextQshProvider)

      "successfully refine to AtlassianHostUserRequest" in {
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = jwt.CanonicalPlayHttpRequest(request)
            forAll(
              signedAsymmetricJwtStringGen(
                keyId,
                privateKey,
                Seq("iss" -> host.clientKey,
                    "sub" -> subject,
                    "qsh" -> ContextQshProvider.qsh,
                    "aud" -> appProperties.baseUrl))) { jwt =>
              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)

              val hostUser =
                DefaultAtlassianHostUser(host, Option(subject))

              (publicKeyProvider
                .fetchPublicKey(_: String)) expects keyId returning Future
                .successful(Right(JwtTestHelper.toPemString(publicKey)))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                refiner.refine(jwtRequest)
              }
              result mustBe Right(
                AtlassianHostUserRequest(hostUser, jwtRequest))
            }
        }
      }

      "fail to refine if QSH provider is CanonicalHttpRequestQshProvider" in {
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = jwt.CanonicalPlayHttpRequest(request)
            forAll(
              signedAsymmetricJwtStringGen(
                keyId,
                privateKey,
                Seq("iss" -> host.clientKey,
                    "sub" -> subject,
                    "qsh" -> ContextQshProvider.qsh,
                    "aud" -> appProperties.baseUrl))) { jwt =>

              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)

              (publicKeyProvider
                .fetchPublicKey(_: String)) expects keyId returning Future
                .successful(Right(JwtTestHelper.toPemString(publicKey)))

              val result =
                AtlassianHostUserActionRefiner(
                  asymmetricJwtAuthenticationProvider,
                  CanonicalHttpRequestQshProvider)
                  .refine(jwtRequest)

              status(result.map(_.left.value)) mustBe UNAUTHORIZED
              contentAsString(result.map(_.left.value)) startsWith "JWT validation failed"
            }
        }
      }
    }

    "refining an asymmetrically signed JwtRequest with a HTTP request QSH claim" should {
      implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny

      val refiner =
        AtlassianHostUserActionRefiner(asymmetricJwtAuthenticationProvider,
                                       CanonicalHttpRequestQshProvider)

      "successfully refine to AtlassianHostUserRequest" in {
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = jwt.CanonicalPlayHttpRequest(request)
            forAll(
              signedAsymmetricJwtStringGen(
                keyId,
                privateKey,
                Seq("iss" -> host.clientKey,
                    "sub" -> subject,
                    "qsh" -> CanonicalHttpRequestQshProvider.qsh(
                      canonicalHttpRequest),
                    "aud" -> appProperties.baseUrl)
              )) { jwt =>
              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)

              val hostUser =
                DefaultAtlassianHostUser(host, Option(subject))

              (publicKeyProvider
                .fetchPublicKey(_: String)) expects keyId returning Future
                .successful(Right(JwtTestHelper.toPemString(publicKey)))

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(Some(host))

              val result = await {
                refiner.refine(jwtRequest)
              }
              result mustBe Right(
                AtlassianHostUserRequest(hostUser, jwtRequest))
            }
        }
      }

      "fail to refine if QSH provider is ContextQshProvider" in {
        forAll(playRequestGen, atlassianHostGen, alphaStr) {
          (request, host, subject) =>
            val canonicalHttpRequest = jwt.CanonicalPlayHttpRequest(request)
            forAll(
              signedAsymmetricJwtStringGen(
                keyId,
                privateKey,
                Seq("iss" -> host.clientKey,
                    "sub" -> subject,
                    "qsh" -> CanonicalHttpRequestQshProvider.qsh(
                      canonicalHttpRequest),
                    "aud" -> appProperties.baseUrl)
              )) { jwt =>

              val jwtRequest =
                JwtRequest(JwtCredentials(jwt, canonicalHttpRequest), request)

              (publicKeyProvider
                .fetchPublicKey(_: String)) expects keyId returning Future
                .successful(Right(JwtTestHelper.toPemString(publicKey)))

              val result =
                AtlassianHostUserActionRefiner(
                  asymmetricJwtAuthenticationProvider,
                  ContextQshProvider)
                  .refine(jwtRequest)

              status(result.map(_.left.value)) mustBe UNAUTHORIZED
              contentAsString(result.map(_.left.value)) startsWith "JWT validation failed"
            }
        }
      }
    }
  }

  "AtlassianHostUserAction" when {

    "importing Implicits object members" should {

      "implicitly convert a HostUserRequest to a AtlassianHostUser" in {
        forAll(atlassianHostUserGen,
               playRequestGen,
               symmetricJwtCredentialsGen()) {
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
