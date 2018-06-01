package com.victor.proto

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes.{ Created, OK }
import akka.http.scaladsl.model.Uri.Path.Segment
import akka.http.scaladsl.server.Directives.{ as, entity, onSuccess, pathEnd, pathPrefix, rejectEmptyResponse, _ }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{ delete, get, post }
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout
import com.victor.json.UserRegistryActor._
import com.victor.json.{ User, UserRoutes, Users }

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait ProtoRoutes extends ProtobufSupport {
  implicit def system: ActorSystem

  def protoRegistryActor: ActorRef

  lazy val log = Logging(system, classOf[UserRoutes])

  implicit lazy val timeout = Timeout(5 seconds)

  lazy val protoRoutes: Route = pathPrefix("proto") {
    pathEnd {
      get {
        val users: Future[Users] =
          (userRegistryActor ? GetUsers).mapTo[Users]
        complete(users)
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
    } ~ path(Segment) { name =>
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
