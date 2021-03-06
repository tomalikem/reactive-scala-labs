package EShop.lab2

import EShop.lab2.Checkout._
import EShop.lab3.{OrderManager, Payment}
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object Checkout {

  sealed trait Data
  case object Uninitialized                               extends Data
  case class SelectingDeliveryStarted(timer: Cancellable) extends Data
  case class ProcessingPaymentStarted(timer: Cancellable) extends Data

  sealed trait Command
  case object StartCheckout                       extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command
  case class SelectPayment(payment: String)       extends Command
  case object ExpirePayment                       extends Command
  case object ConfirmPaymentReceived              extends Command

  sealed trait Event
  case object CheckOutClosed                   extends Event
  case class PaymentStarted(payment: ActorRef) extends Event

  def props(cart: ActorRef) = Props(new Checkout(cart))
}

class Checkout(
  cartActor: ActorRef
) extends Actor {

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)

  val checkoutTimerDuration = 1 seconds
  val paymentTimerDuration  = 1 seconds

  var deliveryMethod = ""
  var paymentMethod  = ""

  def receive: Receive = {
    case StartCheckout =>
      context become selectingDelivery(scheduler.scheduleOnce(checkoutTimerDuration, context.self, ExpireCheckout))
    case CancelCheckout =>
      context become cancelled
  }

  def selectingDelivery(timer: Cancellable): Receive = {
    case SelectDeliveryMethod(method) =>
      context become selectingPaymentMethod(timer)
    case CancelCheckout =>
      context become cancelled
    case ExpireCheckout =>
      context become cancelled
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = {
    case SelectPayment(method: String) =>
      this.paymentMethod = method
      val orderManager = sender()
      val payment =
        context.actorOf(Payment.props(method = deliveryMethod, orderManager = orderManager, checkout = self), "payment")
      orderManager ! OrderManager.ConfirmPaymentStarted(payment)
      context become processingPayment(scheduler.scheduleOnce(paymentTimerDuration, self, ExpirePayment))
    case CancelCheckout =>
      context become cancelled
    case ExpirePayment =>
      context become cancelled
    case ExpireCheckout =>
      context become cancelled
  }

  def processingPayment(timer: Cancellable): Receive = {
    case Checkout.ConfirmPaymentReceived =>
      cartActor ! CartActor.ConfirmCheckoutClosed
      context become closed
    case CancelCheckout =>
      context become cancelled
    case ExpirePayment =>
      context become cancelled
    case ExpireCheckout =>
      context become cancelled
  }

  def cancelled: Receive = {
    case _ =>
      context.stop(self)
  }

  def closed: Receive = {
    case _ =>
      context.stop(self)
  }

}
