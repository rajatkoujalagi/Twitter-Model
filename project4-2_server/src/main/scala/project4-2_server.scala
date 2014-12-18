
import akka.io.IO
import spray.routing._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import akka.actor._
import spray.can.Http
import spray.can.server.Stats
import scala.collection.mutable.HashMap
import scala.collection.mutable.Queue
import scala.collection.mutable.ArrayBuffer
import spray.util._
import spray.http._
import HttpMethods._
import MediaTypes._
import spray.can.Http.RegisterChunkHandler

object project4server extends App {
	val numServers = 80
	val numUsers = 100
	val numClients = numUsers / numServers
	var clientStartId = (-1*numClients)+1
	implicit val system = ActorSystem("PROJECT4-2Server")
	implicit val timeout = Timeout(5.seconds)		
	val statsActor = system.actorOf(Props[StatsActor])
	statsActor ! Init
	for(serverId<- 1 to numServers){
		var server  = system.actorOf(Props(new Server(serverId,numUsers,numServers,statsActor)),name="server"+serverId)		
		clientStartId += numClients		
		IO(Http) ? Http.Bind(server,interface="localhost",port=8000+serverId)		
	}
}



class Server(id:Int,numUsers:Int,numServers:Int,statsActor:ActorRef) extends Actor with SimpleRoutingApp with HttpService{		
	implicit val timeout = Timeout(5.seconds)	
	def cont = context
	import context.dispatcher
	implicit val system =  context.system
	var usersFollowersMap = new HashMap[Int,ArrayBuffer[Int]]()
	var usersTweetsMap = new HashMap[Int,Queue[String]]()
//	println("Server "+id+" is forming the tweets map")
	for(i<- 1 to numUsers){		
		if(id == ((i%numServers)+1)){
			usersFollowersMap(i) = ArrayBuffer[Int]()
			usersTweetsMap(i) = Queue[String]()			
			var numFollowers = 0
			if(i>=(0.99*numUsers).toInt){
	    		numFollowers = (0.5*numUsers).toInt
	 		}

	 		else if(i>=(0.79*numUsers).toInt && i<0.99*numUsers){
	   			numFollowers = (0.2 * numUsers).toInt
	 		}
	 		else if(i>=(0.4*numUsers).toInt && i<0.79*numUsers){
	   			numFollowers = (0.1*numUsers).toInt
	 		}
	 		else{
	   			numFollowers = (0.02*numUsers).toInt
	 		}
				var j = i-1						
			var count=0


			while(j>0){
				if(id==((j%numServers)+1)){
	  				usersFollowersMap(i).append(j)
	  			}
	  			numFollowers -=1
	  			j -= 1
			}
			//println(i+" has "+usersFollowersMap(i).size+" followers")
		}
	}
	if(id==numServers)
		println("Ready")
//	println("Server "+id+" is printing "+usersFollowersMap.size+" "+usersTweetsMap.size)

	var route = 
	get{
		path("hello"){			
			complete{				
				"Connected to server "+id+"\n"
			}
		}~
		path(IntNumber / "followers"){ index =>			
			complete{
				try{
					println(usersFollowersMap(index))
				}catch{  case _ : NoSuchElementException => println("Server "+id+" does not handle this user")}
				"OK\n"
			}
		}~
		path(IntNumber / "tweets"){ userId =>		
			complete{
				statsActor ! GotTweetRequest
				try{
				//	println(userId + "'s tweets are "+usersTweetsMap(userId))
					}catch{ case _ : NoSuchElementException => println("Server "+id+" does not handle this user")}
				"OK\n"
			}
		}~
		path("list"){			
			complete{
				println("Printing server "+id+" users")
				for(i<-usersTweetsMap.keys){
					println(i)
				}
				"OK"
			}
		}~
		path("total"){
			complete{
				statsActor ! GetTotalTweets
				"OK"
			}
		}
	}~	
	post{
		path("hi"){
			parameters("name"){ (name) =>
				println("Name is "+name)
				complete{
					"OK"
				}

			}
		}~
		path("tweet"){
			parameters("userId".as[Int],"msg"){ (userId,msg) =>
				statsActor ! GotTweetRequest
			//	println("User "+userId+" is tweeting")
				try{
					for(follower<-usersFollowersMap(userId)){
						usersTweetsMap(follower).enqueue(msg)
					
						while(usersTweetsMap(follower).size > 100)
	          				usersTweetsMap(follower).dequeue
	          		}
	          	}catch{ case _:NoSuchElementException => println("Server "+id+" does not handle this user")}
          		complete{
          			"OK"
          		}
        	}
		}~
		path("addUser"){
			parameters("userId".as[Int]){ (i) =>
				usersFollowersMap(i) = ArrayBuffer[Int]()
				usersTweetsMap(i) = Queue[String]()
				complete{
					"OK"
				}

			}
		}
		
	}

	def receive = runRoute(route)	
}

class StatsActor extends Actor{
	var avgtweetCount = 0
	var totalCount = 0
	def receive={
		case Init=>
			println("StatsActor has started")
			import context.dispatcher
			context.system.scheduler.schedule(10 seconds, 1 seconds){
				self ! Calculate
			}
		case GotTweetRequest =>
			avgtweetCount += 1
			totalCount += 1
		case Calculate =>
			println("Avg tweets per second is "+avgtweetCount)
			avgtweetCount = 0
		case GetTotalTweets =>
			println("Total number of tweets are "+totalCount)

	}
}

case class START(i:Int,numClients:Int,clientStartId:Int,numUsers:Int)
case object GotTweetRequest
case object Init
case object Calculate
case object GetTotalTweets
case object Received