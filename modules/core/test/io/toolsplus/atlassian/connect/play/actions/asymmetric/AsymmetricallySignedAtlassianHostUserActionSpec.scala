package io.toolsplus.atlassian.connect.play.actions.asymmetric

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.actions.{JwtActionRefiner, JwtRequest}
import io.toolsplus.atlassian.connect.play.api.models.DefaultAtlassianHostUser
import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.auth.jwt
import io.toolsplus.atlassian.connect.play.auth.jwt.asymmetric.{AsymmetricJwtAuthenticationProvider, PublicKeyProvider}
import io.toolsplus.atlassian.connect.play.auth.jwt.symmetric.SymmetricJwtAuthenticationProvider
import io.toolsplus.atlassian.connect.play.auth.jwt.{CanonicalHttpRequestQshProvider, ContextQshProvider, JwtCredentials}
import io.toolsplus.atlassian.connect.play.models.PlayAddonProperties
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import org.scalacheck.Gen.alphaStr
import org.scalacheck.Shrink
import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{contentAsString, status}

import java.security.interfaces.RSAPublicKey
import java.security.{KeyPair, PrivateKey}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AsymmetricallySignedAtlassianHostUserActionSpec
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

  "AsymmetricallySignedAtlassianHostUserActionRefiner" when {

    "refining an asymmetrically signed JwtRequest with a context QSH claim" should {
      implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny

      val refiner =
        AsymmetricallySignedAtlassianHostUserActionRefiner(asymmetricJwtAuthenticationProvider,
          ContextQshProvider)

      "successfully refine to MaybeAtlassianHostUserRequest if host is installed" in {
        forAll(playRequestGen, connectAtlassianHostGen, alphaStr) {
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
                MaybeAtlassianHostUserRequest(Some(hostUser), jwtRequest))
            }
        }
      }

      "successfully refine to MaybeAtlassianHostUserRequest if host is not installed" in {
        forAll(playRequestGen, connectAtlassianHostGen, alphaStr) {
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

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(None)

              val result = await {
                refiner.refine(jwtRequest)
              }
              result mustBe Right(
                MaybeAtlassianHostUserRequest(None, jwtRequest))
            }
        }
      }

      "fail to refine if QSH provider is CanonicalHttpRequestQshProvider" in {
        forAll(playRequestGen, connectAtlassianHostGen, alphaStr) {
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
                AsymmetricallySignedAtlassianHostUserActionRefiner(
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
        AsymmetricallySignedAtlassianHostUserActionRefiner(asymmetricJwtAuthenticationProvider,
          CanonicalHttpRequestQshProvider)

      "successfully refine to MaybeAtlassianHostUserRequest if host is installed" in {
        forAll(playRequestGen, connectAtlassianHostGen, alphaStr) {
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
                MaybeAtlassianHostUserRequest(Some(hostUser), jwtRequest))
            }
        }
      }

      "successfully refine to MaybeAtlassianHostUserRequest if host is not installed" in {
        forAll(playRequestGen, connectAtlassianHostGen, alphaStr) {
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

              (hostRepository
                .findByClientKey(_: ClientKey)) expects host.clientKey returning Future
                .successful(None)

              val result = await {
                refiner.refine(jwtRequest)
              }
              result mustBe Right(
                MaybeAtlassianHostUserRequest(None, jwtRequest))
            }
        }
      }

      "fail to refine if QSH provider is ContextQshProvider" in {
        forAll(playRequestGen, connectAtlassianHostGen, alphaStr) {
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
                AsymmetricallySignedAtlassianHostUserActionRefiner(
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


}
