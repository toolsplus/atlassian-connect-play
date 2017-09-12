package io.toolsplus.atlassian.connect.play

import io.toolsplus.atlassian.connect.play.generators.http.HttpGen
import io.toolsplus.atlassian.connect.play.generators._
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

trait TestSpec
    extends PlaySpec
    with MockFactory
    with GeneratorDrivenPropertyChecks
    with FutureAwaits
    with DefaultAwaitTimeout
    with HttpGen
    with LifecycleEventGen
    with AtlassianHostGen
    with AtlassianConnectJwtGen
    with PlayRequestGen
    with PlayResultGen
