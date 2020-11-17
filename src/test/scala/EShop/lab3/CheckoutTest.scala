package EShop.lab3

import EShop.lab2.{CartActor, Checkout}
import EShop.lab2.CartActor.{ConfirmCheckoutCancelled, ConfirmCheckoutClosed}
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class CheckoutTest
  extends TestKit(ActorSystem("CheckoutTest"))
  with AnyFlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  import Checkout._

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  it should "Send close confirmation to cart" in {
    val cart         = TestProbe()
    val orderManager = TestProbe()
    val checkout     = system.actorOf(Checkout.props(cart.ref), "checkout")
    cart.send(checkout, StartCheckout)
    orderManager.send(checkout, SelectDeliveryMethod("InPost"))
    orderManager.send(checkout, SelectPayment("Transfer"))
    orderManager.send(checkout, ConfirmPaymentReceived)
    cart.expectMsg(ConfirmCheckoutClosed)
  }

}
