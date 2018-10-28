package io.toolsplus.atlassian.connect.play.auth.jwt

import com.netaporter.uri.Uri
import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.auth.jwt.JwtGenerator._
import io.toolsplus.atlassian.connect.play.models.{AtlassianConnectProperties, PlayAddonProperties}
import io.toolsplus.atlassian.connect.play.ws.AtlassianHostUriResolver
import io.toolsplus.atlassian.connect.play.ws.UriImplicits._
import io.toolsplus.atlassian.jwt._
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import org.scalacheck.Gen._
import org.scalacheck.{Gen, Shrink}
import org.scalatest.Assertion
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.Future

class JwtGeneratorSpec extends TestSpec with GuiceOneAppPerSuite {

  val config = app.configuration

  val addonProperties = new PlayAddonProperties(config)
  val connectProperties = new AtlassianConnectProperties(config)

  val hostRepository = mock[AtlassianHostRepository]
  val hostUriResolver = new AtlassianHostUriResolver(hostRepository)

  val jwtGenerator =
    new JwtGenerator(addonProperties, connectProperties, hostUriResolver)

  val toleranceSeconds = 2

  "A JwtGenerator" when {

    "asked to create a token by providing a Atlassian host" should {

      "successfully generate token" in {
        forAll(methodGen, pathGen, atlassianHostGen) {
          (method, relativePath, host) =>
            val absoluteUri = absoluteHostUri(host.baseUrl, relativePath)
            jwtGenerator.createJwtToken(method, absoluteUri, host) match {
              case Right(_) => succeed
              case Left(_)  => fail
            }
        }
      }

      "set token expiry based on configured 'jwtExpirationTime'" in {
        tokenPropertyTest { jwt: Jwt =>
          val now = System.currentTimeMillis / 1000
          val expiry = jwt.claims.getExpirationTime.getTime / 1000
          val expectedExpiry = now + connectProperties.jwtExpirationTime
          expiry mustBe expectedExpiry +- toleranceSeconds
        }
      }

      "set token issuer to add-on key" in {
        tokenPropertyTest { jwt: Jwt =>
          jwt.iss mustBe addonProperties.key
        }
      }

      "set query string hash to proper generated value" in {
        tokenPropertyTest { jwt =>
          val qsh = jwt.claims.getClaim("qsh").asInstanceOf[String]
          Option(qsh) must not be empty
          qsh must not be ""
        }
      }

      "sign token with host's shared secret" in {
        forAll(methodGen, pathGen, atlassianHostGen) {
          (method, relativePath, host) =>
            val absoluteUri = absoluteHostUri(host.baseUrl, relativePath)
            jwtGenerator.createJwtToken(method, absoluteUri, host) match {
              case Right(rawJwt) => {
                val request =
                  CanonicalUriHttpRequest(method, absoluteUri, host.baseUrl)
                val qsh =
                  HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
                JwtReader(host.sharedSecret).readAndVerify(rawJwt, qsh) match {
                  case Right(_) => succeed
                  case Left(e)  => fail(e)
                }
              }
              case Left(_) => fail
            }
        }
      }

      "fail if given URI is not absolute" in {
        forAll(methodGen, pathGen, atlassianHostGen) {
          (method, relativePath, host) =>
            val result =
              jwtGenerator.createJwtToken(method, Uri.parse(relativePath), host)
            result mustBe Left(RelativeUriError)
        }
      }

      "fail if given URI is not matching given host" in {
        forAll(methodGen, pathGen, atlassianHostGen, hostNameGen) {
          (method, relativePath, host, randomHost) =>
            val randomBaseUrl = s"https://$randomHost.atlassian.net"
            val absoluteUri = absoluteHostUri(randomBaseUrl, relativePath)
            val result = jwtGenerator.createJwtToken(method, absoluteUri, host)
            result mustBe Left(BaseUrlMismatchError)
        }
      }

    }

    "asked to generate a token without a Atlassian host" should {

      "successfully generate token" in {
        forAll(methodGen, pathGen, atlassianHostGen) {
          (method, relativePath, host) =>
            val absoluteUri = absoluteHostUri(host.baseUrl, relativePath)

            (hostRepository
              .findByBaseUrl(_: String)) expects host.baseUrl returning Future
              .successful(Some(host))

            val result = await {
              jwtGenerator.createJwtToken(method, absoluteUri)
            }
            result match {
              case Right(_) => succeed
              case Left(_)  => fail
            }
        }
      }

      "fail if given URI is not absolute" in {
        forAll(methodGen, pathGen) { (method, relativePath) =>
          val result = await {
            jwtGenerator.createJwtToken(method, Uri.parse(relativePath))
          }
          result mustBe Left(RelativeUriError)
        }
      }

      "fail if no host for given URI is found" in {
        implicit val doNotShrinkStrings: Shrink[String] = Shrink.shrinkAny
        forAll(methodGen, pathGen, hostNameGen) {
          (method, relativePath, randomHost) =>
            val randomBaseUrl =
              s"https://$randomHost.atlassian.net"
            val absoluteUri = absoluteHostUri(randomBaseUrl, relativePath)

            (hostRepository
              .findByBaseUrl(_: String)) expects randomBaseUrl returning Future
              .successful(None)

            val result = await {
              jwtGenerator.createJwtToken(method, absoluteUri)
            }
            result mustBe Left(AtlassianHostNotFoundError(absoluteUri))
        }
      }

      "fail if secret key is less than 256 bits" in forAll(methodGen, pathGen, atlassianHostGen) {
        (method, relativePath, randomHost) =>
          val absoluteUri = absoluteHostUri(randomHost.baseUrl, relativePath)
          val hostWithInvalidKey = randomHost.copy(sharedSecret = "INVALID")
          val result = jwtGenerator.createJwtToken(method, absoluteUri, hostWithInvalidKey)

          result mustBe Left(InvalidSecretKey)
      }

    }

  }

  private def hostNameGen: Gen[String] = alphaStr.suchThat(!_.isEmpty)

  private def absoluteHostUri(basePath: String, relativePath: String): Uri =
    Uri.parse(basePath).append(Uri.parse(relativePath))

  private def tokenPropertyTest(assertion: Jwt => Assertion) =
    forAll(methodGen, pathGen, atlassianHostGen) {
      (method, relativePath, host) =>
        val absoluteUri = absoluteHostUri(host.baseUrl, relativePath)
        val result = jwtGenerator.createJwtToken(method, absoluteUri, host)
        validate(assertion)(result)
    }

  private def validate(assertion: Jwt => Assertion)(
      result: Either[JwtGeneratorError, RawJwt]) = {
    result match {
      case Right(rawJwt) =>
        JwtParser.parse(rawJwt) match {
          case Right(jwt) => assertion(jwt)
          case Left(e)    => fail(e)
        }
      case Left(_) => fail
    }
  }

}
