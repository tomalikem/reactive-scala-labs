package EShop.lab3

import EShop.lab2.Checkout
import EShop.lab3.Payment.DoPayment
import akka.actor.{Actor, ActorRef, Props}

object Payment {

  sealed trait Command
  case object DoPayment extends Command

  sealed trait Event
  case object PaymentConfirmed extends Event

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
      orderManager ! Checkout.ConfirmPaymentReceived
      checkout ! Checkout.ConfirmPaymentReceived
  }

}
