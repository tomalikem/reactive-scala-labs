package EShop.lab3

import EShop.lab2.{CartActor, Checkout}
import EShop.lab3.OrderManager._
import EShop.lab3.Payment.DoPayment
import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object OrderManager {

  sealed trait Command
  case class AddItem(id: String)                                               extends Command
  case class RemoveItem(id: String)                                            extends Command
  case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String) extends Command
  case object Buy                                                              extends Command
  case object Pay                                                              extends Command
  case class ConfirmCheckoutStarted(checkoutRef: ActorRef)                     extends Command
  case class ConfirmPaymentStarted(paymentRef: ActorRef)                       extends Command
  case object ConfirmPaymentReceived                                           extends Command

  sealed trait Ack
  case object Done extends Ack //trivial ACK
}

class OrderManager extends Actor {
  implicit val timeout: Timeout = Timeout(5 seconds)
  override def receive: Receive = uninitialized

  def uninitialized: Receive = {
    case AddItem(item) =>
      val cartActor = context.actorOf(
        CartActor.props(),
        "cart"
      )
      cartActor ! CartActor.AddItem(item)
      context become open(cartActor)
      sender() ! Done
  }

  def open(cartActor: ActorRef): Receive = {
    case AddItem(item) =>
      cartActor ! CartActor.AddItem(item)
      sender() ! Done
    case RemoveItem(item) =>
      cartActor ! CartActor.RemoveItem(item)
      sender() ! Done
    case Buy =>
      val future           = cartActor ? CartActor.StartCheckout
      val result           = Await.result(future, timeout.duration).asInstanceOf[ConfirmCheckoutStarted]
      val checkoutActorRef = result.checkoutRef
      checkoutActorRef ! Checkout.StartCheckout
      context become inCheckout(checkoutActorRef)
      sender() ! Done
  }

  //def inCheckout(cartActorRef: ActorRef, senderRef: ActorRef): Receive = ???

  def inCheckout(checkoutActorRef: ActorRef): Receive = {
    case SelectDeliveryAndPaymentMethod(delivery, payment) =>
      checkoutActorRef ! Checkout.SelectDeliveryMethod(delivery)

      val future          = checkoutActorRef ? Checkout.SelectPayment(payment)
      val result          = Await.result(future, timeout.duration).asInstanceOf[ConfirmPaymentStarted]
      val paymentActorRef = result.paymentRef
      context become inPayment(paymentActorRef)
      sender() ! Done
  }

  def inPayment(paymentActorRef: ActorRef): Receive = {
    case Pay =>
      val future = paymentActorRef ? DoPayment
      Await.result(future, timeout.duration)
      context become finished
      sender() ! Done
  }

  //def inPayment(paymentActorRef: ActorRef, senderRef: ActorRef): Receive = ???

  def finished: Receive = {
    case _ =>
      sender() ! "order manager finished job"
      sender() ! Done
  }
}
