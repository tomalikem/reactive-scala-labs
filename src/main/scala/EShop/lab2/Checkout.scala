package EShop.lab2

import EShop.lab2.Checkout._
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.duration._
import scala.language.postfixOps

object Checkout {

  sealed trait Command
  case object StartCheckout                       extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command
  case class SelectPayment(payment: String)       extends Command
  case object ExpirePayment                       extends Command
  case object ConfirmPaymentReceived              extends Command

  sealed trait Event
  case object CheckoutClosed                        extends Event
  case class PaymentStarted(payment: ActorRef)      extends Event
  case object CheckoutStarted                       extends Event
  case object CheckoutCancelled                     extends Event
  case class DeliveryMethodSelected(method: String) extends Event

  def props(cart: ActorRef) = Props(new Checkout(cart))
}

class Checkout(
  cartActor: ActorRef
) extends Actor {

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)

  val checkoutTimerDuration = 1 seconds
  val paymentTimerDuration  = 1 seconds

  def receive: Receive = ???

  def selectingDelivery(timer: Cancellable): Receive = ???

  def selectingPaymentMethod(timer: Cancellable): Receive = ???

  def processingPayment(timer: Cancellable): Receive = ???

  def cancelled: Receive = ???

  def closed: Receive = ???

}
