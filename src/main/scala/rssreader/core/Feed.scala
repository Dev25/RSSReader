package rssreader.core

import java.net.URL
import java.time.LocalDateTime

import scala.xml.{NodeSeq, XML}

import org.scalactic.Accumulation._
import org.scalactic._
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import rssreader.utils.XMLExtensions
import XMLExtensions._
import play.modules.reactivemongo.json.ImplicitBSONHandlers.BSONObjectIDFormat

case class Feed(_id: BSONObjectID,
                rssUrl: String,
                title: String,
                link: String,
                description: String,
                items: Seq[FeedItem],
                language: Option[String],
                pubDate: Option[LocalDateTime],
                image: Option[FeedImage])

object Feed {
  implicit val fmt = Json.format[Feed]

  /** Download and parse RSS feed from a url and set the _id */
  def parse(u: URL, id: BSONObjectID): Or[Feed, Every[ErrorMessage]] = parse(u.toString, Some(id), XML.load(u))

  /** Download and parse RSS feed from a url */
  def parse(u: URL): Or[Feed, Every[ErrorMessage]] = parse(u.toString, None, XML.load(u))

  /** Download and parse RSS feed from a xml string */
  def parse(s: String): Or[Feed, Every[ErrorMessage]] = parse("", None, XML.loadString(s))

  /**
   * Parse and validate a RSS feed from a xml node, either returning the parsed Feed
   * or a list of error messages (missing required text or malformed text) including error messages
   * for when parsing feed items.
   */
  def parse(url: String, id: Option[BSONObjectID], root: NodeSeq): Or[Feed, Every[ErrorMessage]] = {
    val node = root \ "channel"
    withGood(
      Good(id.getOrElse(BSONObjectID.generate)),
      Good(url),
      (node \ "title").requiredText("Missing feed title"),
      (node \ "link").requiredText("Missing feed link"),
      (node \ "description").requiredText("Missing feed description"),
      (node \\ "item").map(FeedItem.parse).combined, // Seq[Or[F,E]] => Or[Seq[F], E]
      Good((node \ "language").textOption),
      (node \ "pubDate").dateTimeOption,
      (node \ "image").headOption.map(FeedImage.parse).fold // Option[F] => Or[Option[F], E]
        (Good(None): Or[Option[FeedImage], Every[ErrorMessage]]) // Return None
        (result => result.map(Some.apply)) // Convert to Some()
    )(Feed.apply)
  }
}
