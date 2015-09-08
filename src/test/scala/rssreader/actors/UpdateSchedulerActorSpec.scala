package rssreader.actors

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.Scheduler
import akka.testkit._

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import rssreader.service.FeedService
import rssreader.utils.tests.ActorTestKitSpec

class UpdateSchedulerActorSpec extends ActorTestKitSpec with MockitoSugar {

  val mongoCollection = "RSSReader-UpdateSchedulerActorSpec"


  behavior of "scheduling rss updates"
  it should "Schedule SynchronizeFeeds message at startup" in {
    import UpdateSchedulerActor._

    // Create our mock scheduler and create test actor
    val mockScheduler = mock[Scheduler]
    val actorRef = TestActorRef(new UpdateSchedulerActor{ override def scheduler = mockScheduler })
    val ec = actorRef.underlyingActor.context.dispatcher

    // Check if SynchronizeFeeds message was scheduled when actor was created
    verify(mockScheduler).scheduleOnce(1 second span, actorRef, SynchronizeFeeds)(ec, actorRef)
  }

  it should "Call FeedService::updateAllFeeds() when receiving SynchronizeFeeds message" in {
    import UpdateSchedulerActor._

    // Create our test actor with mock service
    val mockService = mock[FeedService]
    val actorRef = TestActorRef(new UpdateSchedulerActor{ override val service = mockService })
    val ec = actorRef.underlyingActor.context.dispatcher

    // Stub our update function in mocked service to prevent NPE when running
    when(mockService.updateAllFeeds()(ec)).thenReturn(Future.successful(true))

    // Send and check if correct action is performed when message is received
    actorRef ! SynchronizeFeeds
    verify(mockService).updateAllFeeds()(ec)
  }

}
