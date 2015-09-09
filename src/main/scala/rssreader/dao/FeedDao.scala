package rssreader.dao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{DB, ReadPreference}
import reactivemongo.bson.BSONObjectID
import reactivemongo.extensions.json.dao.JsonDao
import reactivemongo.extensions.json.dsl.JsonDsl._
import rssreader.core.{Feed, FeedItem}

class FeedDao(db: DB) extends JsonDao[Feed, BSONObjectID](db, "feeds") with LazyLogging {

  override def autoIndexes = Seq(
    Index(Seq("rssUrl" -> IndexType.Ascending), unique = true, background = true, sparse = true)
  )

  def exists(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Boolean] = count($id(id)).map(_ == 1)

  def exists(url: String)(implicit ec: ExecutionContext): Future[Boolean] = count("rssUrl" $eq url).map(_ == 1)

  def findId(url: String)(implicit ec: ExecutionContext): Future[Option[BSONObjectID]] = findOne("rssUrl" $eq url).map(_.map(_._id))

  def findAllIds()(implicit ec: ExecutionContext): Future[List[BSONObjectID]] = {
    collection.find(
      selector = $empty,
      projection = Json.obj("_id" -> 1))
    .cursor[BSONObjectID](ReadPreference.primary)
    .collect[List]()
  }

  def findByUrl(url: String)(implicit ec: ExecutionContext): Future[Option[Feed]] = findOne("rssUrl" $eq url)

  def findByTitle(title: String)(implicit ec: ExecutionContext): Future[Option[Feed]] = findOne("title" $eq title)

  def findByLink(link: String)(implicit ec: ExecutionContext): Future[Option[Feed]] = findOne("link" $eq link)

  def update(feed: Feed)(implicit ec: ExecutionContext) = updateById(feed._id, feed)

  def insertItems(id: BSONObjectID, items: Seq[FeedItem])(implicit ec: ExecutionContext): Future[UpdateWriteResult] = {
    collection.update(
      selector = $id(id),
      update = Json.obj("$addToSet" -> Json.obj("items" -> Json.obj("$each" -> Json.toJson(items))))
    )
  }

  def getItems(id: BSONObjectID)(implicit ec: ExecutionContext): Future[List[FeedItem]] =
    findById(id).map(_.map(_.items.toList).getOrElse(Nil))

  def getItems(id: BSONObjectID, sliceN: Int)(implicit ec: ExecutionContext): Future[List[FeedItem]] =
    getItemsQuery(id, Json.obj("items" -> Json.obj("$slice" -> sliceN)))

  def getItems(id: BSONObjectID, skipValue: Int, limitValue: Int)(implicit ec: ExecutionContext): Future[List[FeedItem]] =
    getItemsQuery(id, Json.obj("items" -> Json.obj("$slice" -> Json.arr(skipValue, limitValue))))

  private def getItemsQuery(id: BSONObjectID, projection: JsObject)(implicit ec: ExecutionContext): Future[List[FeedItem]] = {
    collection.find(
      selector = $id(id),
      projection = projection
    ).one[Feed].map(_.map(_.items.toList).getOrElse(Nil))
  }

}