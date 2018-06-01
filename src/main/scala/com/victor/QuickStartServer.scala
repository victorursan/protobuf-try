package com.victor

import scala.concurrent.{ Await, ExecutionContext }
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.victor.json.{ UserRegistryActor, UserRoutes }
import com.victor.proto.ProtoRoutes

import scala.concurrent.duration.Duration.Inf
import scala.util.{ Failure, Success }

object QuickStartServer extends App with UserRoutes with ProtoRoutes {

  implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  val userRegistryActor: ActorRef = system.actorOf(UserRegistryActor.props, "userRegistryActor")
  val protoRegistryActor: ActorRef = system.actorOf(UserRegistryActor.props, "protoRegistryActor")

  lazy val routes: Route = userRoutes

  Http(system).bindAndHandle(routes, "localhost", 8080)
    .onComplete {
      case Success(httpServer) => println(s"Server online at  ${httpServer.localAddress}")
      case Failure(t) => println(s"something went wrong while trying to create the server:\n $t")
    }

  Await.result(system.whenTerminated, Inf)
}
