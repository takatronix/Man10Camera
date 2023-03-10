package red.man10.camera

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.projectiles.ProjectileSource
import red.man10.kit.Kit
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class Main : JavaPlugin() ,Listener {
    companion object {
        var commandSender:CommandSender?= null
        val youtubeMessage = "&c&lYoutubeライブ配信中!! &f&l->  &f&l&nhttps://www.youtube.com/@man10server/live"
        val bungeeLiveMessage = "&f&lさんを&c&lYoutubeでライブ配信中！！  &f&l&nhttps://www.youtube.com/@man10server/live"

        val broadcastMessage = "§c§lYoutubeライブ配信中!! §f§l->  §f§l§nhttps://www.youtube.com/@man10server/live"
        val liveMessage = "§f§lさんを§c§lYoutubeでライブ配信中！！  §f§l§nhttps://www.youtube.com/@man10server/live"
        val spectatoressage = "§f§lさんの視点を§c§lYoutubeでライブ配信中！！  §f§l§nhttps://www.youtube.com/@man10server/live"
        val prefix = "§e[MCX]"

        lateinit var plugin: JavaPlugin
        const val cameraCount = 4
        const val sleepDetect = 15000
        var mc1= CameraThread()
        var mc2= CameraThread()
        var mc3= CameraThread()
        var mc4= CameraThread()

        var kit = Kit()

        // プレイヤーの統計データ
        var playerMap = ConcurrentHashMap<UUID, PlayerData>()
        var autoTask = false
        var running = true                      // スレッド終了フラグ
        var taskSwitchTime = 30                 // タスク切替タイミング
        var taskSwitchCount = 30                // 減算していき０になったらスイッチする
        var broadcast:Boolean = false
        var serverName:String? = null           // サーバ名
        //var config: FileConfiguration
    }

    override fun onEnable() {
        plugin = this
        saveDefaultConfig()
        load()
        // カメラスレッド生成
        for(no in 1..cameraCount){
            val label = "mc$no"
            getCommand(label)!!.setExecutor(Command)
            val camera = getCamera(label)
            camera.cameraLabel = label
            camera.load()
            camera.start()
        }
        mc1.cameraName = "[メインカメラ]"
        mc2.cameraName = "[サブカメラ]"
        mc3.cameraName = "[カメラ３]"
        mc4.cameraName = "[カメラ４]"

        // リロード後のユーザーはjoinイベントがないためデータを作る必要がある
        Bukkit.getOnlinePlayers().forEach { player ->  playerMap.putIfAbsent(player.uniqueId,PlayerData())}
        // 自動処理スレッド
        thread {
            info("auto task thread started")
            while(running){
                Thread.sleep(1000)
                taskSwitchCount --
                if(taskSwitchCount <= 0){
                    taskSwitchCount = taskSwitchTime
                    if(autoTask)
                        autoCameraTask()
                }

            }
            info("auto task thread ended")
        }

        plugin.server.pluginManager.registerEvents(this, plugin)
        info("Man10 Camera Plugin Enabled")
    }


    // 除外プレイヤーか(OPとカメラは監視からのぞく)
    fun isExclusionPlayer(uuid:UUID):Boolean{
        var player = Bukkit.getPlayer(uuid)
        if(player?.isOnline == false)
            return true
        if(player?.isOp == true)
            return true
        if(isCamera(player))
            return true
        return false
    }
    fun autoCameraTask(){

        info("自動切換えタスク")

        if(getCamera(1).cameraPlayer == null){
            info("camera1がいないため自動処理は停止")
            return
        }
        /*
        if(getCamera(2).cameraPlayer == null){
            info("camera2がいないため自動処理は停止")
            return
        }
*/
        // アクティブなプレイヤー順にソート
        var list = playerMap.toList().sortedByDescending { it.second.updateTime }
        var activeList:MutableList<PlayerData>  = mutableListOf()
        //  アクティブな放送対象の
        activeList.clear()
        list.forEach {data: Pair<UUID, PlayerData> ->
            val uuid = data.first
            val pd = data.second
            pd.uuid = uuid

            if(!isExclusionPlayer(uuid)){
                // アクティブかつ、現在表示してない対象
                if(pd.isActive() && !isTarget(Bukkit.getPlayer(uuid!!)))
                   activeList.add(pd)
            }
        }

        if(activeList.size == 0){
            info("切替対象なし")
            return
        }
     //   info("アクティブなプレイヤー数:${activeList.size}")
        activeList.shuffle()

        // 次の表示対象
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val player = Bukkit.getPlayer(activeList[0].uuid!!)
            if (player != null) {
                sendBungeeMessage(commandSender!!," &a&l"+ player.name + bungeeLiveMessage)
               // getCamera(1).clone(null,player)
                getCamera(1).backView(null,player)
                getCamera(2).spectate(null,player)
             //   val text = "巡回中:${player.name} / Online:${Bukkit.getOnlinePlayers().count()} / Active:${activeList.size}"
               // getCamera(1).actionText = text
            }
        })

    }

    override fun onDisable() {
        info("Disabling Man10 Camera Plugin")
        running = false
        for(no in 1..cameraCount) {
            getCamera(no).running = false
        }
    }
    fun test(){
        playerMap.toList()
    }
    //region イベントコールバック
    @EventHandler
    fun onPickUp(e: EntityPickupItemEvent){
        val entity=e.entity
        if(entity !is Player)
            return
        //  カメラはアイテムを拾わない
        for(no in 1..cameraCount) {
            if(entity.uniqueId == getCamera(no).uniqueId)
                e.isCancelled = true
        }
    }
    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent){
        val entity=e.entity
        for(no in 1..cameraCount) {
            if(entity.uniqueId == getCamera(no).uniqueId){
                e.isCancelled = true
                entity.sendMessage("カメラプレイヤーにされているため死亡をキャンセルしました")
            }
        }
    }
    @EventHandler
    fun onPlayerMove(e: PlayerMoveEvent){
        val uuid = e.player.uniqueId
        playerMap[uuid]?.playerMoveCount = playerMap[uuid]?.playerMoveCount!! + 1
        playerMap[uuid]?.updateTime = System.currentTimeMillis()
    }
    @EventHandler
    fun onPlayerFish(e: PlayerFishEvent){
        val uuid = e.player.uniqueId
        playerMap[uuid]?.playerFishCount = playerMap[uuid]?.playerFishCount!! + 1
        playerMap[uuid]?.updateTime = System.currentTimeMillis()
    }
    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent){
        val uuid = e.whoClicked.uniqueId
        playerMap[uuid]?.inventoryClickCount = playerMap[uuid]?.inventoryClickCount!! + 1
        playerMap[uuid]?.updateTime = System.currentTimeMillis()
    }
    @EventHandler
    fun onBlockBreak(e:BlockBreakEvent){
        val uuid = e.player.uniqueId
        playerMap[uuid]?.blockBreakCount = playerMap[uuid]?.blockBreakCount!! + 1
        playerMap[uuid]?.updateTime = System.currentTimeMillis()
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent){
        val uuid = e.player.uniqueId
        playerMap.putIfAbsent(uuid,PlayerData())

        if(isCamera(e.player)){
            taskSwitchCount = 3
        }
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        // ログアウトしたユーザーのデータは消去
        playerMap.remove(e.player.uniqueId)
    }

    // 銃や弓などのダメージイベント
    @EventHandler
    fun onEntityDamage(e: EntityDamageByEntityEvent) {
        if (e.damager is Projectile && e.entity is Player) {
            //make sure the damager is a snowball & the damaged entity is a Player
            val shooter: ProjectileSource = (e.damager as Projectile).shooter as? Player ?: return
            val p = shooter as Player

        }
    }
    //      ヒットダメージ等
    @EventHandler
    fun PlayerDamageReceive(e: EntityDamageByEntityEvent) {
        if (e.entity is Player) {
            val damagedPlayer = e.entity as Player
        }
    }
    //endregion

}

fun sendBungeeCommand(sender:CommandSender,command:String){
    Bukkit.dispatchCommand(sender,"bungeee ${command}")
}
fun sendBungeeMessage(sender: CommandSender,message:String){
    Bukkit.dispatchCommand(sender,"bungeee alert ${message}")
}

//region 共通関数
// ラベルからカメラを取得
fun getCamera(label:String="mc1"):CameraThread{
    return when(label){
        "mc1" -> Main.mc1
        "mc2" -> Main.mc2
        "mc3" -> Main.mc3
        "mc4" -> Main.mc4
        else -> Main.mc1
    }
}
// 番号からカメラを取得
fun getCamera(no:Int=1):CameraThread{
    return when(no){
        1 -> Main.mc1
        2 -> Main.mc2
        3 -> Main.mc3
        4 -> Main.mc4
        else -> Main.mc1
    }
}
// 通常ログ
fun info(message:String,sender:CommandSender? = null){
    Bukkit.getLogger().info(Main.prefix+message)
    sender?.sendMessage(message)
}
// エラーログ
fun error(message:String,sender:CommandSender? = null) {
    Bukkit.getLogger().severe(Main.prefix+message)
    sender?.sendMessage(message)
}

private fun isCamera(player:Player?):Boolean{
    if(player == null)
        return false
    for(no in 1..Main.cameraCount) {
        if(getCamera(no).uniqueId == player.uniqueId){
            return true
        }
    }
    return false
}
private fun isTarget(player:Player?):Boolean{
    if(player == null)
        return false
    for(no in 1..Main.cameraCount) {
        if(getCamera(no).targetUniqueId == player.uniqueId){
            return true
        }
    }
    return false
}
//endregion

private fun save(sender: CommandSender?=null): Boolean {

    return true
}

fun load(sender: CommandSender? = null): Boolean {

    return true
}

