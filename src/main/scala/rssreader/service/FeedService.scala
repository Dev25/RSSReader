package rssreader.service

import java.net.URL

import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.bson.BSONObjectID
import rssreader.core.Feed
import rssreader.dao.FeedDao
import rssreader.utils.MongoContext

class FeedService extends LazyLogging {
  val dao = new FeedDao(MongoContext.db)

  def count()(implicit ec: ExecutionContext): Future[Int] = dao.count()

  def exists(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Boolean] = dao.exists(id)
  def exists(url: String)(implicit ec: ExecutionContext): Future[Boolean] = dao.exists(url)

  def findAll()(implicit ec: ExecutionContext): Future[List[Feed]] = dao.findAll()
  def findAllIds()(implicit ec: ExecutionContext): Future[List[BSONObjectID]] = dao.findAllIds()
  def findById(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Option[Feed]] = dao.findById(id)
  def findByUrl(url: String)(implicit ec: ExecutionContext): Future[Option[Feed]] = dao.findByUrl(url)
  def findByTitle(title: String)(implicit ec: ExecutionContext): Future[Option[Feed]] = dao.findByTitle(title)

  /**
   * Parse and save a new RSS feed
   * @param url Feed url
   * @return Future containing parsed Feed as Option or None if parsing failed
   */
  def saveFeed(url: URL)(implicit ec: ExecutionContext): Future[Option[Feed]] = {
    logger.debug(s"Saving a new feed:$url")
    Feed.parse(url).fold(
      feed => {
        val future = dao.save(feed)
        future.onFailure { case error => logger.error(s"Failed to save feed:$error") }
        future.map(_ => Some(feed))
      },
      errors => {
        logger.warn(s"Failed to parse a new feed [$url,$errors]")
        Future.successful(None)
      }
    )
  }

  /**
   * Update an existing feed by fetching latest metadata + items
   * @param id Feed id
   * @return true if update succeeded or false if it failed, wrapped in a Future
   */
  def updateFeed(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.debug(s"Attempting to update feed:$id")
    dao.findById(id).flatMap {
      case Some(feed) =>
        logger.debug(s"Updating Feed[ID:$id,Source:${feed.rssUrl}]")
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
   * Update all existing feeds
   * @return true if all feeds were updated successfully otherwise false
   */
  def updateAllFeeds()(implicit ec: ExecutionContext): Future[Boolean]  = {
    logger.debug("Updating all feeds...")
    dao.findAllIds().flatMap(ids => Future.sequence(ids.map(updateFeed)).map(_.forall(_ == true)))
  }

  /**
   * Delete a feed
   * @param id Feed id
   * @return true if feed deleted otherwise false
   */
  def deleteFeed(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.debug(s"Deleting Feed: $id")
    dao.removeById(id).map(_.n == 1)
  }

}

