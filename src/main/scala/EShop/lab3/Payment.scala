package EShop.lab3

import EShop.lab2.Checkout
import EShop.lab3.OrderManager
import EShop.lab3.Payment._
import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive

object Payment {

  sealed trait Command
  case object DoPayment extends Command

  def props(method: String, orderManager: ActorRef, checkout: ActorRef) =
    Props(new Payment(method, orderManager, checkout))
}

class Payment(
  method: String,
  orderManager: ActorRef,
  checkout: ActorRef
) extends Actor {

  override def receive: Receive = {
    case DoPayment =>
      sender() ! OrderManager.ConfirmPaymentReceived
      checkout ! Checkout.ConfirmPaymentReceived
      context.stop(self)
  }
}
