package red.man10.camera
import org.bukkit.*
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
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
    CLONE,                      // インベントリの同期
    BACK,                       // 背後（左右だけあわせる）
    BACK_VIEW,                  // 背後から視線も合わせる

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
    private var workingCounter = 0
    private var isInBlockLast = false
    //endregion
    //region 設定
    private var autoTarget:Boolean = true       // ターゲットを見失ったとき
    private var radius:Double = 5.0            // 回転半径
    private var height:Double = 2.0
    private var angleStep = 0.08                // 回転速度
    private var nightVisionFlag = false           // 暗視設定
    private var costume:String = ""
    // private var broadcast = true                // 配信を全体に通知するか
    private var notificationFlag = true             // 配信を個人に通知するか
    private var titleFlag = true             // 配信を個人に通知するか

    private var relativePos:Vector= Vector(5.0,2.0,0.0)    // カメラの相対位置
    private var cameraMode:CameraMode = CameraMode.AUTO              // 動作モード
    private var visibleMode:VisibleMode = VisibleMode.SHOW           // 表示モード


    //endregion
    //region プロパティ
    var cameraLabel = ""                     // カメララベル
    var cameraName = ""                     // カメラ名称
    var running = true                      // スレッド終了フラグ
    var actionText:String = ""
    public val cameraPlayer:Player?
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
    private val cameraPos:Vector?
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
            Bukkit.getScheduler().runTask(Main.plugin, Runnable {
                sendActionText(cameraPlayer,actionText)
            })

            if(!canWork())
                continue

            // カメラがブロックとかさなっているか検出
            Bukkit.getScheduler().runTask(Main.plugin, Runnable {
                val isInBlock = isInBlock()
                if(isInBlockLast != isInBlock){
                    if(isInBlock){
                        angleStep *= -1
                        onEnterBlock()
                    }else{
                        onExitBlock()
                    }
                    isInBlockLast = isInBlock
                }
            })

            when(cameraMode){
                CameraMode.AUTO -> onAutoMode()
                CameraMode.SPECTATOR -> onSpectatorMode()
                CameraMode.FOLLOW -> onFollowMode()
                CameraMode.ROTATE -> onRotateMode()
                CameraMode.LOOK -> onLookMode()
                CameraMode.CLONE -> onCloneMode()
                CameraMode.BACK -> onBackMode()
                CameraMode.BACK_VIEW -> onBackViewMode()
                else-> onStopMode()
            }
            workingCounter++
        }
        info("Camera thread ended:${cameraLabel}")
    }

    // tick毎イベント
    fun onTick(){




    }

    fun onEnterBlock(){
        info("${cameraLabel}がブロックにはいった")
        /*
        if(visibleMode == VisibleMode.HIDE)
            return

        Bukkit.getScheduler().runTask(Main.plugin, Runnable {
            cameraPlayer?.gameMode = GameMode.SPECTATOR
        })*/
    }
    fun onExitBlock(){
        info("${cameraLabel}がブロックからぬけた")
        /*
        if(visibleMode == VisibleMode.HIDE)
            return

        Bukkit.getScheduler().runTask(Main.plugin, Runnable {
            cameraPlayer?.gameMode = GameMode.CREATIVE
        })*/
    }

    // ターゲットがオフラインになった
    private fun onTargetOffline(){
        if(autoTarget){
            info("ターゲットがオフラインのため切り替える")
            Main.taskSwitchCount = 0
        }
    }
    public fun changeMode(mode:CameraMode){
        cameraMode = mode
    }
    fun showModeTitle(title:String, subtitle:String="", fadeIn:Int = 10, stay:Int=100, fadeOut:Int = 10){
        if(titleFlag)
            cameraPlayer?.sendTitle(title,subtitle,fadeIn,stay,fadeOut)
    }
    fun sendTitle(title:String, subtitle:String="",time:Double = 3.0){
        Bukkit.getScheduler().runTask(Main.plugin, Runnable {
            val tick = time * 20
            cameraPlayer?.sendTitle(title.replace("&","§"),subtitle.replace("&","§"),10,tick.toInt(),10)
        })
    }
    fun sendText(text:String,time:Double = 3.0){

        this.actionText = text.replace("&","§")
        val tick = time * 20
        Bukkit.getScheduler().runTaskLater(Main.plugin, Runnable { actionText = ""}, tick.toLong())
    }


    // スレッドが動作可能か？
    fun canWork() :Boolean{
        when(cameraMode){
            // Lookモードは対象がなくてもOK
            CameraMode.LOOK -> {
                if(cameraPlayer?.isOnline == true)
                    return true
            }
            CameraMode.STOP -> return false
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
    private fun setMode(sender: CommandSender?,mode:CameraMode,specTarget:Player?= null){
        cameraMode = mode

        // クリエイティブとスペクテーターを切り替えてスペクテーターターゲットを外す
        Bukkit.getScheduler().runTask(Main.plugin, Runnable {
            when (mode) {
                CameraMode.CLONE -> {
                    cameraPlayer?.gameMode = GameMode.SURVIVAL
                }
                CameraMode.SPECTATOR -> {
                    cameraPlayer?.gameMode = GameMode.CREATIVE
                    cameraPlayer?.gameMode = GameMode.SPECTATOR
                    cameraPlayer?.spectatorTarget = specTarget
                }
                CameraMode.BACK_VIEW -> {
                    cameraPlayer?.gameMode = GameMode.SPECTATOR
                }
                else -> {
                    cameraPlayer?.gameMode = GameMode.CREATIVE
                }
            }

        })
        setAppearance(sender)
        setNightVision(sender,nightVisionFlag)
    }
    fun setAppearance(sender: CommandSender?){
        // 表示モードに基づいて表示を合わせる
        info("visibleMode:$visibleMode")
//        cameraPlayer?.inventory.item
        when(visibleMode){
            VisibleMode.SHOWBODY -> showBody(sender)
            VisibleMode.SHOW -> show(sender)
            VisibleMode.HIDE -> hide(sender)
        }
    }

    // 鯖にいるユーザーに通知する
    private fun notifyUsers(message:String,sender: CommandSender?, target:Player?){
        if(sender == null)
            return
        Bukkit.getOnlinePlayers().forEach {
            p ->
            run {
                if(target == null)
                    return
                if(Main.configData.broadcast)
                    p.sendMessage("§e§l"+cameraName+" §a§l"+ target.name +message)
                else{
                    if(notificationFlag && target == p)
                        p.sendMessage("§e§l"+cameraName+" §a§l"+ target.name +message)
                }
            }
        }
    }

    fun canStart(sender:CommandSender?):Boolean{
        if(cameraPlayer == null){
            error("カメラなし",sender)
            return false
        }
        if(!cameraPlayer!!.isOnline){
            error("カメラはオフライン",sender)
            return false
        }

        if(targetPlayer == null){
            error("ターゲットなし",sender)
            return false
        }
        if(!targetPlayer!!.isOnline){
            error("ターゲットはオフライン",sender)
            return false
        }
        return true
    }

    //region 基本コマンド
    // 特定プレイヤーを追跡
    fun follow(sender: CommandSender?,player:Player? = null) {
        target = player?.uniqueId
        if (!canStart(sender))
            return
        setMode(sender, CameraMode.FOLLOW)
        if (player?.isOnline == true) {
            target = player.uniqueId
        }

        // 対象の視線ベクトル
        val vec = targetPlayer!!.location.direction.clone()
        vec.y = 0.0
        vec.normalize()
        vec.multiply(-1 * radius)
        vec.y = height

        this.relativePos = vec

        showCamera();

        info("${player!!.name}をフォローモードに設定", sender)
        notifyUsers(Main.liveMessage, sender, player)
        showModeTitle("§e§l${targetPlayer?.name}§f§lさんを§b§l配信中")
    }


    fun back(sender: CommandSender?,player:Player? = null){
        target = player?.uniqueId
        if(!canStart(sender))
            return

        setMode(sender,CameraMode.BACK)
        if(player?.isOnline == true){
            target = player.uniqueId
        }

        // 対象の視線ベクトル
        val vec = targetPlayer!!.location.direction.clone()
        vec.y = 0.0
        vec.normalize()
        vec.multiply(-1 * radius)
        vec.y = height
        this.relativePos = vec

        showCamera();

        info("${player!!.name}を背後モードに設定",sender)
        notifyUsers(Main.liveMessage,sender,player)
        showModeTitle("§e§l${targetPlayer?.name}§f§lさんを§b§l配信中")
    }
    fun backView(sender: CommandSender?,player:Player? = null){
        target = player?.uniqueId
        if(!canStart(sender))
            return

        setMode(sender,CameraMode.BACK_VIEW)
        if(player?.isOnline == true){
            target = player.uniqueId
        }

        // 対象の視線ベクトル
        val vec = targetPlayer!!.location.direction.clone()
        vec.normalize()
        vec.multiply(-1 * radius)
        vec.y = height
        this.relativePos = vec

        showCamera();


        info("${player!!.name}を背後Viewモードに設定",sender)
        notifyUsers(Main.liveMessage,sender,player)
        showModeTitle("§e§l${targetPlayer?.name}§f§lさんを§b§l配信中")
    }

    fun clone(sender: CommandSender?,player:Player? = null){
        target = player?.uniqueId
        if(!canStart(sender))
            return
        setMode(sender,CameraMode.CLONE)
        if(player?.isOnline == true){
            target = player.uniqueId
        }

        showCamera()
        // targetにCameraをうつさないようにする
        targetPlayer?.hidePlayer(Main.plugin,cameraPlayer!!)
        cameraPlayer?.hidePlayer(Main.plugin,targetPlayer!!)
        info("${player!!.name}をクローンモードに設定",sender)
        notifyUsers(Main.liveMessage,sender,player)
        showModeTitle("§e§l${targetPlayer?.name}§f§lさんを§b§l配信中")
    }

    fun showCamera(){
        Bukkit.getOnlinePlayers().forEach { p ->
            p.showPlayer(Main.plugin,cameraPlayer!!)
            cameraPlayer?.showPlayer(Main.plugin,p)
        }
    }

    fun hideCamera(){
        Bukkit.getOnlinePlayers().forEach { p ->
            p.hidePlayer(Main.plugin,cameraPlayer!!)
            cameraPlayer?.hidePlayer(Main.plugin,p)
        }
    }
    fun spectate(sender: CommandSender?, player:Player? = null){
        target = player?.uniqueId
        setMode(sender,CameraMode.SPECTATOR,targetPlayer)
        if(!canStart(sender))
            return

        showCamera();
        showModeTitle("§d§l${targetPlayer?.name}§f§lさんの視点")
        info("${player!!.name}をスペクテーターモードで監視",sender)
        notifyUsers(Main.spectatoressage,sender,player)
    }
    // 特定プレイヤーを回転しながら追跡
    fun rotate(sender: CommandSender?, player:Player? = null){
        target = player?.uniqueId
        if(!canStart(sender))
            return
        setMode(sender,CameraMode.ROTATE)
        if(player?.isOnline == true){
            target = player.uniqueId
        }
        showCamera();
        info("${cameraLabel}を回転モードに設定",sender)
        notifyUsers(Main.liveMessage,sender,player)
        showModeTitle("§a§l${targetPlayer?.name}§f§lさんを§b§l配信中")
    }
    // カメラを固定でプレイヤーを注視
    fun look(sender: CommandSender,player:Player? = null){
        setMode(sender,CameraMode.LOOK)
        if(player?.isOnline == true){
            target = player.uniqueId
        }
        info("${cameraLabel}をルックモードに設定",sender)
        notifyUsers(Main.liveMessage,sender,player)
        showModeTitle("§e§l${targetPlayer?.name}§f§lさんを§b§l配信中")
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
        height = h
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

        Bukkit.getScheduler().runTask(Main.plugin, Runnable {
            var camera = cameraPlayer
            var target = targetPlayer
            if (camera == null)
                return@Runnable
            if (target == null)
                return@Runnable

            var retarget = false
            if(cameraPlayer?.world != target.world){
                info("プレイヤーがワールド移動した -> 再登録")
                retarget = true
            }
            if(retarget){
                info("スペクテーターモード中にカメラから距離がはなれたため、スペクテーター解除されたと判断")
                camera.gameMode = GameMode.CREATIVE
                camera.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE,10,true,false))
                camera.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE,10,true,false))
                camera.gameMode = GameMode.SPECTATOR
                camera.spectatorTarget = targetPlayer

                val loc = targetPlayer?.location
                camera.teleport(loc!!)
            }

        })
    }

    fun isInBlock():Boolean{

        if(cameraPlayer?.isOnline == true){
            if(cameraPlayer?.location?.block?.type == Material.AIR){
                return  false
            }
        }
        return true
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
        teleport(loc)
    }
    private fun onBackMode(){

        // 対象の視線ベクトル
        val vec = targetPlayer!!.location.direction.clone()
        vec.y = 0.0
        vec.normalize()
        vec.multiply(-1 * radius)
        vec.y = height
        this.relativePos = vec

        // ターゲットの相対位置のカメラ位置
        val loc = targetPlayer?.location?.add(relativePos)
        val pos = loc?.toVector()
        val dir = targetPos?.subtract(pos!!)
        loc?.direction = dir!!
        teleport(loc)
    }
    private fun onBackViewMode(){

        // 対象の視線ベクトル
        val vec = targetPlayer!!.location.direction.clone()
        vec.normalize()
        vec.multiply(-1 * radius)
        vec.y = height
        this.relativePos = vec

        // ターゲットの相対位置のカメラ位置
        val loc = targetPlayer?.location?.add(relativePos)
        val pos = loc?.toVector()
        val dir = targetPos?.subtract(pos!!)
        loc?.direction = dir!!
        teleport(loc)
    }
    private fun onCloneMode(){

        // ターゲットの相対位置のカメラ位置
        val loc = targetPlayer?.location
        val pos = loc?.toVector()
        val dir = targetPos?.subtract(pos!!)
       // loc?.direction = dir!!

        Bukkit.getScheduler().runTask(Main.plugin, Runnable {

            val content = targetPlayer?.inventory?.contents
            (content as Array<out ItemStack>?)?.let { cameraPlayer?.inventory?.setContents(it) }
            cameraPlayer?.inventory?.heldItemSlot = targetPlayer?.inventory?.heldItemSlot!!

            if(targetPlayer?.health!! > 0)
                cameraPlayer?.health = targetPlayer?.health!!
            cameraPlayer?.foodLevel = targetPlayer?.foodLevel!!
            cameraPlayer?.exp = targetPlayer?.exp!!
            cameraPlayer?.totalExperience = targetPlayer?.totalExperience!!
            cameraPlayer?.level = targetPlayer?.level!!

            val camera = cameraPlayer
            if(loc != null && camera != null && camera.isOnline){
                camera.teleport(loc)
            }
        })
    }

    //region 表示モード
    fun hide(sender:CommandSender? = null) {
        val camera = cameraPlayer
        camera?.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE, 10, true,false))
        camera?.gameMode = GameMode.SPECTATOR
        visibleMode = VisibleMode.HIDE
    }
    fun show(sender:CommandSender?){
        showCamera()
        val camera = cameraPlayer
        camera?.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE,10,true,false))
        camera?.gameMode = GameMode.CREATIVE
        visibleMode = VisibleMode.SHOW
    }
    fun showBody(sender:CommandSender?){
        showCamera()
        val camera = cameraPlayer
        camera?.removePotionEffect(PotionEffectType.INVISIBILITY)
        camera?.gameMode = GameMode.CREATIVE
        visibleMode = VisibleMode.SHOWBODY
    }
    //endregion

    fun setNightVision(sender:CommandSender?,flag:Boolean){
        val camera = cameraPlayer
        if(flag)
            camera?.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE,10,true,false))
        else
            camera?.removePotionEffect(PotionEffectType.NIGHT_VISION)

        if(camera?.isOnline == false){
            error("カメラがオンラインではないのでナイトビジョンにできない",sender)
        }
        nightVisionFlag = flag
        info("{$cameraName}ナイトビジョンを{$flag}にしました",sender)
        save(sender)
    }
    fun setNotification(sender:CommandSender,flag:Boolean){
        notificationFlag = flag
        info("{$cameraName}個人通知を{$flag}にしました",sender)
        save(sender)
    }
    fun setTitleFlag(sender:CommandSender,flag:Boolean){
        titleFlag = flag
        info("{$cameraName}タイトル表示を{$flag}にしました",sender)
        save(sender)
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
        // ターゲットの位置 -> カメラのベクトルを求める
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
            config["height"] = height
            config["nightVisionFlag"] = nightVisionFlag
            config["notificationFlag"] = notificationFlag
            config["titleFlag"] = titleFlag
            config["costume"] = costume

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
            var s = config.getString("target",null)
            if(s != null) target = UUID.fromString(s)
            // camera UUID
            s = config.getString("camera",null)
            if(s != null) camera = UUID.fromString(s)
            cameraMode = enumValueOf(config["cameraMode"].toString())
            visibleMode = enumValueOf(config["visibleMode"].toString())
            nightVisionFlag = config.getBoolean("nightVisionFlag",true)
            notificationFlag = config.getBoolean("notificationFlag",true)
            titleFlag = config.getBoolean("titleFlag",true)

            // 回転半径
            radius = config.getDouble("radius",5.0)
            height = config.getDouble("height",2.0)

            costume = config.getString("costume",null).toString()
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

