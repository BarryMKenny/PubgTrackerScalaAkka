package ky.barry.pubgtracker.manager

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import ky.barry.pubgtracker.group.PubgTrackerGroup
import ky.barry.pubgtracker.manager.PubgTrackerManager.RequestTrackUser
object PubgTrackerManager {
  def props(): Props = Props(new PubgTrackerManager)

  final case class RequestTrackUser(groupId: String, userId: String)
  case object UserRegistered
}

class PubgTrackerManager extends Actor with ActorLogging {
  var groupIdToUser = Map.empty[String, ActorRef]
  var userToGroupId = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("PubgTrackerManager started")

  override def postStop(): Unit = log.info("PubgTrackerManager stopped")

  override def receive = {
    case trackMsg @ RequestTrackUser(groupId, _) =>
      log.info("Received")
      groupIdToUser.get(groupId) match {
        case Some(ref) =>
          log.info("Some(ref) {}", groupId)
          ref forward trackMsg
        case None =>
          log.info("Creating user group actor for {}", groupId)
          val groupActor = context.actorOf(PubgTrackerGroup.props(groupId), "group-" + groupId)
          context.watch(groupActor)
          groupActor forward trackMsg
          groupIdToUser += groupId -> groupActor
          userToGroupId += groupActor -> groupId
      }

    case Terminated(groupActor) =>
      val groupId = userToGroupId(groupActor)
      log.info("User group actor for {} has been terminated", groupId)
      userToGroupId -= groupActor
      groupIdToUser -= groupId

  }
}
