package rssreader.core

import java.time.LocalDateTime

import scala.xml._

import org.scalactic.Accumulation._
import org.scalactic._
import play.api.libs.json.Json
import rssreader.utils.XMLExtensions
import rssreader.utils.XMLExtensions._

case class FeedItem(title: String,
                    link: String,
                    description: String,
                    author: Option[String],
                    comments: Option[String],
                    guid: Option[String],
                    pubDate: Option[LocalDateTime])


object FeedItem {
  implicit val itemFmt = Json.format[FeedItem]

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
