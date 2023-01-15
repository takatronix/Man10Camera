package red.man10.camera

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.projectiles.ProjectileSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class Main : JavaPlugin() ,Listener {
    companion object {
        val broadcastMessage = "§c§lYoutubeライブ配信中!! §f§l->  §f§l§nhttps://www.youtube.com/@man10server/live"
        val notifyMessage = "§f§lさんを§c§lYoutubeでライブ配信中！！  §f§l§nhttps://www.youtube.com/@man10server/live"
        val prefix = "§e[MCR]"
        lateinit var plugin: JavaPlugin
        const val cameraCount = 4
        var mc1= CameraThread()
        var mc2= CameraThread()
        var mc3= CameraThread()
        var mc4= CameraThread()
        // プレイヤーの統計データ
        var playerData = ConcurrentHashMap<UUID, PlayerData>()
    }

    fun test(){
        var list = playerData.toList()
    }
    override fun onEnable() {
        plugin = this
        saveDefaultConfig()
        // カメラスレッド生成
        for(no in 1..cameraCount){
            val label = "mc$no"
            getCommand(label)!!.setExecutor(Command)
            val camera = getCamera(label)
            camera.cameraName = label
            camera.load()
            camera.start()
        }
        // リロード後のユーザーはjoinイベントがないためデータを作る必要がある
        Bukkit.getOnlinePlayers().forEach { player ->  playerData.putIfAbsent(player.uniqueId,PlayerData())}
        plugin.server.pluginManager.registerEvents(this, plugin)
        info("Man10 Camera Plugin Enabled")
    }

    override fun onDisable() {
        info("Disabling Man10 Camera Plugin")
        for(no in 1..cameraCount) {
            getCamera(no).running = false
        }
    }

    private fun isCamera(player:Player?):Boolean{
        if(player == null)
            return false
        for(no in 1..cameraCount) {
            if(getCamera(no).uniqueId == player.uniqueId){
                return true
            }
        }
        return false
    }
    private fun isTarget(player:Player?):Boolean{
        if(player == null)
            return false
        for(no in 1..cameraCount) {
            if(getCamera(no).targetUniqueId == player.uniqueId){
                return true
            }
        }
        return false
    }
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
        playerData[uuid]?.playerMoveCount = playerData[uuid]?.playerMoveCount!! + 1
        playerData[uuid]?.updateTime = System.currentTimeMillis()

    }
    @EventHandler
    fun onBlockBreak(e:BlockBreakEvent){
        val uuid = e.player.uniqueId
        playerData[uuid]?.blockBreakCount = playerData[uuid]?.blockBreakCount!! + 1
        playerData[uuid]?.updateTime = System.currentTimeMillis()
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent){
        val uuid = e.player.uniqueId
        playerData.putIfAbsent(uuid,PlayerData())
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        // ログアウトしたユーザーのデータは消去
        playerData.remove(e.player.uniqueId)
    }

    //      銃や弓などのダメージイベント
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


}
fun getCamera(label:String="mc1"):CameraThread{
    return when(label){
        "mc1" -> Main.mc1
        "mc2" -> Main.mc2
        "mc3" -> Main.mc3
        "mc4" -> Main.mc4
        else -> Main.mc1
    }
}
fun getCamera(no:Int=1):CameraThread{
    return when(no){
        1 -> Main.mc1
        2 -> Main.mc2
        3 -> Main.mc3
        4 -> Main.mc4
        else -> Main.mc1
    }
}
fun info(message:String,sender:CommandSender? = null){
    Bukkit.getLogger().info(Main.prefix+message)
    sender?.sendMessage(message)
}
fun error(message:String,sender:CommandSender? = null) {
    Bukkit.getLogger().severe(Main.prefix+message)
    sender?.sendMessage(message)
}


