import akka.io.IO
import spray.client.pipelining._
import spray.client._
import akka.pattern.ask
import akka.actor._
import spray.can.Http
import spray.http._
import HttpMethods._
import akka.util.Timeout
import scala.util.{Success,Failure}
import scala.concurrent.duration._
import scala.concurrent.Future

object project4client extends App {
	val numServers = 80
	val numUsers = 100

	implicit val system = ActorSystem("PROJECT4-2Client")

	for(userId<-1 to numUsers){
		var user = system.actorOf(Props(new User(userId,(userId%numServers)+1)),name="user"+userId)
		user ! START
	}
}

class User(id:Int,myServer:Int) extends Actor {
	import context.dispatcher
	val url = "http://localhost:"+(8000+myServer)
	implicit val system = context.system
	implicit val timeout: Timeout = 5.seconds
	val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
	def get(url:String) : Future[String] ={
		val futureResponse = pipeline(Get(url))
		futureResponse.map(_.entity.asString)
	}
	def receive ={
		case START =>
			println("Client "+id+" has server "+myServer)			
			var time = (3/id).toInt
			context.system.scheduler.schedule(5 seconds, time milliseconds){
				self ! Tweet
			}
			//Thread.sleep(5)
			context.system.scheduler.schedule(15 seconds, 5 seconds){
				self ! GetMyTimeline
			}
			/*val futureResponse = get(url+"/hello")

			futureResponse onComplete {
				case Success(response) => println(response)
				case Failure(error) => println("Error has occured "+error.getMessage)
			}*/
			//pipeline(Post("http://localhost:8081/hi?name=Bond"))
		
		case Tweet =>
			println("User "+id+" is tweeting with server "+myServer)
			pipeline(Post(url+"/tweet?userId="+id+"&msg="+"heyfrom"+id))
			self ! GetMyTimeline

		case GetMyTimeline =>
			println("User "+id+" requesting timeline")
			get(url+"/"+id+"/tweets")
		case Received =>
			println("Client got an object from server")
	}
}

case object START
case object Received
case object Tweet
case object GetMyTimeline
