import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl._

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.http.scaladsl._
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl._

object Main extends App {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  private val trustfulSslContext: SSLContext = {
    object NoCheckX509TrustManager extends X509TrustManager {
      override def checkClientTrusted(chain: Array[X509Certificate], authType: String) = ()
  
      override def checkServerTrusted(chain: Array[X509Certificate], authType: String) = ()
  
      override def getAcceptedIssuers = Array[X509Certificate]()
    }
  
    val context = SSLContext.getInstance("TLS")
    context.init(Array[KeyManager](), Array(NoCheckX509TrustManager), new SecureRandom())
    context
  }
  val noCertificateCheckContext = ConnectionContext.https(trustfulSslContext)
  val url = "https://self-signed.badssl.com"
  val res = Await.result(Http().singleRequest(Get(url), noCertificateCheckContext), 60 seconds)
  println(res)
  system.terminate()
}
