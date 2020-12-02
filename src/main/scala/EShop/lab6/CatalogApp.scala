package EShop.lab6
import java.net.URI
import java.util.zip.GZIPInputStream

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.routing.{ActorRefRoutee, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.Random

class SearchService() {

  private val gz = new GZIPInputStream(
    getClass.getResourceAsStream("/query_result.gz")
  )
  
  private[lab6] val productItemsMap = Source
    .fromInputStream(gz)
    .getLines()
    .drop(1) //skip header
    .filter(_.split(",").length >= 3)
    .map { line =>
      val values = line.split(",")
      Catalog.Item(
        new URI("http://catalog.com/product/" + values(0).replaceAll("\"", "")),
        values(1).replaceAll("\"", ""),
        values(2).replaceAll("\"", ""),
        Random.nextInt(1000).toDouble,
        Random.nextInt(100)
      )
    }
    .toList
    .groupBy(_.product.toLowerCase)

  def search(product: String, keyWords: List[String]): List[Catalog.Item] = {
    val lowerCasedKeyWords = keyWords.map(_.toLowerCase)
    productItemsMap
      .getOrElse(product.toLowerCase, Nil)
      .map(
        item => (lowerCasedKeyWords.count(item.name.toLowerCase.contains), item)
      )
      .sortBy(-_._1)
      .take(10)
      .map(_._2)
  }
}

object Master {
  def props(): Props =
    Props(new Master())
}

class Master() extends Actor {
  import Catalog._
  val nbOfRoutees = 5
  val router: ActorRef =
    context.actorOf(RoundRobinPool(nbOfRoutees).props(Catalog.props(new SearchService)), "router")
  def receive: Receive = LoggingReceive {
    case GetItems(product, productKeyWords) =>
      router.forward(Catalog.GetItems(product, productKeyWords))
    case _ =>
  }
}

object Catalog {
  case class Item(id: URI, name: String, product: String, price: BigDecimal, count: Int)
  sealed trait Query
  case class GetItems(product: String, productKeyWords: List[String]) extends Query
  sealed trait Ack
  case class Items(items: List[Item]) extends Ack
  def props(searchService: SearchService): Props =
    Props(new Catalog(searchService))
}

class Catalog(searchService: SearchService) extends Actor {
  import Catalog._
  override def receive: Receive = LoggingReceive {
    case GetItems(product, productKeyWords) =>
      sender() ! Items(searchService.search(product, productKeyWords))
  }
}

object CatalogApp extends App {
  private val config = ConfigFactory.load()
  private val catalogSystem = ActorSystem(
    "Catalog",
    config.getConfig("catalog").withFallback(config)
  )
  catalogSystem.actorOf(
    Master.props(),
    "catalog"
  )
  Await.result(catalogSystem.whenTerminated, Duration.Inf)
}
