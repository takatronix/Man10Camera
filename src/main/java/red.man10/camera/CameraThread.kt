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

//region 定義
// カメラの動作モード
enum class CameraMode{
    STOP,                       // 自動
    AUTO,                       // 自動
    SPECTATOR,                  // スペクテーター
    LOOK,                       // 停止して対象を見る
    FOLLOW,                     // 追跡
    ROTATE,                     // 周囲を回る
}
// カメラの表示モード
enum class VisibleMode {
    HIDE,                       // 非表示(スペクテーター)
    SHOW,                       // 透明で頭だけ
    SHOWBODY                    // ボディも表示
}
//endregion
class CameraThread : Thread() {
    //region 内部変数
    private var wait:Long = 1000 / 60           // 更新サイクル
    private var target: UUID? = null            // 監視対象
    private var camera: UUID? = null            // カメラプレーヤ
    private var angle:Double = 0.0              // 現在のカメラの回転角度(0-360)
    private var isTargetOnline:Boolean = false
    //endregion
    //region 設定
    private var autoTarget:Boolean = true       // ターゲットを見失ったとき
    private var radius:Double = 10.0            // 回転半径
    private var angleStep = 0.08                // 回転速度
    private var nightVision = false             // 暗視設定
    private var broadcast = true                // 配信を全体に通知するか
    private var notification = true             // 配信を個人に通知するか
    private var relativePos:Vector= Vector(2.0,2.0,0.0)    // カメラの相対位置
    private var cameraMode:CameraMode = CameraMode.AUTO              // 動作モード
    private var visibleMode:VisibleMode = VisibleMode.SHOW           // 表示モード
    //endregion
    //region プロパティ
    var cameraLabel = ""                     // カメララベル
    var cameraName = ""                     // カメラ名称
    var running = true                      // スレッド終了フラグ

    private val cameraPlayer:Player?
        get() {
            if(camera == null)
                return null
            return Bukkit.getPlayer(camera!!)}
    private val targetPlayer:Player?
        get() {
            if(target == null)
                return null
            return Bukkit.getPlayer(target!!)}
    // カメラのUUID
    val uniqueId:UUID?
        get(){ return camera}
    val targetUniqueId:UUID?
        get(){ return target }
    // カメラの座標
    val cameraPos:Vector?
        get(){ return cameraPlayer?.location?.toVector() }
    // カメラの向き
    val cameraDir:Vector?
        get() { return cameraPlayer?.location?.direction }
    // ターゲットの座標
    private val targetPos:Vector?
        get() { return targetPlayer?.location?.toVector() }
    // ターゲットの向き
    val targetDir:Vector?
        get(){ return targetPlayer?.location?.direction }
    // ターゲット-カメラ間距離
    val targetDistance:Double?
        get(){ return targetPlayer?.location?.distance(cameraPlayer?.location!!) }
    // カメラ->ターゲットのベクトル
    private val toTargetVec:Vector?
        get(){ return targetPos?.subtract(cameraPos!!) }
    //endregion

    // スレッドメイン
    override fun run() {
        info("Camera thread started:${cameraLabel}")
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
        info("Camera thread ended:${cameraLabel}")
    }

    // ターゲットがオフラインになった
    private fun onTargetOffline(){
        if(autoTarget){
            info("ターゲットがオフラインのため切り替える")
        }
    }


    // スレッドが動作可能か？
    private fun canWork() :Boolean{
        when(cameraMode){
            // Lookモードは対象がなくてもOK
            CameraMode.LOOK -> {
                if(cameraPlayer?.isOnline == true)
                    return true
            }
            else -> {
                if(cameraPlayer?.isOnline == true && targetPlayer?.isOnline == true){
                    isTargetOnline = true
                    return true
                }else{
                    if(isTargetOnline){
                        isTargetOnline = false
                        onTargetOffline()
                    }
                }
            }
        }

        return false
    }
    private fun resetAllPostionEffects(player:Player){
        for (effect in player.activePotionEffects)
            player.removePotionEffect(effect.type)
    }
    // カメラモード設定
    private fun setMode(sender: CommandSender?,mode:CameraMode){
        cameraMode = mode
        // クリエイティブとスペクテーターを切り替えてスペクテーターターゲットを外す
        val camera = cameraPlayer
        camera?.gameMode = GameMode.CREATIVE
        camera?.gameMode = GameMode.SPECTATOR
        camera?.spectatorTarget  = null
        save(sender)

        resetAllPostionEffects(cameraPlayer!!)
        // スペクテーターモードの時はボディの表示は必要ない
        if(mode == CameraMode.SPECTATOR){
            return
        }
        // 表示モードに基づいて表示を合わせる
        when(visibleMode){
            VisibleMode.SHOWBODY -> showBody(sender)
            VisibleMode.SHOW -> show(sender)
            else -> VisibleMode.HIDE
        }
        setNightVision(sender,nightVision)
    }

    // 鯖にいるユーザーに通知する
    private fun notifyUsers(message:String,sender: CommandSender?, target:Player?){
        Bukkit.getOnlinePlayers().forEach {
            p ->
            run {
                if(target == null)
                    return
                if(broadcast)
                    p.sendMessage("§e§l"+cameraName+" §a§l"+ target.name +message)
                else{
                    if(notification && target == p)
                        p.sendMessage("§e§l"+cameraName+" §a§l"+ target.name +message)
                }
            }
        }
    }

    //region 基本コマンド
    // 特定プレイヤーを追跡
    fun follow(sender: CommandSender,player:Player? = null){
        setMode(sender,CameraMode.FOLLOW)
        if(player?.isOnline == true){
            target = player.uniqueId
        }
        info("${cameraLabel}をフォローモードに設定",sender)
        notifyUsers(Main.liveMessage,sender,player)
    }
    fun spectator(sender: CommandSender?, player:Player? = null){
        setMode(sender,CameraMode.SPECTATOR)
        save(sender)
        if(player?.isOnline == true){
            target = player.uniqueId
            cameraPlayer?.spectatorTarget = targetPlayer
        }
        info("${cameraLabel}をスペクテーターモードで監視",sender)
        notifyUsers(Main.spectatoressage,sender,player)
    }
    // 特定プレイヤーを回転しながら追跡
    fun rotate(sender: CommandSender?, player:Player? = null){
        setMode(sender,CameraMode.ROTATE)
        if(player?.isOnline == true){
            target = player.uniqueId
        }
        info("${cameraLabel}を回転モードに設定",sender)
        notifyUsers(Main.liveMessage,sender,player)
    }
    // カメラを固定でプレイヤーを注視
    fun look(sender: CommandSender,player:Player? = null){
        setMode(sender,CameraMode.ROTATE)
        if(player?.isOnline == true){
            target = player.uniqueId
        }
        info("${cameraLabel}をルックモードに設定",sender)
        notifyUsers(Main.liveMessage,sender,player)
    }
    // カメラ停止
    fun stop(sender: CommandSender,player:Player? = null){
        setMode(sender,CameraMode.STOP)
        info("${cameraLabel}を停止させました",sender)
    }
    //endregion

    // カメラの相対位置の設定
    fun setRelativePosition(sender: CommandSender,x:Double,y:Double,z:Double){
        relativePos = Vector(x,y,z)
        save(sender)
    }

    // 半径の設定
    fun setRadius(sender: CommandSender,r:Double){
        radius = r
        if(radius < 1.0)
            radius = 1.0
        relativePos.x = radius
        save(sender)
    }

    // 高さの設定
    fun setHeight(sender: CommandSender,h:Double){
        relativePos.y = h
        save(sender)
    }
    // カメラプレイヤーの設定
    fun setCamera(sender: CommandSender, name:String?):Boolean {
        val player = getOfflinePlayer(sender,name) ?: return false
        camera = player.player?.uniqueId
        info("${cameraLabel}: ${player.name}をカメラに設定しました",sender)
        save(sender)
        return true
    }

    // 監視対象の設定
    fun setTarget(sender: CommandSender, name:String?):Boolean {
        val player = getOfflinePlayer(sender,name) ?: return false
        target = player.player?.uniqueId
        info("${cameraLabel}${player.name}をターゲットに設定しました",sender)
        save(sender)
        return true
    }

    private fun onAutoMode(){
    }
    private fun onStopMode(){
    }
    private fun onSpectatorMode(){
    }
    fun onLookMode(){
        lookAt(targetPos)
    }
    private fun onFollowMode(){
        // ターゲットの相対位置のカメラ位置
        val loc = targetPlayer?.location?.add(relativePos)
        val pos = loc?.toVector()
        val dir = targetPos?.subtract(pos!!)
        loc?.direction = dir!!
        // TODO:カメラがブロックとかぶっていたら、距離を近づける
        if(loc?.block?.type == Material.AIR){

        }
        teleport(loc)
    }

    //region 表示モード
    fun hide(sender:CommandSender) {
        val camera = cameraPlayer
        camera?.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE, 1, true))
        camera?.gameMode = GameMode.SPECTATOR
        visibleMode = VisibleMode.HIDE
        save(sender)
    }
    fun show(sender:CommandSender?){
        val camera = cameraPlayer
        camera?.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE,1,true))
        camera?.gameMode = GameMode.CREATIVE
        visibleMode = VisibleMode.SHOW
        save(sender)
    }
    fun showBody(sender:CommandSender?){
        val camera = cameraPlayer
        camera?.removePotionEffect(PotionEffectType.INVISIBILITY)
        camera?.gameMode = GameMode.CREATIVE
        visibleMode = VisibleMode.SHOWBODY
        save(sender)
    }
    //endregion

    fun setNightVision(sender:CommandSender?,flag:Boolean){
        val camera = cameraPlayer
        if(flag)
            camera?.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE,1,true))
        else
            camera?.removePotionEffect(PotionEffectType.NIGHT_VISION)
        nightVision = flag
        save(sender)
        info("ナイトビジョンを{$flag}にしました",sender)
    }
    fun setBroadcast(sender:CommandSender?,flag:Boolean){
        broadcast = flag
        save(sender)
        info("全体通知を{$flag}にしました",sender)
    }
    fun setNotification(sender:CommandSender,flag:Boolean){
        notification = flag
        save(sender)
        info("個人通知を{$flag}にしました",sender)
    }
    private fun onRotateMode(){
        // ターゲットの相対位置のカメラ位置
        val loc = targetPlayer?.location?.add(relativePos)
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
        val loc = cameraPlayer?.location
        loc?.direction = toTargetVec!!
        teleport(loc)
    }
    // テレポートする
    private fun teleport(loc:Location?){
        Bukkit.getScheduler().runTask(Main.plugin, Runnable {
            val camera = cameraPlayer
            if(loc != null && camera != null && camera.isOnline){
                camera.teleport(loc)
            }
        })
    }
    //region ファイル管理
    private fun save(sender:CommandSender?=null): Boolean {
        val file = File(Main.plugin.dataFolder, "camera${File.separator}$cameraLabel.yml")
        info("saving ${file.absolutePath}")
        try{
            val config = YamlConfiguration.loadConfiguration(file)
            config["target"] = target?.toString()
            config["camera"] = camera?.toString()
            config["cameraMode"] = cameraMode.toString()
            config["visibleMode"] = visibleMode.toString()
            config["gameMode"] = cameraPlayer?.gameMode.toString()
            config["radius"] = radius
            config["nightVision"] = nightVision
            config["broadcast"] = broadcast
            config["notification"] = notification
            config.save(file)
        }
        catch (e:Exception){
            error("カメラ設定の保存に失敗しました:${cameraLabel} / ${e.localizedMessage}",sender)
            return false
        }
        info("${file.path}に保存")
        return true
    }

    fun load(sender:CommandSender? = null): Boolean {
        val file = File(Main.plugin.dataFolder, "camera${File.separator}$cameraLabel.yml")
        info("loading ${file.absolutePath}")
        try{
            val config = YamlConfiguration.loadConfiguration(file)

            // target UUID
            var s = config.getString("target")
            if(s != null)
                target = UUID.fromString(s)
            // camera UUID
            s = config.getString("camera")
            if(s != null)
                camera = UUID.fromString(s)

            cameraMode = enumValueOf(config["cameraMode"].toString())
            visibleMode = enumValueOf(config["visibleMode"].toString())
            nightVision = config.getBoolean("nightVision")
            broadcast = config.getBoolean("broadcast")
            notification = config.getBoolean("notification")
            // 回転半径
            radius = config.getDouble("radius")
            if(radius < 1.0)
                radius = 1.0

            val gameMode = enumValueOf<GameMode>(config["gameMode"].toString())
            cameraPlayer?.gameMode = gameMode
        }
        catch (e:Exception){
            error("カメラ設定の読み込みに失敗しました:${cameraLabel} / ${e.localizedMessage}",sender)
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

