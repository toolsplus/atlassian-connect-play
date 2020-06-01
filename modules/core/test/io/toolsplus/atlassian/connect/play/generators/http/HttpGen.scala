package io.toolsplus.atlassian.connect.play.generators.http

import org.scalacheck.Gen
import org.scalacheck.Gen.{alphaNumStr, choose, listOf, listOfN, oneOf}

trait HttpGen {

  def statusGen: Gen[Int] = choose(100, 599)

  def methodGen: Gen[String] = oneOf("GET", "PUT", "POST", "DELETE", "PATCH")

  def rootRelativePathGen: Gen[String] =
    listOf(alphaNumStr.suchThat(!_.isEmpty)).map(s => s"/${s.mkString("/")}")

  def rootRelativePathWithQueryGen: Gen[String] = for {
    uri <- rootRelativePathGen
    query <- queryStringGen
  } yield s"$uri?$query"

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

  def queryStringGen: Gen[String] =
    for {
      query <- queryParams
    } yield toQueryString(query)


  def toQueryString(params: Map[String, Seq[String]]) =
    params
      .map {
        case (key: String, values: Seq[String]) =>
          s"$key=${values.mkString(",")}"
      }
      .mkString("&")

}
