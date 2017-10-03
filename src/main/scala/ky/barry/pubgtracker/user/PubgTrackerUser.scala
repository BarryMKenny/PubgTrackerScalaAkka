package ky.barry.pubgtracker.user

import akka.actor.{Actor, ActorLogging, Props}
import ky.barry.pubgtracker.manager.PubgTrackerManager

object PubgTrackerUser {
  def props(groupId: String, userId: String): Props = Props(new PubgTrackerUser(groupId, userId))

  final case class RecordStats(requestId: Long, value: String)
  final case class StatsRecorded(requestId: Long)

  final case class ReadStats(requestId: Long)
  final case class RespondStats(requestId: Long, value: Option[String])
}

class PubgTrackerUser(groupId: String, userId: String) extends Actor with ActorLogging {
  import PubgTrackerUser._

  var lastStatsReading: Option[String] = None

  override def preStart(): Unit = log.info("PubgTrackerUser {}-{} started", groupId, userId)

  override def postStop(): Unit = log.info("PubgTrackerUser {}-{} stopped", groupId, userId)

  override def receive: Receive = {
    case PubgTrackerManager.RequestTrackUser(`groupId`, `userId`) =>
      sender() ! PubgTrackerManager.UserRegistered

    case PubgTrackerManager.RequestTrackUser(groupId, userId) =>
      log.warning(
        "Ignoring TrackUser request for {}-{}.This actor is responsible for {}-{}.",
        groupId, userId, this.groupId, this.userId)

    case RecordStats(id, value) =>
      log.info("Recorded stats reading {} with {}", value, id)
      lastStatsReading = Some(value)
      sender() ! StatsRecorded(id)

    case ReadStats(id) =>
      sender() ! RespondStats(id, lastStatsReading)
  }
}
/** object PubgTrackerUser extends App {

  def getUserStats(user : String) = {
    val url = "https://pubgtracker.com/api/profile/pc/" + user
    val response: HttpResponse[String] = Http(url).header("TRN-API-KEY", "5596f0de-2e8a-4b3f-8fa6-499701859a77").asString
    println(user + "'s stats: " + response.body)
  }
} **/