package ky.barry.pubgtracker.app

import akka.actor.ActorSystem
import ky.barry.pubgtracker.group.PubgTrackerGroup
import ky.barry.pubgtracker.manager.PubgTrackerManager
import ky.barry.pubgtracker.user.PubgTrackerUser

object PubgTrackerApp extends App {
  val system = ActorSystem("pubg-tracker-system")

  val manager = system.actorOf(PubgTrackerManager.props())
  val group = system.actorOf(PubgTrackerGroup.props("userGroup"))
  val apollyonVeyron = system.actorOf(PubgTrackerUser.props("userGroup", "ApollyonVeyron"))

  apollyonVeyron ! (PubgTrackerManager.RequestTrackUser("userGroup", "ApollyonVeyron"), apollyonVeyron)
  apollyonVeyron ! PubgTrackerUser.RecordStats(1,"1")
  group ! PubgTrackerUser.RecordStats(1,"1")




/**
  getUserStats("ApollyonVeyron")
  getUserStats("Smorkula")
  getUserStats("GillyWilly1")
  getUserStats("EvilSpaceMantis")
  getUserStats("TrueCold")
  getUserStats("Con_Lad")
  **/
}