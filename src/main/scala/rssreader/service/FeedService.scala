package rssreader.service

import java.net.URL

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.typesafe.scalalogging.LazyLogging
import org.scalactic.{Every, Or, ErrorMessage}
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.bson.BSONObjectID
import rssreader.core.Feed
import rssreader.dao.FeedDao
import rssreader.utils.MongoContext

object FeedService extends FeedService

class FeedService extends LazyLogging {
  val dao = new FeedDao(MongoContext.db)

  def count(): Future[Int] = dao.count()

  def exists(id: BSONObjectID): Future[Boolean] = dao.exists(id)
  def exists(url: String): Future[Boolean] = dao.exists(url)

  def findAll(): Future[List[Feed]] = dao.findAll()
  def findById(id: BSONObjectID): Future[Option[Feed]] = dao.findById(id)
  def findByUrl(url: String): Future[Option[Feed]] = dao.findByUrl(url)
  def findByTitle(title: String): Future[Option[Feed]] = dao.findByTitle(title)

  /**
   * Parse and save a new RSS feed
   * @param url Feed url
   * @return Future containing parsed Feed as Option or None if parsing failed
   */
  def saveFeed(url: URL): Future[Option[Feed]] = {
    //logger.info(s"Saving a new feed:$url")
    Feed.parse(url).fold(
      feed => {
        val future = dao.save(feed)
        future.onFailure { case error => logger.error(s"Failed to save feed:$error") }
        future.map(_ => Some(feed))
      },
      errors => {
        logger.error(s"Failed to parse a new feed:$url\n$errors")
        Future.successful(None)
      }
    )
  }

  /**
   * Update an existing feed by fetching latest metadata + items
   * @param id Feed id
   * @return true if update succeeded or false if it failed, wrapped in a Future
   */
  def updateFeed(id: BSONObjectID): Future[Boolean] = {
    //logger.info(s"Updating feed:$id")
    dao.findById(id).flatMap {
      case Some(feed) =>
          Feed.parse(new URL(feed.rssUrl), feed._id).map { updated =>

            // Remove items key from json, we don't want to overwrite that array
            val jsonTransformer = (__ \ 'items).json.prune
            val updatedJs = Json.toJson(updated).transform(jsonTransformer).get

            // Run the 2 futures, wait and return a boolean indicating if both operations succeeded
            val f1 = dao.updateById(id, Json.obj("$set" -> updatedJs))
            val f2 = dao.insertItems(id, updated.items)
            for {
              r1 <- f1
              r2 <- f2
            } yield r1.ok && r2.ok
          }.getOrElse(Future.successful(false))

      case None => Future.successful(false)
    }
  }

  /**
   * Delete a feed
   * @param id Feed id
   * @return true if feed deleted otherwise false
   */
  def deleteFeed(id: BSONObjectID): Future[Boolean] = {
    //logger.info(s"Deleting Feed: $id")
    dao.removeById(id).map(_.n == 1)
  }

}

