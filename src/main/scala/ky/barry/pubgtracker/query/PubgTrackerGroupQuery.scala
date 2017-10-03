package ky.barry.pubgtracker.query

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import ky.barry.pubgtracker.group.PubgTrackerGroup
import ky.barry.pubgtracker.user.PubgTrackerUser

import scala.concurrent.duration.FiniteDuration

object PubgTrackerGroupQuery {
  case object CollectionTimeout

  def props(
             actorToUserId: Map[ActorRef, String],
             requestId: Long,
             requester: ActorRef,
             timeout: FiniteDuration): Props = {
    Props(new PubgTrackerGroupQuery(actorToUserId, requestId, requester, timeout))
  }
}

class PubgTrackerGroupQuery(
                             actorToUserId: Map[ActorRef, String],
                        requestId: Long,
                        requester: ActorRef,
                        timeout: FiniteDuration) extends Actor with ActorLogging {
  import PubgTrackerGroupQuery._
  import context.dispatcher
  val queryTimeoutTimer = context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)

  override def preStart(): Unit = {
    actorToUserId.keysIterator.foreach { userActor =>
      context.watch(userActor)
      userActor ! PubgTrackerUser.ReadStats(0)
    }
  }

  override def postStop(): Unit = {
    queryTimeoutTimer.cancel()
  }

  override def receive: Receive =
    waitingForReplies(
      Map.empty,
      actorToUserId.keySet)

  def waitingForReplies(
                         repliesSoFar: Map[String, PubgTrackerGroup.StatsReading],
                         stillWaiting: Set[ActorRef]): Receive = {
    case PubgTrackerUser.RespondStats(0, valueOption) =>
      val userActor = sender()
      val reading = valueOption match {
        case Some(value) => PubgTrackerGroup.Stats(value)
        case None => PubgTrackerGroup.UserNotAvailable
      }
      receivedResponse(userActor, reading, stillWaiting, repliesSoFar)

    case Terminated(userActor) =>
      receivedResponse(userActor, PubgTrackerGroup.UserNotAvailable, stillWaiting, repliesSoFar)

    case CollectionTimeout =>
      val timedOutReplies =
        stillWaiting.map { userActor =>
          val userId = actorToUserId(userActor)
          userId -> PubgTrackerGroup.UserTimedOut
        }
      requester ! PubgTrackerGroup.RespondAllStats(requestId, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }

  def receivedResponse(
                        userActor: ActorRef,
                        reading: PubgTrackerGroup.StatsReading,
                        stillWaiting: Set[ActorRef],
                        repliesSoFar: Map[String, PubgTrackerGroup.StatsReading]): Unit = {
    context.unwatch(userActor)
    val userId = actorToUserId(userActor)
    val newStillWaiting = stillWaiting - userActor

    val newRepliesSoFar = repliesSoFar + (userId -> reading)
    if (newStillWaiting.isEmpty) {
      requester ! PubgTrackerGroup.RespondAllStats(requestId, newRepliesSoFar)
      context.stop(self)
    } else {
      context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
    }
  }

}

