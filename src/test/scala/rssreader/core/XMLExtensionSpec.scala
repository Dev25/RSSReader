package rssreader.core

import java.time.LocalDateTime

import scala.xml.XML

import org.scalactic.One
import org.scalatest._
import rssreader.core.XMLExtensions._

class XMLExtensionSpec extends FlatSpec with Matchers {

  behavior of "Parsing node as Option"
  it should "return None when empty" in {
    val xml = XML.loadString("<bar></bar>")
    val result = xml.textOption
    result shouldBe 'empty
  }

  it should "return Some(text) when non empty" in {
    val xml = XML.loadString("<bar>Huzzah</bar>")
    val result = xml.textOption
    result shouldBe 'defined
    result shouldBe Some("Huzzah")
  }

  behavior of "Parsing non empty node"
  it should "return error message when empty" in {
    val xml = XML.loadString("<bar></bar>")
    val result = xml.requiredText("Must have bar")
    result shouldBe 'bad
    result.fold(
      good => fail(),
      bad => bad shouldBe One("Must have bar")
    )
  }

  it should "return parsed text when non empty" in {
    val xml = XML.loadString("<bar>Huzzah</bar>")
    val result = xml.requiredText("Must have bar")
    result shouldBe 'good
    result.get shouldBe "Huzzah"
  }

  behavior of "Parsing node as Option[Int]"
  it should "return None when empty node" in {
    val str = "<number></number>"
    val result = XML.loadString(str).asIntOpt
    result shouldBe 'good
    result.get shouldBe 'empty
  }

  it should "return parsed text as Some(Int)" in {
    val str = "<number>1</number>"
    val result = XML.loadString(str).asIntOpt
    result shouldBe 'good
    result.get shouldBe Some(1)
  }

  it should "return error when text cannot be parsed as Int" in {
    val str = "<number>one</number>"
    val result = XML.loadString(str).asIntOpt
    result shouldBe 'bad
    result.fold(
      good => fail(),
      bad => bad shouldBe One("Cannot parse string to Int")
    )
  }


  behavior of "Parsing node as LocalDateTime using RFC822"
  it should "return wellformed parsed text as LocalDateTime" in {
    val str = "<date>Thu, 1 Jan 2015 12:00:00 GMT</date>"
    val result = XML.loadString(str).dateTimeOption
    result shouldBe 'good
    result.get shouldBe Some(LocalDateTime.of(2015, 1, 1, 12, 0))
  }

  it should "return malformed parsed text as failure" in {
    val str = "<date>1 Janurary 2015 12pm GMT</date>"
    val result = XML.loadString(str).dateTimeOption
    result shouldBe 'bad
  }

  it should "return None when node empty" in {
    val str = "<date></date>"
    val result = XML.loadString(str).dateTimeOption
    result shouldBe 'good
    result.get shouldBe 'empty
  }


}

