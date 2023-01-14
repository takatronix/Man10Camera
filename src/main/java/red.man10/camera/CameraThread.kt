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


enum class CameraMode{
    STOP,                       // 自動
    AUTO,                       // 自動
    SPECTATOR,                  // スペクテーター
    LOOK,                       // 停止して対象を見る
    FOLLOW,                     // 追跡
    GOAROUND,                   // 周囲を回る
}
class CameraThread : Thread() {
    var cameraName = ""
    var wait:Long = 1000 / 60 // 60fps
    var running = true
    var target: Player? = null

    var radius:Double = 10.0

    var angleStep = 0.08
    var angle = 0.0
    var nightVision = false
    // カメラの相対位置
    var relativePos:Vector= Vector(2.0,2.0,0.0)

    // カメラモード
    private var cameraMode:CameraMode = CameraMode.AUTO

    // カメラプレイヤー
    private var camera: Player? = null

    val uniqueId:UUID?
        get() {
            return camera?.uniqueId
        }
    //region プロパティ
    // カメラの座標
    val cameraPos:Vector?
        get() {
            return camera?.location?.toVector()
        }
    // カメラの向き
    val cameraDir:Vector?
        get() {
            return camera?.location?.direction
        }
    // ターゲットの座標
    private val targetPos:Vector?
        get() {
            return target?.location?.toVector()
        }
    // ターゲットの向き
    val targetDir:Vector?
        get() {
            return target?.location?.direction
        }
    // ターゲット-カメラ間距離
    val targetDistance:Double?
        get(){
            return target?.location?.distance(camera?.location!!)
        }
    // カメラ->ターゲットのベクトル
    val toTargetVec:Vector?
        get(){
            return targetPos?.subtract(cameraPos!!)
        }

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
                CameraMode.GOAROUND -> onGoAroundMode()
                CameraMode.LOOK -> onSpectatorMode()
                else-> onStopMode()
            }
           // info("$cameraName is active")
        }
        info("Camera thread ended:${cameraName}")
    }

    // スレッドが動作可能か？
    // カメラと対象がオンラインの時のみ動作可能
    private fun canWork() :Boolean{
        if(camera == null || camera?.isOnline == false)
            return false
        if(target == null || target?.isOnline == false)
            return false
        return true
    }

    fun setMode(sender: CommandSender, mode:CameraMode){
        cameraMode = mode
        camera?.gameMode = GameMode.CREATIVE
        camera?.gameMode = GameMode.SPECTATOR
        camera?.spectatorTarget  = null
        when(cameraMode){
            CameraMode.SPECTATOR -> {
                info("${cameraName}:スペクテーターモード",sender)
                camera?.spectatorTarget = target
            }
            CameraMode.FOLLOW -> {
                info("${cameraName}:フォローモード",sender)
            }
            CameraMode.GOAROUND -> {
            }
            CameraMode.LOOK -> {
                info("${cameraName}:ルックモード",sender)
                onLookMode()
            }
            CameraMode.AUTO -> {
                onAutoMode()
            }
            else ->{
                onStopMode()
            }
        }
        save(sender)
    }

    fun setRelativePosition(sender: CommandSender,x:Double,y:Double,z:Double){
        relativePos = Vector(x,y,z)
        save(sender)
    }
    fun setRadius(sender: CommandSender,r:Double){
        radius = r
        if(radius < 2.0)
            radius = 2.0
        relativePos.x = radius
        save(sender)
    }

    fun setHeight(sender: CommandSender,h:Double){
        relativePos.y = h
        save(sender)
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
        info("${cameraName}: ${player.name}をカメラに設定しました",sender)
        save(sender)
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
//        camera?.spectatorTarget = target
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

        if(loc?.block?.type == Material.AIR){


        }

        teleport(loc)
    }

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
    fun setNightVision(sender:CommandSender,flag:Boolean){
        if(flag)
            camera?.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE,1,true))
        else
            camera?.removePotionEffect(PotionEffectType.NIGHT_VISION)
        nightVision = flag
        save(sender)
    }

    fun onGoAroundMode(){

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

    fun getCircle(center: Location, radius: Double, amount: Int): ArrayList<Location> {
        val world: World = center.world
        val increment = 2 * Math.PI / amount
        val locations: ArrayList<Location> = ArrayList<Location>()
        for (i in 0 until amount) {
            val angle = i * increment
            val x: Double = center.x + radius * cos(angle)
            val z: Double = center.z + radius * sin(angle)
            locations.add(Location(world, x, center.y, z))
        }
        return locations
    }
    fun toRadian(angle: Double): Double {
        return angle * Math.PI / 180f
    }
    fun toAngle(radian: Double): Double {
        return radian * 180 / Math.PI
    }
    private fun getAngle(player: Player): Double {
        var rotation = ((player.location.yaw - 90) % 360).toDouble()
        if (rotation < 0) {
            rotation += 360.0
        }
        return rotation
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
            target = Bukkit.getPlayer(UUID.fromString(config["target"].toString()))
            camera = Bukkit.getPlayer(UUID.fromString(config["camera"].toString()))
            cameraMode = enumValueOf(config["cameraMode"].toString())
            nightVision = config.getBoolean("nightVision")
            radius = config.getDouble("radius")
            if(radius < 2)
                radius = 10.0

            val gameMode = enumValueOf<GameMode>(config["gameMode"].toString())
            camera?.gameMode = gameMode
        }
        catch (e:Exception){
            error("カメラ設定の読み込みに失敗しました:${cameraName} / ${e.localizedMessage}",sender)
            return false
        }
        return true
    }

    // カメラ設定ファイルを削除
    companion object fun deleteFile(sender: CommandSender?, name: String): Boolean {
        val file = File(Main.plugin.dataFolder, "camera${File.separator}$name.yml")
        if (file.delete()) {
            info("${name}を削除しました",sender)
        } else {
            error("${name}を削除に失敗しました",sender)
        }
        return false
    }
    //endregion



}

