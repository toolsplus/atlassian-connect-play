package io.toolsplus.atlassian.connect.play.auth.jwt.symmetric

import io.lemonlabs.uri.Url
import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.api.repositories.AtlassianHostRepository
import io.toolsplus.atlassian.connect.play.auth.jwt.CanonicalUriHttpRequest
import io.toolsplus.atlassian.connect.play.auth.jwt.symmetric.JwtGenerator.{
  BaseUrlMismatchError,
  InvalidSecretKey,
  JwtGeneratorError,
  RelativeUriError
}
import io.toolsplus.atlassian.connect.play.models.{
  PlayAddonProperties,
  PlayAtlassianConnectProperties
}
import io.toolsplus.atlassian.jwt._
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.symmetric.SymmetricJwtReader
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalatest.Assertion
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration

import java.net.URI

class JwtGeneratorSpec extends TestSpec with GuiceOneAppPerSuite {

  val config: Configuration = app.configuration

  val addonProperties = new PlayAddonProperties(config)
  val connectProperties = new PlayAtlassianConnectProperties(config)

  val hostRepository: AtlassianHostRepository = mock[AtlassianHostRepository]

  val jwtGenerator =
    new JwtGenerator(addonProperties, connectProperties)

  val toleranceSeconds = 2

  "A JwtGenerator" when {

    "asked to create a token by providing a Atlassian host" should {

      "successfully generate token" in {
        forAll(methodGen, rootRelativePathGen, atlassianHostGen) {
          (method, relativePath, host) =>
            val absoluteUri = absoluteHostUri(host.baseUrl, relativePath)
            jwtGenerator.createJwtToken(method, absoluteUri, host) match {
              case Right(_) => succeed
              case Left(_)  => fail()
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
        forAll(methodGen, rootRelativePathGen, atlassianHostGen) {
          (method, relativePath, host) =>
            val absoluteUri = absoluteHostUri(host.baseUrl, relativePath)
            val hostContextPath = Option(URI.create(host.baseUrl).getPath)
            jwtGenerator.createJwtToken(method, absoluteUri, host) match {
              case Right(rawJwt) =>
                val request =
                  CanonicalUriHttpRequest(method, absoluteUri, hostContextPath)
                val qsh =
                  HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
                SymmetricJwtReader(host.sharedSecret)
                  .readAndVerify(rawJwt, qsh) match {
                  case Right(_) => succeed
                  case Left(e)  => fail(e)
                }
              case Left(_) => fail()
            }
        }
      }

      "fail if given URI is not absolute" in {
        forAll(methodGen, rootRelativePathGen, atlassianHostGen) {
          (method, relativePath, host) =>
            val result =
              jwtGenerator.createJwtToken(
                method,
                URI.create(relativePath),
                host
              )
            result mustBe Left(RelativeUriError)
        }
      }

      "fail if given URI is not matching given host" in {
        forAll(methodGen, rootRelativePathGen, atlassianHostGen, hostNameGen) {
          (method, relativePath, host, randomHost) =>
            val randomBaseUrl = s"https://$randomHost.atlassian.net"
            val absoluteUri = absoluteHostUri(randomBaseUrl, relativePath)
            val result = jwtGenerator.createJwtToken(method, absoluteUri, host)
            result mustBe Left(BaseUrlMismatchError)
        }
      }

      "fail if secret key is less than 256 bits" in forAll(
        methodGen,
        rootRelativePathGen,
        atlassianHostGen
      ) { (method, relativePath, randomHost) =>
        val absoluteUri = absoluteHostUri(randomHost.baseUrl, relativePath)
        val hostWithInvalidKey = randomHost.copy(sharedSecret = "INVALID")
        val result =
          jwtGenerator.createJwtToken(method, absoluteUri, hostWithInvalidKey)

        result mustBe Left(InvalidSecretKey)
      }

    }
  }

  private def hostNameGen: Gen[String] = alphaStr.suchThat(_.nonEmpty)

  private def absoluteHostUri(baseUrl: String, relativePath: String): URI = {
    val base = Url.parse(baseUrl)
    val relative = Url.parse(relativePath)
    URI.create(
      base.withPath(base.path.addParts(relative.path.parts)).toString()
    )
  }

  private def tokenPropertyTest(assertion: Jwt => Assertion) =
    forAll(methodGen, rootRelativePathGen, atlassianHostGen) {
      (method, relativePath, host) =>
        val absoluteUri = absoluteHostUri(host.baseUrl, relativePath)
        val result = jwtGenerator.createJwtToken(method, absoluteUri, host)
        validate(assertion)(result)
    }

  private def validate(
      assertion: Jwt => Assertion
  )(result: Either[JwtGeneratorError, RawJwt]) = {
    result match {
      case Right(rawJwt) =>
        JwtParser.parse(rawJwt) match {
          case Right(jwt) => assertion(jwt)
          case Left(e)    => fail(e)
        }
      case Left(_) => fail()
    }
  }

}
