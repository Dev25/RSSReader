package rssreader.core

import java.net.URL
import java.time.LocalDateTime

import scala.xml.{NodeSeq, XML}

import org.scalactic.Accumulation._
import org.scalactic._
import rssreader.core.XMLExtensions._

case class Feed(title: String,
                link: String,
                description: String,
                items: Seq[FeedItem],
                language: Option[String],
                pubDate: Option[LocalDateTime],
                image: Option[FeedImage])

object Feed {

  def apply(u: URL): Or[Feed, Every[ErrorMessage]] = parse(XML.load(u))
  def apply(s: String): Or[Feed, Every[ErrorMessage]] = parse(XML.loadString(s))

  /**
   * Parse and validate a RSS feed from a xml node, either returning the created Feed
   * or a list of error messages (required text or malformed text) including error messages
   * for when parsing feed items.
   */
  def parse(root: NodeSeq): Or[Feed, Every[ErrorMessage]] = {
    val node = root \ "channel"
    withGood(
      (node \ "title").requiredText("Missing feed title"),
      (node \ "link").requiredText("Missing feed link"),
      (node \ "description").requiredText("Missing feed description"),
      (node \\ "item").map(FeedItem.parse).combined, // Seq[Or[F,E]] -> Or[Seq[F], E]
      Good((node \ "language").textOption),
      (node \ "pubDate").dateTimeOption,
      (node \ "image").headOption.map(FeedImage.parse).fold // Option[F] -> Or[Option[F], E]
        (Good(None): Or[Option[FeedImage], Every[ErrorMessage]]) // Return None
        (result => result.map(Some.apply)) // Convert to Some()
    )(Feed.apply)
  }

}
