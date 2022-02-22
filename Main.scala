import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl._

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl._

object Main extends App {

  def createInsecureSslContext(): SSLContext = {
    val trustAllCerts: Array[TrustManager] = Array(new X509TrustManager {
        override def getAcceptedIssuers(): Array[X509Certificate] = null
        override def checkClientTrusted(certs: Array[X509Certificate], authType: String) = {}
        override def checkServerTrusted(certs: Array[X509Certificate], authType: String) = {}
    })

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustAllCerts, new SecureRandom())
    sslContext
  }

  def createInsecureSslEngine(host: String, port: Int): SSLEngine = {
    val context = createInsecureSslContext()   

    val engine = context.createSSLEngine(host, port)
    // val engine = SSLContext.getDefault.createSSLEngine(host, port)
    engine.setUseClientMode(true)
    engine.setSSLParameters({
      val params = engine.getSSLParameters
      params.setEndpointIdentificationAlgorithm("https")
      params})
    engine
  }

  implicit val system = ActorSystem()
  val url = "https://nu.nl"
  val badCtx = ConnectionContext.httpsClient(createInsecureSslEngine _)
  val res = Http().outgoingConnectionHttps("expired.badssl.com", 443, connectionContext = badCtx)
  val response = Await.result(Source.single(Get(url)).via(res).runWith(Sink.head), 60 seconds)
  val responseString = Await.result(Unmarshal(response).to[String], 60 seconds)
  println(responseString)
  system.terminate()
}
