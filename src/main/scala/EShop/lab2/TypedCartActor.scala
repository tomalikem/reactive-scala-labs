package EShop.lab2

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.language.postfixOps
import scala.concurrent.duration._
import EShop.lab3.TypedOrderManager

object TypedCartActor {

  sealed trait Command
  case class AddItem(item: Any)                                                  extends Command
  case class RemoveItem(item: Any)                                               extends Command
  case object ExpireCart                                                         extends Command
  case class StartCheckout(orderManagerRef: ActorRef[TypedOrderManager.Command]) extends Command
  case object ConfirmCheckoutCancelled                                           extends Command
  case object ConfirmCheckoutClosed                                              extends Command
  case class GetItems(sender: ActorRef[Cart])                                    extends Command // command made to make testing easier

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef[TypedCheckout.Command]) extends Event
}

class TypedCartActor {

  import TypedCartActor._

  val cartTimerDuration: FiniteDuration = 5 seconds

  private def scheduleTimer(context: ActorContext[TypedCartActor.Command]): Cancellable =
    context.scheduleOnce(cartTimerDuration, context.self, ExpireCart)

  def start: Behavior[TypedCartActor.Command] = empty

  def empty: Behavior[TypedCartActor.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case AddItem(item) => nonEmpty(Cart.empty.addItem(item), scheduleTimer(context))
        case _             => Behaviors.same
    }
  )

  def nonEmpty(cart: Cart, timer: Cancellable): Behavior[TypedCartActor.Command] =
    Behaviors.receive(
      (context, msg) =>
        msg match {
          case AddItem(item) => nonEmpty(cart.addItem(item), timer)
          case RemoveItem(item) if cart.contains(item) =>
            if (cart.size != 1) nonEmpty(cart.removeItem(item), timer) else empty
          case StartCheckout(orderManagerRef) =>
            val checkout = context.spawn(new TypedCheckout(context.self).start, "typedCheckout")
            orderManagerRef ! TypedOrderManager.ConfirmCheckoutStarted(checkout)
            inCheckout(cart)
          case ExpireCart => empty
      }
    )

  def inCheckout(cart: Cart): Behavior[TypedCartActor.Command] =
    Behaviors.receive(
      (context, msg) =>
        msg match {
          case ConfirmCheckoutCancelled => nonEmpty(cart, scheduleTimer(context))
          case ConfirmCheckoutClosed    => empty
      }
    )

}
