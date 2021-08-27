package io.toolsplus.atlassian.connect.play.auth.jwt.asymmetric

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.auth.jwt.InvalidJwtError
import io.toolsplus.atlassian.connect.play.models.AtlassianConnectProperties
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc._
import play.api.routing.Router
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.api.{BuiltInComponentsFromContext, Configuration}
import play.core.server.Server
import play.filters.HttpFiltersComponents

import java.security.KeyPair
import java.security.interfaces.RSAPublicKey
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PublicKeyProviderSpec
    extends TestSpec
    with GuiceOneAppPerSuite
    with EitherValues {

  val connectProperties = new AtlassianConnectProperties(
    Configuration("atlassian.connect.publicKeyHostBaseUrl" -> ""))

  val keyPair: KeyPair = JwtTestHelper.generateKeyPair()
  val publicKey: RSAPublicKey = keyPair.getPublic.asInstanceOf[RSAPublicKey]
  val pemEncodedKey: String = JwtTestHelper.toPemString(publicKey)

  "Given a PublicKeyProvider" when {

    "asked to fetch a public key" should {

      "successfully fetch an existing public key" in withPublicKeyProvider {
        provider =>
          val result =
            Await.result(provider.fetchPublicKey("exists"), 10.seconds)
          result.value mustBe pemEncodedKey
      }

      "fail to fetch an non existing public key" in withPublicKeyProvider {
        provider =>
          val result =
            Await.result(provider.fetchPublicKey("notFound"), 10.seconds)
          result.left.value mustBe InvalidJwtError(
            s"Failed to find public key for keyId 'notFound'")
      }

      "fail to fetch an if key server returns unexpected error" in withPublicKeyProvider {
        provider =>
          val result =
            Await.result(provider.fetchPublicKey("400"), 10.seconds)
          result.left.value mustBe a[InvalidJwtError]
      }
    }
  }

  def withPublicKeyProvider[T](block: PublicKeyProvider => T): T = {
    Server.withApplicationFromContext() { context =>
      new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
        override def router: Router = Router.from {
          case GET(p"/exists") =>
            Action { _ =>
              Results.Ok(pemEncodedKey)
            }
          case GET(p"/notFound") =>
            Action { _ =>
              Results.NotFound
            }
          case GET(p"/400") =>
            Action { _ =>
              Results.BadRequest
            }
        }
      }.application
    } { implicit port =>
      WsTestClient.withClient { client =>
        block(new PublicKeyProvider(client, connectProperties))
      }
    }
  }

}
