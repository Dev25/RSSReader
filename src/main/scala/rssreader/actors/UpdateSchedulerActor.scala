package rssreader.actors

import scala.concurrent.duration._

import akka.actor.{ActorLogging, Actor}
import rssreader.service.FeedService

object UpdateSchedulerActor {
  case object SynchronizeFeeds
}

class UpdateSchedulerActor extends Actor with ActorLogging {
  import UpdateSchedulerActor._
  import context._

  val service = new FeedService

  def scheduler = context.system.scheduler

  override def preStart() = {
    log.info("Starting UpdateScheduler")
    scheduler.scheduleOnce(1 second span, self, SynchronizeFeeds) // Send the initial message to start update loop
  }

  // override postRestart so we don't call preStart and schedule a new message again
  override def postRestart(reason: Throwable) = {}

  def receive = {
    case SynchronizeFeeds =>
      log.info("Synchronizing feeds...")
      scheduler.scheduleOnce(30 minutes span, self, SynchronizeFeeds) // Send another message after a period of time
      service.updateAllFeeds().onSuccess {
        case true => log.info("Updated all feeds")
        case _ => log.warning("Failed to update all feeds")
      }
    }
}