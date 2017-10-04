package ky.barry.pubgtracker.app

import akka.actor.ActorSystem
import ky.barry.pubgtracker.group.PubgTrackerGroup
import ky.barry.pubgtracker.manager.PubgTrackerManager
import ky.barry.pubgtracker.user.PubgTrackerUser

object PubgTrackerApp extends App {
  val system = ActorSystem("pubg-tracker-system")

  val manager = system.actorOf(PubgTrackerManager.props())
  manager ! "default"
  //manager ! PubgTrackerManager.RequestTrackUser("userGroup", "ApollyonVeyron")
  //manager ! PubgTrackerManager.RequestTrackUser("userGroup", "Smorkula")
  //manager ! PubgTrackerManager.RequestTrackUser("userGroup", "GillyWilly1")
  //manager ! PubgTrackerManager.RequestTrackUser("userGroup", "EvilSpaceMantis")
  //manager ! PubgTrackerManager.RequestTrackUser("userGroup", "TrueCold")
  //manager ! PubgTrackerManager.RequestTrackUser("userGroup", "Con_Lad")
}