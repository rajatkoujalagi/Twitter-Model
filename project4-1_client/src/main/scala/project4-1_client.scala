import akka.actor._
import scala.collection.mutable.HashMap
import scala.collection.mutable.Queue
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent._
import scala.concurrent.duration._
object project4client extends App {	
  val numServers = 20
	val numClients = 10000
	implicit val system = ActorSystem("PROJECT4Client")	
	
	for(i<-1 to numClients){
        var user = system.actorOf(Props[User],name = "User"+i)        
        user ! Initialize(i,numServers,numClients)        
      }
}
	



class User extends Actor{
  var id = 0
  var tweetCount = 0
  var message = ""   
  var myserver  = context.actorSelection("/user/Server")
  //override def toString = { id.toString }
  def receive={
    case Initialize(userId,numServers,numUsers)=>
	println("Initializing user "+userId)
      id = userId
      var serverId:Double = 0
      var chunkSize = numUsers/numServers
      serverId = (id*1.0/(chunkSize+1)).ceil

      myserver = context.actorSelection("akka.tcp://PROJECT4-1@127.0.0.1:2999/user/server"+serverId.toInt)
      var registerServer = context.actorSelection("akka.tcp://PROJECT4-1@127.0.0.1:2999/user/server1")
      registerServer ! Register(id,self)
      //myserver = context.actorFor("/user/server"+((id/(numUsers+1)) +1))
      Thread.sleep(5)
      var time=(30/id).toInt      
		import context.dispatcher
        context.system.scheduler.schedule(10 seconds, time milliseconds){
			self ! Tweet
		}
      //time=(5*id).toInt
      context.system.scheduler.schedule(20 seconds,10 seconds){
        println(s"User $id is asking for tweets")
        myserver ! GetMyTweets(id)
      }
      //myserver ! "Hey"
      
      //import context.dispatcher
      //context.system.scheduler.scheduleOnce(100 milliseconds, self, Tweet)
     // context.system.scheduler.scheduleOnce(200 milliseconds, self, Tweet)
      //context.system.scheduler.scheduleOnce(1000 milliseconds, myserver, GetMyTweets(self))
    case Tweet => 
        println(s"User $id is tweeting")
        tweetCount += 1
        message = "User " + id + " has tweeted " + tweetCount + " times"
    	myserver ! Tweet(id,message)
    
    case _=>
  }
}

case class Start(serverId:Int,numClients:Int)
case class Initialize(userId:Int,numServers:Int,numUsers:Int)
case class Register(userId:Int,user:ActorRef)
case object Tweet
case class Tweet(userId:Int,message:String)
case class GetMyTweets(userId:Int)

