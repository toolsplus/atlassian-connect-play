package io.toolsplus.atlassian.connect.play.generators

import io.toolsplus.atlassian.connect.play.generators.http.HttpGen
import org.scalacheck.Gen
import org.scalacheck.Gen._
import play.api.test.FakeRequest

trait PlayRequestGen extends HttpGen {

  def playRequestGen: Gen[FakeRequest[_]] =
    for {
      method <- methodGen
      query <- queryStringGen
      n <- chooseNum(1, 5)
      uri <- listOfN(n, alphaNumStr.suchThat(_.nonEmpty))
        .map(_.mkString("/"))
    } yield FakeRequest(method, s"$uri?$query")

  def playRequestGen(
      queryParams: Map[String, Seq[String]]): Gen[FakeRequest[_]] = {
    val additionalQuery = toQueryString(queryParams)
    for {
      method <- methodGen
      query <- queryStringGen
      n <- chooseNum(1, 5)
      uri <- listOfN(n, alphaNumStr.suchThat(_.nonEmpty))
        .map(_.mkString("/"))
    } yield FakeRequest(method, s"$uri?$query&$additionalQuery")
  }

}
