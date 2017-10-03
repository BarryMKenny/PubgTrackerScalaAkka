import org.scalatest.FunSuite

import scalaj.http.{Http, HttpResponse}

class PubgTrackerAppHttpTest extends FunSuite {

  test("PubgTrackerApp.getUserStats") {
    val user = "Smorkula"
    val url = "https://pubgtracker.com/api/profile/pc/" + user
    val response: HttpResponse[String] = Http(url).header("TRN-API-KEY", "5596f0de-2e8a-4b3f-8fa6-499701859a77").asString
    assert(response.code === 200)
  }
}