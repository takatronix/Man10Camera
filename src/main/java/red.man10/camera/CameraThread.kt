package red.man10.camera

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*

enum class CameraMode{
    AUTO,                       // 自動
    SPECTATOR,                  // スペクテーター
    FOLLOW,                     // 追跡
    GOAROUND,                   // 周囲を回る
}
class CameraThread(_cameraName: String) : Thread() {
    var cameraName = _cameraName
    var wait:Long = 500
    var running = true
    var target: Player? = null
    var distance:Double = 1.0
    // カメラモード
    private var cameraMode:CameraMode = CameraMode.AUTO

    var camera: Player? = null

    override fun run() {
        info("Camera thread started:${cameraName}")
        while(running){
            sleep(wait)
            if(!canWork())
                continue
            when(cameraMode){
                CameraMode.AUTO -> onAutoMode()
                CameraMode.SPECTATOR -> onSpectatorMode()
                CameraMode.FOLLOW -> onFollowMode()
                CameraMode.GOAROUND -> onGoAroundMode()
            }
            info("$cameraName is active")
        }
        info("Camera thread ended:${cameraName}")
    }

    private fun canWork() :Boolean{
        if(camera == null || camera?.isOnline == false)
            return false
        if(target == null || target?.isOnline == false)
            return false
        return true
    }

    fun setMode(mode:CameraMode){
        cameraMode = mode
    }

    // カメラプレイヤーの設定
    fun setCamera(sender: CommandSender, name:String?):Boolean {
        if(name == null){
            error("名前が指定されていません",sender)
            return false
        }
        val player = Bukkit.getOfflinePlayerIfCached(name)
        if(player == null){
            error("プレイヤーが存在しません",sender)
            return false
        }

        camera = player.player
        camera!!.gameMode = GameMode.SPECTATOR
        camera!!.spectatorTarget = target
        info("${player.name}をカメラに設定しました",sender)
        return true
    }

    // 監視対象の設定
    fun setTarget(sender: CommandSender, name:String?):Boolean {
        if(name == null){
            error("ターゲットが指定されていません",sender)
            return false
        }
        val player = Bukkit.getOfflinePlayerIfCached(name)
        if(player == null){
            error("プレイヤーが存在しません",sender)
            return false
        }

        target = player.player
        camera?.spectatorTarget = target
        info("${player.name}をターゲットに設定しました",sender)
        return true
    }


    fun onAutoMode(){
    }
    fun onSpectatorMode(){
    }
    fun onFollowMode(){
    }
    fun onGoAroundMode(){
    }
}

