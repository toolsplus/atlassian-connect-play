package io.toolsplus.atlassian.connect.play.generators

import org.scalacheck.Gen
import org.scalacheck.Gen._

trait UrlGen {

  def urlGen: Gen[String] = for {
  subdomain <- alphaStr.suchThat(s => s.nonEmpty)
  domain <- alphaStr.suchThat(s => s.nonEmpty)
  tld <- oneOf("com", "net", "org", "io")
  } yield s"https://${subdomain}.${domain}.${tld}"

}
