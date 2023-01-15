package red.man10.camera

import org.bukkit.*
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.io.File
import java.util.*
import kotlin.math.cos
import kotlin.math.sin


// カメラの動作モード
enum class CameraMode{
    STOP,                       // 自動
    AUTO,                       // 自動
    SPECTATOR,                  // スペクテーター
    LOOK,                       // 停止して対象を見る
    FOLLOW,                     // 追跡
    ROTATE,                     // 周囲を回る
}
enum class VisibleMode {
    HIDE,                       // 非表示(スペクテーター)
    SHOW,                       // 透明で頭だけ
    SHOWBODY                    // ボディも表示
}

class CameraThread : Thread() {
    var cameraName = ""                 // カメラ名
    var running = true                  // スレッド終了フラグ
    private var wait:Long = 1000 / 60   // 更新サイクル(60fps)
    private var target: Player? = null
    private var camera: Player? = null
    //region 設定
    private var radius:Double = 10.0
    var angleStep = 0.08            // 回転速度
    var angle = 0.0
    var nightVision = false
    var broadcast = false
    // カメラの相対位置
    var relativePos:Vector= Vector(2.0,2.0,0.0)
    // カメラモード
    private var cameraMode:CameraMode = CameraMode.AUTO

    val uniqueId:UUID?
        get(){ return camera?.uniqueId }
    //region プロパティ
    // カメラの座標
    val cameraPos:Vector?
        get(){ return camera?.location?.toVector() }
    // カメラの向き
    val cameraDir:Vector?
        get() { return camera?.location?.direction }
    // ターゲットの座標
    private val targetPos:Vector?
        get() { return target?.location?.toVector() }
    // ターゲットの向き
    val targetDir:Vector?
        get(){ return target?.location?.direction }
    // ターゲット-カメラ間距離
    val targetDistance:Double?
        get(){ return target?.location?.distance(camera?.location!!) }
    // カメラ->ターゲットのベクトル
    val toTargetVec:Vector?
        get(){ return targetPos?.subtract(cameraPos!!) }
    //endregion

    // スレッドメイン
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
                CameraMode.ROTATE -> onRotateMode()
                CameraMode.LOOK -> onSpectatorMode()
                else-> onStopMode()
            }
        }
        info("Camera thread ended:${cameraName}")
    }

    // スレッドが動作可能か？
    private fun canWork() :Boolean{
        when(cameraMode){
            // Lookモードは対象がなくてもOK
            CameraMode.LOOK -> {
                if(camera?.isOnline == true)
                    return true
            }
            else -> {
                if(camera?.isOnline == true && target?.isOnline == true)
                    return true
            }
        }
        return false
    }

    private fun setMode(sender: CommandSender,mode:CameraMode){
        cameraMode = mode
        // クリエイティブとスペクテーターを切り替えてスペクテーターターゲットを外す
        camera?.gameMode = GameMode.CREATIVE
        camera?.gameMode = GameMode.SPECTATOR
        camera?.spectatorTarget  = null
        save(sender)
    }

    //region 基本コマンド
    // 特定プレイヤーを追跡
    public fun follow(sender: CommandSender,player:Player? = null){
        setMode(sender,CameraMode.FOLLOW)
        if(player?.isOnline == true){
            target = player
        }
        info("${cameraName}をフォローモードに設定",sender)
    }
    public fun spectator(sender: CommandSender,player:Player? = null){
        setMode(sender,CameraMode.SPECTATOR)
        save(sender)
        if(player?.isOnline == true){
            camera?.spectatorTarget = player
        }
        info("${cameraName}をスペクテーターモードで監視",sender)
    }
    // 特定プレイヤーを回転しながら追跡
    public fun rotate(sender: CommandSender,player:Player? = null){
        setMode(sender,CameraMode.ROTATE)
        if(player?.isOnline == true){
            target = player
        }
        info("${cameraName}を回転モードに設定",sender)
    }
    // カメラを固定でプレイヤーを注視
    public fun look(sender: CommandSender,player:Player? = null){
        setMode(sender,CameraMode.ROTATE)
        if(player?.isOnline == true){
            target = player
        }
        info("${cameraName}をルックモードに設定",sender)
    }
    // カメラ停止
    public fun stop(sender: CommandSender,player:Player? = null){
        setMode(sender,CameraMode.STOP)
        info("${cameraName}を停止させました",sender)
    }
    //endregion

    fun setRelativePosition(sender: CommandSender,x:Double,y:Double,z:Double){
        relativePos = Vector(x,y,z)
        save(sender)
    }
    fun setRadius(sender: CommandSender,r:Double){
        radius = r
        if(radius < 1.0)
            radius = 1.0
        relativePos.x = radius
        save(sender)
    }

    fun setHeight(sender: CommandSender,h:Double){
        relativePos.y = h
        save(sender)
    }
    // カメラプレイヤーの設定
    fun setCamera(sender: CommandSender, name:String?):Boolean {
        val player = getOfflinePlayer(sender,name) ?: return false
        camera = player.player
        info("${cameraName}: ${player.name}をカメラに設定しました",sender)
        save(sender)
        return true
    }

    // 監視対象の設定
    fun setTarget(sender: CommandSender, name:String?):Boolean {
        val player = getOfflinePlayer(sender,name) ?: return false
        target = player.player
        info("${cameraName}${player.name}をターゲットに設定しました",sender)
        save(sender)
        return true
    }



    fun onAutoMode(){
    }
    fun onStopMode(){
    }
    fun onSpectatorMode(){
    }
    fun onLookMode(){
        lookAt(targetPos)
    }
    fun onFollowMode(){
        // ターゲットの相対位置のカメラ位置
        var loc = target?.location?.add(relativePos)
        var pos = loc?.toVector()
        var dir = targetPos?.subtract(pos!!)
        loc?.direction = dir!!
        // TODO:カメラがブロックとかぶっていたら、距離を近づける
        if(loc?.block?.type == Material.AIR){
        }
        teleport(loc)
    }

    //region 表示モード
    fun hide(sender:CommandSender){
        camera?.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE,1,true))
        camera?.gameMode = GameMode.SPECTATOR
    }
    fun show(sender:CommandSender){
        camera?.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE,1,true))
        camera?.gameMode = GameMode.CREATIVE
    }
    fun showBody(sender:CommandSender){
        camera?.removePotionEffect(PotionEffectType.INVISIBILITY)
        camera?.gameMode = GameMode.CREATIVE
    }
    //endregion

    fun setNightVision(sender:CommandSender,flag:Boolean){
        if(flag)
            camera?.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE,1,true))
        else
            camera?.removePotionEffect(PotionEffectType.NIGHT_VISION)
        nightVision = flag
        save(sender)
    }
    fun setBoradcast(sender:CommandSender,flag:Boolean){
        broadcast = flag
        save(sender)
    }

    private fun onRotateMode(){
        // ターゲットの相対位置のカメラ位置
        val loc = target?.location?.add(relativePos)
        angle += angleStep
        if(angle > 360)
            angle = 0.0
        val x = targetPos?.x?.plus(radius * cos(toRadian(angle)))!!
        val z = targetPos?.z?.plus(radius * sin(toRadian(angle)))!!
        val y = targetPos?.y?.plus(relativePos.y)!!
        loc?.set(x,y,z)
        val dir = targetPos?.subtract(loc!!.toVector())
        loc?.direction = dir!!
        teleport(loc)
    }

    // ポジションカメラをむける
    private fun lookAt(pos:Vector?){
        if(pos == null)
            return
        // カメラ->ターゲットのベクトルを設定する
        val loc = camera?.location
        loc?.direction = toTargetVec!!
        teleport(loc)
    }
    // テレポートする
    private fun teleport(loc:Location?){
        Bukkit.getScheduler().runTask(Main.plugin, Runnable {
            if(loc != null && camera != null && camera?.isOnline == true)
                camera?.teleport(loc)
        })
    }

    //region ファイル管理
    private fun save(sender:CommandSender?=null): Boolean {
        val file = File(Main.plugin.dataFolder, "camera${File.separator}$cameraName.yml")
        info("saving ${file.absolutePath}")
        try{
            val config = YamlConfiguration.loadConfiguration(file)
            config["target"] = target?.uniqueId.toString()
            config["camera"] = camera?.uniqueId.toString()
            config["cameraMode"] = cameraMode.toString()
            config["gameMode"] = camera?.gameMode.toString()
            config["radius"] = radius
            config["nightVision"] = nightVision
            config["broadcast"] = broadcast
            config.save(file)
        }
        catch (e:Exception){
            error("カメラ設定の保存に失敗しました:${cameraName} / ${e.localizedMessage}",sender)
            return false
        }
        info("${file.path}に保存")
        return true
    }

    fun load(sender:CommandSender? = null): Boolean {
        val file = File(Main.plugin.dataFolder, "camera${File.separator}$cameraName.yml")
        info("loading ${file.absolutePath}")
        try{
            val config = YamlConfiguration.loadConfiguration(file)

            // target UUID
            var s = config.getString("target")
            if(s != null)
                target = Bukkit.getPlayer(UUID.fromString(s))
            // camera UUID
            s = config.getString("camera")
            if(s != null)
                camera = Bukkit.getPlayer(UUID.fromString(s))

            cameraMode = enumValueOf(config["cameraMode"].toString())


            nightVision = config.getBoolean("nightVision")
            broadcast = config.getBoolean("broadcast")

            // 回転半径
            radius = config.getDouble("radius")
            if(radius < 1.0)
                radius = 1.0

            val gameMode = enumValueOf<GameMode>(config["gameMode"].toString())
            camera?.gameMode = gameMode
        }
        catch (e:Exception){
            error("カメラ設定の読み込みに失敗しました:${cameraName} / ${e.localizedMessage}",sender)
            return false
        }
        return true
    }

    // 設定ファイルを削除
    companion object fun deleteFile(sender: CommandSender?,folder:String, name: String): Boolean {
        val file = File(Main.plugin.dataFolder, "$folder${File.separator}$name.yml")
        if (file.delete()) {
            info("${name}を削除しました",sender)
        } else {
            error("${name}を削除に失敗しました",sender)
        }
        return false
    }
    //endregion



}

