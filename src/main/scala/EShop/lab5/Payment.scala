package EShop.lab5

import EShop.lab5.Payment.{DoPayment, PaymentConfirmed, PaymentRejected, PaymentRestarted}
import EShop.lab5.PaymentService.{PaymentClientError, PaymentServerError, PaymentSucceeded}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import akka.event.LoggingReceive

import scala.concurrent.duration._

object Payment {
  sealed trait Command
  case object DoPayment extends Command
  sealed trait Event
  case object PaymentConfirmed extends Event

  case object PaymentRejected
  case object PaymentRestarted

  def props(method: String, orderManager: ActorRef, checkout: ActorRef) =
    Props(new Payment(method, orderManager, checkout))

}

class Payment(
  method: String,
  orderManager: ActorRef,
  checkout: ActorRef
) extends Actor
  with ActorLogging {

  override def receive: Receive = LoggingReceive {
    case DoPayment =>
      // use payment service
      context.actorOf(PaymentService.props(method, self))
    case PaymentSucceeded =>
      orderManager ! PaymentConfirmed
      checkout ! PaymentConfirmed
    case _ =>
  }

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1.seconds) {
      case _: PaymentClientError =>
        notifyAboutRejection()
        Stop
      case _: PaymentServerError =>
        notifyAboutRestart()
        Restart
    }

  //please use this one to notify when supervised actor was stoped
  private def notifyAboutRejection(): Unit = {
    orderManager ! PaymentRejected
    checkout ! PaymentRejected
  }

  //please use this one to notify when supervised actor was restarted
  private def notifyAboutRestart(): Unit = {
    orderManager ! PaymentRestarted
    checkout ! PaymentRestarted
  }
}
