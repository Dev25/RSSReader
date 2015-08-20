package rssreader.dao

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest._
import reactivemongo.api.commands.WriteResult
import rssreader.core.{FutureHelper, FeedItem, Feed}

class FeedDaoSpec extends FlatSpec with Matchers with BeforeAndAfter with FutureHelper {
  val db = MongoContext.connection("RSSReader-test")
  val dao = new FeedDao(db)

  // Shared fixture for when tests need to insert a feed via the dao
  trait InsertedFeed {
    // Default feed for test usage, override if another feed is required
    val feed = Feed.parse(getClass.getResource("/exampleFeed.xml")).get

    // Run the callback function once the feed is inserted to the db
    def whenInserted[U](callback: (WriteResult) => U) = {
      val future = dao.save(feed)
      whenReady(future)(callback)
    }
  }

  after {
    dao.drop() // Wipe db clean after each test
  }

  behavior of "Inserting feeds"
  it should "insert single feed" in new InsertedFeed {
    whenInserted { result =>
      result.ok shouldBe true
      dao.count().futureValue shouldBe 1
    }
  }

  behavior of "Querying feeds"
  it should "query feed id by rss url" in new InsertedFeed {
    whenInserted { _ =>
      dao.findId(feed.rssUrl.get).futureValue.get shouldBe feed._id
    }
  }

  it should "query feed by title" in new InsertedFeed {
    whenInserted { _ =>
      dao.findByTitle(feed.title).futureValue shouldBe Some(feed)
    }
  }

  it should "query feed by rss url"  in new InsertedFeed {
    whenInserted { _ =>
      dao.findByUrl(feed.rssUrl.get).futureValue shouldBe Some(feed)
    }
  }

  it should "query feed by link" in new InsertedFeed {
    whenInserted { _ =>
      dao.findByLink(feed.link).futureValue shouldBe Some(feed)
    }
  }

  behavior of "Updating feeds"

  it should "update existing feed" in new InsertedFeed {
    whenInserted { _ =>
      val updatedFeed = feed.copy(title = "Title changed!")
      whenReady(dao.update(updatedFeed)) {_ =>
        val dbFeed = dao.findById(feed._id).futureValue.get
        dbFeed shouldNot equal (feed)
        dbFeed should equal (updatedFeed)
      }
    }
  }


  behavior of "Deleting feeds"

  it should "delete feed" in new InsertedFeed {
    whenInserted { _ =>
      val f = dao.removeById(feed._id)
      whenReady(f) { _ =>
        dao.findById(feed._id).futureValue shouldBe None
      }
    }
  }


  behavior of "Inserting new feed items"
  it should "insert new feed item" in new InsertedFeed {
    whenInserted { _ =>
      val newItem = FeedItem("title", "link", "description", None, None, None, None)
      val f = dao.insertItems(feed._id, newItem :: Nil)
      whenReady(f) { result =>
        result.nModified shouldBe 1 // Single row added to db
        dao.findById(feed._id).futureValue.get.items should contain (newItem)
      }
    }
  }

  it should "ignore any existing feed items already present" in new InsertedFeed {
    whenInserted { result =>
      val existingItem = feed.items.head
      val f = dao.insertItems(feed._id, existingItem :: Nil)
      f.futureValue.nModified shouldBe 0 // No rows altered
    }
  }

  behavior of "Querying feed items"
  it should "return Nil if no items" in new InsertedFeed {
    override val feed = Feed.parse(getClass.getResource("/minimalFeed.xml")).get
    whenInserted { _ =>
      dao.getItems(feed._id).futureValue should have size 0
    }
  }

  it should "return all items" in new InsertedFeed {
    override val feed = Feed.parse(getClass.getResource("/multipleItemFeed.xml")).get
    whenInserted { _ =>
      dao.getItems(feed._id).futureValue should have size 2
    }
  }

  it should "return n newest items" in new InsertedFeed {
    override val feed = Feed.parse(getClass.getResource("/multipleItemFeed.xml")).get
    whenInserted { _ =>
      dao.getItems(feed._id, 1).futureValue should have size 1
    }
  }


  it should "able to paginate items using skip/limit" in new InsertedFeed {
    override val feed = Feed.parse(getClass.getResource("/10ItemFeed.xml")).get
    whenInserted { _ =>
      val dbItems = dao.getItems(feed._id, 2, 3).futureValue
      dbItems should have size 3
      dbItems should contain theSameElementsAs feed.items.slice(2,5)
    }
  }

}
