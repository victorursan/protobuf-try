package com.victor.proto

import akka.actor.{ Actor, ActorLogging, Props }
import com.victor.other.ActionPerformed
import users_schema.{ PUser, PUsers }

object ProtoRegistryActor {
  final case object GetUsers
  final case class CreateUser(user: PUser)
  final case class GetUser(name: String)
  final case class DeleteUser(name: String)

  def props: Props = Props[ProtoRegistryActor]
}

class ProtoRegistryActor extends Actor with ActorLogging {
  import ProtoRegistryActor._

  var users = Set.empty[PUser]

  def receive: Receive = {
    case GetUsers =>
      sender() ! PUsers
    case CreateUser(user) =>
      users += user
      sender() ! ActionPerformed(s"User ${user.name} created.")
    case GetUser(name) =>
      sender() ! users.find(_.name == name)
    case DeleteUser(name) =>
      users.find(_.name == name) foreach { user => users -= user }
      sender() ! ActionPerformed(s"User $name deleted.")
  }
}
