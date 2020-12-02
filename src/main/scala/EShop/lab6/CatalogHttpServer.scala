package EShop.lab6

import java.net.URI

import EShop.lab6.Catalog.{GetItems, Item, Items}
import EShop.lab6.CatalogHttpServer.Response
import akka.actor.{ActorSystem, Props}
import akka.cluster.routing.{ClusterRouterPool, ClusterRouterPoolSettings}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.util.Try
object CatalogHttpServer {
  case class Query(product: String, productKeywords: List[String])
  case class Response(products: List[Item])
}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  //custom formatter just for example
  implicit val uriFormat = new JsonFormat[java.net.URI] {
    override def write(obj: java.net.URI): spray.json.JsValue = JsString(obj.toString)
    override def read(json: JsValue): URI = json match {
      case JsString(url) => new URI(url)
      case _             => throw new RuntimeException("Parsing exception")
    }
  }
  implicit val itemFormat     = jsonFormat5(Item)
  implicit val queryFormat    = jsonFormat2(CatalogHttpServer.Query)
  implicit val responseFormat = jsonFormat1(CatalogHttpServer.Response)
}

object ClusterNodeApp extends App {
  private val config = ConfigFactory.load()

  val system = ActorSystem(
    "ClusterWorkRouters",
    config
      .getConfig(Try(args(0)).getOrElse("cluster-default"))
      .withFallback(config.getConfig("cluster-default"))
  )
}

object CatalogHttpServerApp extends App {
  new CatalogHttpServer().startServer("localhost", args(0).toInt)
}

class CatalogHttpServer extends HttpApp with JsonSupport {
  val config = ConfigFactory.load()
  val system = ActorSystem(
    "ClusterWorkRouters",
    config.getConfig("cluster-default")
  )
  val workers = system.actorOf(
    ClusterRouterPool(
      RoundRobinPool(0),
      ClusterRouterPoolSettings(totalInstances = 100, maxInstancesPerNode = 3, allowLocalRoutees = true)
    ).props(Catalog.props(new SearchService())),
    name = "clusterWorkerRouter"
  )

  implicit val timeout: Timeout = 5.seconds
  override protected def routes: Route = {
    path("search") {
      post {
        entity(as[CatalogHttpServer.Query]) { query =>
          val future = workers ? GetItems(query.product, query.productKeywords)
          val result = Await.result(future, timeout.duration).asInstanceOf[Items]

          complete {
            Future.successful(Response(result.items))
          }
        }
      }
    }
  }
}
