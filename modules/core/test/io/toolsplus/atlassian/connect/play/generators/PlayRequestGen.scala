package io.toolsplus.atlassian.connect.play.generators

import io.toolsplus.atlassian.connect.play.generators.PlayRequestGen.Method
import org.scalacheck.Gen
import org.scalacheck.Gen._
import play.api.test.FakeRequest

trait PlayRequestGen {

  def methodGen: Gen[Method] = oneOf("GET", "PUT", "POST", "DELETE", "PATCH")

  def queryParam: Gen[(String, List[String])] =
    for {
      name <- alphaNumStr.suchThat(!_.isEmpty)
      count <- choose(1, 5)
      values <- listOfN(count, alphaNumStr.suchThat(!_.isEmpty))
    } yield (name, values)

  def queryParams: Gen[Map[String, List[String]]] =
    for {
      count <- choose(1, 10)
      params <- listOfN(count, queryParam)
    } yield
      params.foldLeft(Map(): Map[String, List[String]]) { (map, param) =>
        map + param
      }

  def queryString: Gen[String] =
    for {
      query <- queryParams
    } yield toQueryString(query)

  def playRequestGen: Gen[FakeRequest[_]] =
    for {
      method <- methodGen
      query <- queryString
      uri <- listOf(alphaNumStr.suchThat(!_.isEmpty))
        .map(_.mkString("/"))
    } yield FakeRequest(method, s"$uri?$query")

  def playRequestGen(
      queryParams: Map[String, Seq[String]]): Gen[FakeRequest[_]] = {
    val additionalQuery = toQueryString(queryParams)
    for {
      method <- methodGen
      query <- queryString
      uri <- listOf(alphaNumStr.suchThat(!_.isEmpty))
        .map(_.mkString("/"))
    } yield FakeRequest(method, s"$uri?$query&$additionalQuery")
  }

  private def toQueryString(params: Map[String, Seq[String]]) =
    params
      .map {
        case (key: String, values: List[String]) =>
          s"$key=${values.mkString(",")}"
      }
      .mkString("&")

}

object PlayRequestGen {

  type Method = String

}
