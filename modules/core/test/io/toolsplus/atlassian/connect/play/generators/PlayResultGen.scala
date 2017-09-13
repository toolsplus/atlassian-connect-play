package io.toolsplus.atlassian.connect.play.generators

import io.toolsplus.atlassian.connect.play.generators.http.HttpGen
import org.scalacheck.Gen
import play.api.http.HttpEntity
import play.api.mvc.{ResponseHeader, Result}

trait PlayResultGen extends HttpGen {

  def playResultGen: Gen[Result] = for {
    status <- statusGen
  } yield Result(header = ResponseHeader(status), body = HttpEntity.NoEntity)

}
