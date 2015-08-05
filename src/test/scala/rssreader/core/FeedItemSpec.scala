package rssreader.core

import java.time.LocalDateTime

import scala.xml.XML

import org.scalatest._


class FeedItemSpec extends FlatSpec with Matchers {
  behavior of "Parsing valid RSS"

  it should "parse fields correctly" in {
    val str =
      """
        |<item>
        |  <title>Item Title</title>
        |  <description>Lorem Ipsum</description>
        |  <link>Item Link</link>
        |  <guid>Item guid</guid>
        |  <author>FooBar</author>
        |  <comments>Comments src</comments>
        |  <pubDate>Thu, 1 Jan 2015 12:00:00 GMT</pubDate>
        |</item>
      """.stripMargin

    val result = FeedItem(str)
    result shouldBe 'good
    result.get should have(
      'title ("Item Title"),
      'description ("Lorem Ipsum"),
      'link ("Item Link"),
      'guid (Some("Item guid")),
      'author (Some("FooBar")),
      'comments (Some("Comments src")),
      'pubDate (Some(LocalDateTime.of(2015, 1, 1, 12, 0)))
    )
  }

  it should "parse from xml string" in {
    val str =
      """
        |<item>
        |  <title>Item Title</title>
        |  <description>Lorem Ipsum</description>
        |  <link>Item Link</link>
        |  <guid>Item guid</guid>
        |  <pubDate>Thu, 1 Jan 2015 12:00:00 GMT</pubDate>
        |</item>
      """.stripMargin

    FeedItem(str) shouldBe 'good
  }

  it should "parse from xml NodeSeq" in {
    val str =
      """
        |<item>
        |  <title>Item Title</title>
        |  <description>Lorem Ipsum</description>
        |  <link>Item Link</link>
        |  <guid>Item guid</guid>
        |  <pubDate>Thu, 1 Jan 2015 12:00:00 GMT</pubDate>
        |</item>
      """.stripMargin
    val node = XML.loadString(str)

    FeedItem(node) shouldBe 'good
  }

  behavior of "Parsing invalid RSS"
  it should "fail when empty title field" in {
    val str =
      """
        |<item>
        |  <description>lorem ipsum</description>
        |  <link>foobar</link>
        |</item>
      """.stripMargin

    FeedItem.parse(XML.loadString(str)) shouldBe 'bad
  }

  it should "fail when empty description field" in {
    val str =
      """
        |<item>
        |  <title>Title</title>
        |  <description></description>
        |  <link>Link</link>
        |</item>
      """.stripMargin

    FeedItem.parse(XML.loadString(str)) shouldBe 'bad
  }

  it should "fail when empty link field" in {
    val str =
      """
        |<item>
        |  <title>Title</title>
        |  <description>Description</description>
        |  <link></link>
        |</item>
      """.stripMargin

    FeedItem.parse(XML.loadString(str)) shouldBe 'bad
  }

  it should "accumulate errors when validating" in {
    val str =
      """
        |<item>
        |  <title></title>
        |  <description></description>
        |  <link></link>
        |</item>
      """.stripMargin

    val result = FeedItem.parse(XML.loadString(str))
    result.fold(
      good => fail("should not be good"),
      bad => bad should have size 3 // Missing title, link, description
    )
  }
}
