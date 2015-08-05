package rssreader.core

import java.net.URL
import java.time.LocalDateTime

import scala.xml._

import org.scalactic.Accumulation._
import org.scalactic._
import rssreader.core.XMLExtensions._

case class FeedItem(title: String,
                    link: String,
                    description: String,
                    author: Option[String],
                    comments: Option[String],
                    guid: Option[String],
                    pubDate: Option[LocalDateTime])


object FeedItem {

  def apply(u: URL): Or[FeedItem, Every[ErrorMessage]] = parse(XML.load(u))
  def apply(s: String): Or[FeedItem, Every[ErrorMessage]] = parse(XML.loadString(s))
  def apply(node: NodeSeq): Or[FeedItem, Every[ErrorMessage]] = parse(node)


  /**
   * Parse and validate a feed item from xml.
   * Errors will accumulate when required text is missing or the pubDate field not formatted correctly.
   */
  def parse(node: NodeSeq): Or[FeedItem, Every[ErrorMessage]] = {
    withGood(
      (node \ "title").requiredText("Missing item title"),
      (node \ "link").requiredText("Missing item link"),
      (node \ "description").requiredText("Missing item description"),
      Good((node \ "author").textOption),
      Good((node \ "comments").textOption),
      Good((node \ "guid").textOption),
      (node \ "pubDate").dateTimeOption
    )(FeedItem.apply)
  }


}
