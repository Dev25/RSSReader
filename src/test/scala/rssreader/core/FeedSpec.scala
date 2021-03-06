package rssreader.core

import java.time.LocalDateTime

import scala.io.Source

import rssreader.utils.tests.TestSpec

class FeedSpec extends TestSpec {

  behavior of "Parsing valid RSS"

  it should "correctly parse fields" in {
    val result = Feed.parse(fileExampleFeed)
    result shouldBe 'good
    result.get should have(
      'title ("Example Feed"),
      'link ("http://www.example.com"),
      'description ("The latest stories from ..."),
      'items (List(FeedItem("Item 1", "Link 1", "Description 1", None, None, None, None))),
      'pubDate (Some(LocalDateTime.of(2015, 7, 1, 15, 0))),
      'image (Some(FeedImage("http://example.com/foobar.png", "Title Here", "http://example.com/foo", None, None)))
    )
  }

  it should "ignore optional fields when empty" in {
    val result = Feed.parse(fileMinimalFeed)
    result shouldBe 'good
    result.get should have(
      'title ("Minimal Feed"),
      'link ("http://www.example.com"),
      'description ("The latest stories from ..."),
      'items (Nil),
      'pubDate (None),
      'image (None)
    )
  }

  it should "create from url pointing to xml" in {
    Feed.parse(fileExampleFeed) shouldBe 'good
  }

  it should "create from xml string" in {
    val str = Source.fromURL(fileExampleFeed).mkString
    Feed.parse(str) shouldBe 'good
  }

  it should "parse all child items" in {
    val result = Feed.parse(fileMultipleItemFeed)
    result.get.items should have size 2
  }

  behavior of "Parsing invalid RSS"
  it should "fail when empty title field" in {
    val str =
      """
        |<rss>
        |  <channel>
        |    <title></title>
        |    <link>http://www.example.com</link>
        |    <description>The latest stories from ...</description>
        |  </channel>
        |</rss>
      """.stripMargin

    Feed.parse(str) shouldBe 'bad
  }

  it should "fail when empty link field" in {
    val str =
      """
        |<rss>
        |  <channel>
        |    <title>Example Feed</title>
        |    <link></link>
        |    <description>The latest stories from ...</description>
        |  </channel>
        |</rss>
      """.stripMargin

    Feed.parse(str) shouldBe 'bad
  }

  it should "fail when empty description field" in {
    val str =
      """
        |<rss>
        |  <channel>
        |    <title>Example Feed</title>
        |    <link>http://www.example.com</link>
        |    <description></description>
        |  </channel>
        |</rss>
      """.stripMargin

    Feed.parse(str) shouldBe 'bad
  }

  it should "fail when an item cannot be parsed" in {
    val str =
      """
        |<rss>
        |  <channel>
        |    <title>Example Feed</title>
        |    <link>http://www.example.com</link>
        |    <description>The latest stories from ...</description>
        |   <item>
        |     <title>Item with no link or description</title>
        |   </item>
        |  </channel>
        |</rss>
      """.stripMargin

    val result = Feed.parse(str)
    result shouldBe 'bad
    result.fold(
      good => fail(),
      bad => bad should have size 2
    )
  }

  it should "accumulate errors when validating" in {
    val str =
      """
        |<rss>
        |  <channel>
        |    <title></title>
        |    <link></link>
        |    <description></description>
        |  </channel>
        |</rss>
      """.stripMargin

    val result = Feed.parse(str)
    result shouldBe 'bad
    result.fold(
      good => fail(),
      bad => bad should have size 3
    )
  }


}
