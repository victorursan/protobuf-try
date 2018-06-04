package com.victor.proto

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes.{ Created, OK }
import akka.http.scaladsl.server.Directives.{ as, entity, onSuccess, pathEnd, pathPrefix, rejectEmptyResponse, _ }
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{ delete, get, post }
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout
import users_schema.{ PUser, PUsers }
import akka.pattern.ask
import com.victor.json.UserRoutes

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait ProtoRoutes extends ProtobufSupport {
  implicit def system: ActorSystem

  def protoRegistryActor: ActorRef

  import com.victor.other.ActionPerformed
  import com.victor.proto.ProtoRegistryActor._

  lazy val log = Logging(system, classOf[UserRoutes])

  implicit lazy val timeout = Timeout(5 seconds)

  implicit lazy val x = PUser.messageCompanion
  implicit lazy val y = PUsers.messageCompanion

  lazy val protoRoutes: Route = pathPrefix("proto") {
    pathEnd {
      get {
        val users: Future[PUsers] =
          (protoRegistryActor ? GetUsers).mapTo[PUsers]
        complete(users)
      } ~
        post {
          entity(as[PUser]) { user =>
            val userCreated: Future[ActionPerformed] =
              (protoRegistryActor ? CreateUser(user)).mapTo[ActionPerformed]
            onSuccess(userCreated) { performed =>
              log.info(s"Created user [${user.name}]: ${performed.description}")
              complete(Created, performed)
            }
          }
        }
    } ~ path(Segment) { name =>
      get {

        val maybeUser: Future[Option[PUser]] =
          (protoRegistryActor ? GetUser(name)).mapTo[Option[PUser]]
        rejectEmptyResponse {
          complete(maybeUser)
        }
      } ~
        delete {
          val userDeleted: Future[ActionPerformed] =
            (protoRegistryActor ? DeleteUser(name)).mapTo[ActionPerformed]
          onSuccess(userDeleted) { performed =>
            log.info(s"Deleted user [$name]: ${performed.description}")
            complete(OK, performed)
          }
        }
    }
  }

}
