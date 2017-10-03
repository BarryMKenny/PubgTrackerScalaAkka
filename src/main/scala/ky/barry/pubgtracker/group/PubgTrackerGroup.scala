package ky.barry.pubgtracker.group

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import ky.barry.pubgtracker.group.PubgTrackerGroup.{ReplyUserList, RequestAllStats, RequestUserList}
import ky.barry.pubgtracker.manager.PubgTrackerManager.RequestTrackUser
import ky.barry.pubgtracker.query.PubgTrackerGroupQuery
import ky.barry.pubgtracker.user.PubgTrackerUser
import scala.concurrent.duration._

object PubgTrackerGroup {
  def props(groupId: String): Props = Props(new PubgTrackerGroup(groupId))

  final case class RequestUserList(requestId: Long)
  final case class ReplyUserList(requestId: Long, ids: Set[String])

  final case class RequestAllStats(requestId: Long)
  final case class RespondAllStats(requestId: Long, stats: Map[String, StatsReading])

  sealed trait StatsReading
  final case class Stats(value: String) extends StatsReading
  case object StatsTemperatureNotAvailable extends StatsReading
  case object UserNotAvailable extends StatsReading
  case object UserTimedOut extends StatsReading
}

class PubgTrackerGroup(groupId: String) extends Actor with ActorLogging {
  var userIdToActor = Map.empty[String, ActorRef]
  var actorToUserId = Map.empty[ActorRef, String]
  var nextCollectionId = 0L

  override def preStart(): Unit = log.info("PubgTrackerGroup {} started", groupId)

  override def postStop(): Unit = log.info("PubgTrackerGroup {} stopped", groupId)

  override def receive: Receive = {
    case trackMsg @ RequestTrackUser(`groupId`, _) =>
      userIdToActor.get(trackMsg.userId) match {
        case Some(userActor) =>
          userActor forward trackMsg
        case None =>
          log.info("Creating user actor for {}", trackMsg.userId)
          val userActor = context.actorOf(PubgTrackerUser.props(groupId, trackMsg.userId), s"user-${trackMsg.userId}")
          context.watch(userActor)
          actorToUserId += userActor -> trackMsg.userId
          userIdToActor += trackMsg.userId -> userActor
          userActor forward trackMsg
      }

    case RequestTrackUser(groupId, userId) =>
      log.warning(
        "Ignoring TrackUser request for {}. This actor is responsible for {}.",
        groupId, this.groupId)

    case RequestUserList(requestId) =>
      sender() ! ReplyUserList(requestId, userIdToActor.keySet)

    case Terminated(userActor) =>
      val userId = actorToUserId(userActor)
      log.info("User actor for {} has been terminated", userId)
      actorToUserId -= userActor
      userIdToActor -= userId

    case RequestAllStats(requestId) =>
      context.actorOf(PubgTrackerGroupQuery.props(
        actorToUserId = actorToUserId,
        requestId = requestId,
        requester = sender(),
        3.seconds))

  }
}