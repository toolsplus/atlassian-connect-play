package io.toolsplus.atlassian.connect.play.helper.jwt

import io.toolsplus.atlassian.connect.play.TestSpec
import io.toolsplus.atlassian.connect.play.auth.jwt.SelfAuthenticationTokenGenerator
import io.toolsplus.atlassian.connect.play.models.{
  PlayAddonProperties,
  AtlassianConnectProperties
}
import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class JwtResponseHelperSpec
    extends TestSpec
    with GuiceOneAppPerSuite
    with EitherValues {

  val config = app.configuration

  val addonProperties = new PlayAddonProperties(config)
  val connectProperties = new AtlassianConnectProperties(config)

  val tokenGenerator =
    new SelfAuthenticationTokenGenerator(addonProperties, connectProperties)

  trait ConfiguredJwtResponseHelper {
    val helper = new JwtResponseHelper {
      override def selfAuthenticationTokenGenerator
        : SelfAuthenticationTokenGenerator = tokenGenerator
    }
  }

  "A JwtResponseHelper" when {

    "when given a Play result" should {

      "set default JWT response header to result" in new ConfiguredJwtResponseHelper {
        forAll(playResultGen, atlassianHostUserGen) { (result, hostUser) =>
          implicit val atlassianHostUser = hostUser
          val signedResult = helper.withJwtResponseHeader(result)
          signedResult.right.value.header.headers
            .get(helper.jwtResponseHeaderName) mustBe defined
        }
      }
    }

  }

}
