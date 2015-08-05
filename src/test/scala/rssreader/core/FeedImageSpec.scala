package rssreader.core

import scala.xml.XML

import org.scalatest._

class FeedImageSpec extends FlatSpec with Matchers {


  behavior of "Parsing RSS feed image"
  it should "parse from valid xml" in {
    val str =
      """
        |<image>
        |  <url>http://example.com/foobar.png</url>
        |  <title>Title Here</title>
        |  <link>http://example.com/foo</link>
        |  <width>120</width>
        |  <height>60</height>
        |</image>
      """.stripMargin

    val result = FeedImage.parse(XML.loadString(str))
    result shouldBe 'good
    result.get should have(
      'url ("http://example.com/foobar.png"),
      'title ("Title Here"),
      'link ("http://example.com/foo"),
      'width (Some(120)),
      'height (Some(60))
    )

  }

  it should "set optional fields as None if empty" in {
    val str =
      """
        |<image>
        |  <url>http://example.com/foobar.png</url>
        |  <title>Title Here</title>
        |  <link>http://example.com/foo</link>
        |</image>
      """.stripMargin

    val result = FeedImage.parse(XML.loadString(str))
    result shouldBe 'good
    result.get should have(
      'width (None),
      'height (None)
    )
  }

  it should "fail when empty url field" in {
    val str =
      """
        |<image>
        |  <title>BBC News - Home</title>
        |  <link>http://example.com/foo</link>
        |</image>
      """.stripMargin

    FeedImage.parse(XML.loadString(str)) shouldBe 'bad
  }


  it should "fail when empty title field" in {
    val str =
      """
        |<image>
        |  <url>http://example.com/foobar.png</url>
        |  <link>http://example.com/foo</link>
        |</image>
      """.stripMargin

    FeedImage.parse(XML.loadString(str)) shouldBe 'bad
  }


  it should "fail when empty link field" in {
    val str =
      """
        |<image>
        |  <url>http://example.com/foobar.png</url>
        |  <title>BBC News - Home</title>
        |</image>
      """.stripMargin

    FeedImage.parse(XML.loadString(str)) shouldBe 'bad
  }



  it should "accumulate errors when validating" in {
    val str =
      """
        |<image>
        |  <width>120</width>
        |</image>
      """.stripMargin

    FeedImage.parse(XML.loadString(str)).fold(
      good => fail("should not be good"),
      bad => bad should have size 3 // Missing url, title, link
    )

  }

}
