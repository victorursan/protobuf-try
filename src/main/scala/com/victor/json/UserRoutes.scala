package com.victor.json

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{ delete, get, post }
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import com.victor.json.UserRegistryActor._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait UserRoutes extends JsonSupport {
  implicit def system: ActorSystem

  def userRegistryActor: ActorRef

  lazy val log = Logging(system, classOf[UserRoutes])

  implicit lazy val timeout = Timeout(5 seconds)

  //  implicit val companion =

  lazy val userRoutes: Route =
    pathPrefix("users") {
      pathEnd {
        get {

          val users: Future[Users] =
            (userRegistryActor ? GetUsers).mapTo[Users]
          complete(OK, users)
        } ~
          post {
            entity(as[User]) { user =>
              val userCreated: Future[ActionPerformed] =
                (userRegistryActor ? CreateUser(user)).mapTo[ActionPerformed]
              onSuccess(userCreated) { performed =>
                log.info(s"Created user [${user.name}]: ${performed.description}")
                complete(Created, performed)
              }
            }
          }
      } ~
        path(Segment) { name =>
          get {
            val maybeUser: Future[Option[User]] =
              (userRegistryActor ? GetUser(name)).mapTo[Option[User]]
            rejectEmptyResponse {
              complete(maybeUser)
            }
          } ~
            delete {
              val userDeleted: Future[ActionPerformed] =
                (userRegistryActor ? DeleteUser(name)).mapTo[ActionPerformed]
              onSuccess(userDeleted) { performed =>
                log.info(s"Deleted user [$name]: ${performed.description}")
                complete(OK, performed)
              }
            }
        }
    }

}
