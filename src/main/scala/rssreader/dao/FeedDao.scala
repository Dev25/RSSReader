package rssreader.dao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.DB
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import reactivemongo.extensions.json.dao.JsonDao
import reactivemongo.extensions.json.dsl.JsonDsl._
import rssreader.core.{Feed, FeedItem}

class FeedDao(db: DB) extends JsonDao[Feed, BSONObjectID](db, "feeds") with LazyLogging {

  override def autoIndexes = Seq(
    Index(Seq("url" -> IndexType.Ascending), unique = true, background = true, sparse = true)
  )

  def count(): Future[Int] = count($empty)

  def findId(url: String): Future[Option[BSONObjectID]] = findOne("rssUrl" $eq url).map(_.map(_._id))

  def findByUrl(url: String): Future[Option[Feed]] = findOne("rssUrl" $eq url)

  def findByTitle(title: String): Future[Option[Feed]] = findOne("title" $eq title)

  def findByLink(link: String): Future[Option[Feed]] = findOne("link" $eq link)

  def update(feed: Feed) = updateById(feed._id, feed)

  def insertItems(id: BSONObjectID, items: Seq[FeedItem]) = {
    collection.update(
      selector = $id(id),
      update = Json.obj("$addToSet" -> Json.obj("items" -> Json.obj("$each" -> Json.toJson(items))))
    )
  }

  def getItems(id: BSONObjectID): Future[List[FeedItem]] =
    findById(id).map(_.map(_.items.toList).getOrElse(Nil))

  def getItems(id: BSONObjectID, sliceN: Int): Future[List[FeedItem]] =
    getItemsQuery(id, Json.obj("items" -> Json.obj("$slice" -> sliceN)))

  def getItems(id: BSONObjectID, skipValue: Int, limitValue: Int): Future[List[FeedItem]] =
    getItemsQuery(id, Json.obj("items" -> Json.obj("$slice" -> Json.arr(skipValue, limitValue))))

  private def getItemsQuery(id: BSONObjectID, projection: JsObject): Future[List[FeedItem]] = {
    collection.find(
      selector = $id(id),
      projection = projection
    ).one[Feed].map(_.map(_.items.toList).getOrElse(Nil))
  }

}