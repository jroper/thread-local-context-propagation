package controllers

import play.api.Logger
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def playWebsite = Action.async {
    Logger.info("Checking playframework.com...")
    WS.url("http://playframework.com").get().map { response =>
      val message = if (response.status <= 400) {
        "playframework.com is up: " + response.status
      } else {
        "playframework.com is down: " + response.status
      }
      Logger.info(message)
      Ok(message + "\n")
    }
  }

}