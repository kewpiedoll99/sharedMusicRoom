import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

import scala.collection.mutable
import scala.concurrent.Future

/**
  *  The class you will be trying to implement.
  *
  *  @param songRepo a library that gives the lengths of songs. It will be passed in as a dependency; you don't need to implement it.
  **/
class MusicSharingApp(songRepo: SongRepo) {
  // room-skip votes map
  val roomSkipVotesMap: ConcurrentHashMap[Int, Int] = new ConcurrentHashMap[Int, Int]()

  // room-channel map
  val roomChannelMap: ConcurrentHashMap[Int, mutable.ListBuffer[Channel]] = new ConcurrentHashMap[Int, mutable.ListBuffer[Channel]]()
  // room channel count is roomChannelMap.getOrElse(roomId, new ConcurrentHashMap[Int, List[Channel]]).size

  val roomNextSongTimeMap: ConcurrentHashMap[Int, LocalDateTime] = new ConcurrentHashMap[Int, LocalDateTime]()

  // room-song stack
  val roomSongStackMap: ConcurrentHashMap[Int, mutable.Stack[Int]] = new ConcurrentHashMap[Int, mutable.Stack[Int]]()

  def playNextSong(roomId: Int) = {
    // pop song off stack
    val songId = roomSongStackMap.get(roomId).pop()
    // get song length from songRepo
    val length = songRepo.getLength(songId)
    // compute time song will be done
    val timeDone: LocalDateTime = currentTime.plus(length.asInstanceOf[Long], ChronoUnit.SECONDS)
    // set that time in roomNextSongTimeMap
    roomNextSongTimeMap.put(roomId, timeDone)
    // foreach channel in roomChannelMap channel.pushSong(songId)
    roomChannelMap.get(roomId).foreach(channel => channel.pushSong(songId))
  }
  def skipSong(roomId: Int): Boolean = {
    roomSkipVotesMap.get(roomId) >= roomChannelMap.get(roomId).size / 2
  }

  def currentTime: LocalDateTime = {
    LocalDateTime.now()
  }

  def timeUp(roomId: Int): Boolean = {
    currentTime.compareTo(roomNextSongTimeMap.get(roomId)) > 0
  }
  def main(args: Array[String]): Unit = {
    // for each room in keys to room-channel map
    while (roomChannelMap.keys().hasMoreElements) {
      val roomId = roomChannelMap.keys().nextElement()
      // if the time is up or majority want to skip, playNextSong(roomId)
      if (skipSong(roomId) || timeUp(roomId)) playNextSong(roomId)
      // wait a sec
      Thread sleep 1000
    }
  }

  /**
    * This is just an entrance of the app, futher communication with the clients will be through the channel
    *
    * @param roomId the id of th room. If the roomId does not exist in the system create a room and join that room
    * @param channel the communication Channel with the clients going forward. The trait is given below; you don't need to implement it.
    **/
  def joinOrCreateRoom(roomId: Int, channel: Channel): Unit = {
    val channels = roomChannelMap.get(roomId, new mutable.ListBuffer[Channel]) :+ channel
    roomChannelMap.put(roomId, channels)
  }
  def addSong(roomId: Int, songId: Int): Unit = {
    val songs = roomSongStackMap.get(roomId, new mutable.Stack[Int]())
    songs.push(songId)
    roomSongStackMap.put(roomId, songs)
  }
}
/** Dependencies to the following classes and traits will be passed in. No need to try implement them. **/
trait Channel {
  def pushSong(songId: Int): Unit           // push a song to the client which will start to play
  def onReceive(handler: (Message) => Unit) // incoming messages from the client will be passed to the handler
}

//trait Room {
//  def playSong(songId: Int): Unit
//}
sealed trait Message
case class AddSong(songId: Int) extends Message
case class VoteToSkipSong(songId: Int) extends Message

trait SongRepo {
  def getLength(songId: Int): Future[Option[Int]] //get the length of a song in seconds. This is an async method. Returns None if the song is not found.
}
