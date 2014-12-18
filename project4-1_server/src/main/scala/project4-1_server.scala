import akka.actor._
import scala.collection.mutable.HashMap
import scala.collection.mutable.Queue
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent._
import scala.concurrent.duration._
object project4server extends App {
	val numServers = 20
	val numClients = 10000
	val system = ActorSystem("PROJECT4-1")	
  var startId = 1
  var chunkSize = (numClients/numServers) 
  println(chunkSize)
  for(serverId<-1 to numServers){
    var server = system.actorOf(Props[Server],name = "server"+serverId)
    server ! Start(serverId,numClients,startId,chunkSize)
    startId = startId + chunkSize + 1
  }

  
  //println(repository.usersFollowersMap)
   

}	

object repository{
  //var usersArray = new ArrayBuffer[ActorRef]()
  var usersMap = new HashMap[Int,ActorRef]() 
  var usersFollowersMap = new HashMap[Int,ArrayBuffer[Int]]()// {override def default(key:Int) = new ArrayBuffer[Int]() }
  var usersTweetsMap = new HashMap[Int,Queue[String]]()      // {override def default(key:Int) = new Queue[String]() }
  
}
class Server extends Actor{
  var id = 0
  var numUsers = 0  
  var tweetCount = 0
  var usersFollowersMap = new HashMap[Int,ArrayBuffer[Int]]()
  var usersTweetsMap = new HashMap[Int,Queue[String]]()
  def receive ={
    case Start(serverId,numClients,startId,chunkSize)=>
      
      id = serverId
      numUsers = numClients      
      println("Server "+ id +"is working with "+startId+" and "+(startId+chunkSize))
      for(i<-startId to (startId+chunkSize)){
        usersFollowersMap(i) = ArrayBuffer[Int]()
        usersTweetsMap(i) = Queue[String]()        
        //println(i)
      }
      println("1st time Server "+id +" printing Users followers map has size "+usersFollowersMap.keys.size)
      Thread.sleep(10)
      for(i<-startId to (startId+chunkSize)){
        //repository.usersTweetsMap(i) = new Queue[String]()
        var numFollowers = 0
         if(i>=(0.99*numClients).toInt){
            numFollowers = (0.5*numClients).toInt
         }
        
         else if(i>=(0.79*numClients).toInt && i<0.99*numClients){
           numFollowers = (0.2 * numClients).toInt
         }
        
         else if(i>=(0.4*numClients).toInt && i<0.79*numClients){
           numFollowers = (0.1*numClients).toInt
         }
         else{
           numFollowers = (0.02*numClients).toInt
         }
      var j = i-1
        while(numFollowers>0 && j>0){
          usersFollowersMap(i).append(j)
          numFollowers -=1
          j -= 1
        } 
        
      }
    
      //println(repository.usersFollowersMap)
      println("Server "+id +" printing Users followers map has size "+usersFollowersMap.keys.size)
      //println("Server "+id +" printing Users tweets map has size "+repository.usersTweetsMap.keys.size)
      println(s"Server $id is ready")      
      import context.dispatcher
      context.system.scheduler.schedule(10 seconds, 1 second){
        self ! Calculate
      }

      //usersMap.foreach{keyVal => println(keyVal._1 + " "+keyVal._2.length) }
      //context.system.shutdown
     // println(usersMap)
    case Calculate=>
        println("Avg tweet counts by server "+id+" is "+tweetCount)
        tweetCount = 0
	 case Register(userId,user)=>
          println(s"Server $id is registering user $userId")
          repository.usersMap(userId) = user
          repository.usersTweetsMap(userId) = Queue[String]()
    
    case Tweet(userId,message) =>
      tweetCount += 1
      try{
      for(follower<-usersFollowersMap(userId)){
        repository.usersTweetsMap(follower).enqueue(message)
        while(repository.usersTweetsMap(follower).size > 100)
          repository.usersTweetsMap(follower).dequeue
        }
      }
        catch{
          case e:Exception=>
          println(s"Server $id does not have it $userId")
        }
      
    case GetMyTweets(userId) =>
      println(userId + "'s no. of followers are "+usersFollowersMap(userId).length)
      println(userId + "'s no. of tweets are "+repository.usersTweetsMap(userId).length)
      //for(msg<-repository.usersTweetsMap(userId))
      //  println(msg)
    println()
    case _=> println("Not a valid case")

  }
}



case class Start(serverId:Int,numClients:Int,startId:Int,chunkSize:Int)
case class Initialize(userId:Int,numServers:Int,numUsers:Int)
case class Register(userId:Int,user:ActorRef)
case object Tweet
case class Tweet(userId:Int,message:String)
case class GetMyTweets(userId:Int)
case object Calculate
