package io.toolsplus.atlassian.connect.play.generators.http

import org.scalacheck.Gen
import org.scalacheck.Gen.{alphaNumStr, choose, chooseNum, listOfN, oneOf}

trait HttpGen {

  def statusGen: Gen[Int] = choose(100, 599)

  def methodGen: Gen[String] = oneOf("GET", "PUT", "POST", "DELETE", "PATCH")

  def rootRelativePathGen: Gen[String] = for {
    n <- chooseNum(0, 5)
    segments <- listOfN(n, alphaNumStr.suchThat(_.nonEmpty))
  } yield s"/${segments.mkString("/")}"

  def rootRelativePathWithQueryGen: Gen[String] = for {
    uri <- rootRelativePathGen
    query <- queryStringGen
  } yield s"$uri?$query"

  def queryParam: Gen[(String, List[String])] =
    for {
      name <- alphaNumStr.suchThat(_.nonEmpty)
      n <- choose(1, 3)
      values <- listOfN(n, alphaNumStr.suchThat(_.nonEmpty))
    } yield (name, values)

  def queryParams: Gen[Map[String, List[String]]] =
    for {
      count <- choose(1, 3)
      params <- listOfN(count, queryParam)
    } yield
      params.foldLeft(Map(): Map[String, List[String]]) { (map, param) =>
        map + param
      }

  def queryStringGen: Gen[String] =
    for {
      query <- queryParams
    } yield toQueryString(query)

  def toQueryString(params: Map[String, Seq[String]]): String =
    params
      .map {
        case (key: String, values: Seq[String]) =>
          s"$key=${values.mkString(",")}"
      }
      .mkString("&")

}
