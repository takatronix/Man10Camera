package red.man10.camera

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.ConcurrentHashMap

  class PlayerData : Comparable<PlayerData> {
      var updateTime:Long = System.currentTimeMillis()
      var blockBreakCount:Int = 0
      var playerMoveCount:Int = 0
      override fun compareTo(other: PlayerData): Int {
        return (updateTime - other.updateTime).toInt()
      }
  }


