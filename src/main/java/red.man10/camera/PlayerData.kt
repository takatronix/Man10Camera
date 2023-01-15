package red.man10.camera

import java.util.*

  class PlayerData() : Comparable<PlayerData> {

      var updateTime:Long = System.currentTimeMillis()
      var blockBreakCount:Int = 0
      var playerMoveCount:Int = 0
      var uuid:UUID? = null

      // ユーザーのアクティブ時間
      fun getSleepTime():Long{
          return System.currentTimeMillis() - updateTime
      }

      // ユーザーがアクティブか
      fun isActive():Boolean{
          if(getSleepTime() > 5000)
              return false
          return true
      }
      override fun compareTo(other: PlayerData): Int {
        return (updateTime - other.updateTime).toInt()
      }
  }


