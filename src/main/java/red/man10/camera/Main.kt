package red.man10.camera

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.projectiles.ProjectileSource
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread


class Main : JavaPlugin() ,Listener {

    private val teleportTasks = mutableMapOf<Player, TeleportTask>()

    companion object {
        val version = "2023/4/10"
        var commandSender: CommandSender? = null
        val youtubeMessage = "&c&lYoutubeライブ配信中!! &f&l->  &f&l&nhttps://www.youtube.com/@man10server/live"
        val bungeeLiveMessage = "&f&lさんを&c&lYoutubeでライブ配信中！！  &f&l&nhttps://www.youtube.com/@man10server/live"

        val broadcastMessage = "§c§lYoutubeライブ配信中!! §f§l->  §f§l§nhttps://www.youtube.com/@man10server/live"
        val liveMessage = "§f§lさんを§c§lYoutubeでライブ配信中！！  §f§l§nhttps://www.youtube.com/@man10server/live"
        val spectatoressage = "§f§lさんの視点を§c§lYoutubeでライブ配信中！！  §f§l§nhttps://www.youtube.com/@man10server/live"
        val prefix = "§e[MCX]"

        lateinit var plugin: JavaPlugin
        const val cameraCount = 4
        const val sleepDetect = 15000
        var mc1 = CameraThread()
        var mc2 = CameraThread()
        var mc3 = CameraThread()
        var mc4 = CameraThread()

        var kitManager = KitManager()
        var locationManager = LocationManager()

        // プレイヤーの統計データ
        var playerMap = ConcurrentHashMap<UUID, PlayerData>()
        var autoTask = false
        var running = true                      // スレッド終了フラグ
        var taskSwitchCount = 30                // 減算していき０になったらスイッチする


        lateinit var configData: ConfigData

    }

    override fun onEnable() {
        plugin = this
        saveDefaultConfig()
        configData = loadConfigData(config)
        showConfigData()

        getCommand("mc")!!.setExecutor(Command)
        getCommand("manbo")!!.setExecutor(Command)


        locationManager.name = "test";
        locationManager.load()

        // カメラスレッド生成
        for (no in 1..cameraCount) {
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
        Bukkit.getOnlinePlayers().forEach { player -> playerMap.putIfAbsent(player.uniqueId, PlayerData()) }
        // 自動処理スレッド
        thread {
            info("auto task thread started")
            while (running) {
                Thread.sleep(1000)
                taskSwitchCount--
                if (taskSwitchCount <= 0) {
                    taskSwitchCount = configData.switchTime
                    if (autoTask)
                        autoCameraTask()
                }

            }
            info("auto task thread ended")
        }


        // 1tick毎のタスクを設定する
        Bukkit.getServer().scheduler.scheduleSyncRepeatingTask(this, {
            for (no in 1..cameraCount) {
                getCamera(no).onTick()
            }
        }, 0L, 1L)



        plugin.server.pluginManager.registerEvents(this, plugin)
        info("Man10 Camera Plugin Enabled")
    }


    // 除外プレイヤーか(OPとカメラは監視からのぞく)
    fun isExclusionPlayer(uuid: UUID): Boolean {
        var player = Bukkit.getPlayer(uuid)
        if (player?.isOnline == false)
            return true
        if (player?.isOp == true)
            return true
        if (isCamera(player))
            return true
        return false
    }

    fun autoCameraTask() {

        info("自動切換えタスク")

        if (getCamera(1).cameraPlayer == null) {
            info("camera1がいないため自動処理は停止")
            return
        }

        // アクティブなプレイヤー順にソート
        val list = playerMap.toList().sortedByDescending { it.second.updateTime }
        val activeList: MutableList<PlayerData> = mutableListOf()
        //  アクティブな放送対象の
        activeList.clear()
        list.forEach { data: Pair<UUID, PlayerData> ->
            val uuid = data.first
            val pd = data.second
            pd.uuid = uuid

            if (!isExclusionPlayer(uuid)) {
                // アクティブかつ、現在表示してない対象
                if (pd.isActive() && !isTarget(Bukkit.getPlayer(uuid!!)))
                    activeList.add(pd)
            }
        }

        if (activeList.size == 0) {
            info("切替対象なし")
            return
        }
        //   info("アクティブなプレイヤー数:${activeList.size}")
        activeList.shuffle()

        // 次の表示対象
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val player = Bukkit.getPlayer(activeList[0].uuid!!)
            if (player != null) {
                sendBungeeMessage(commandSender!!, " &a&l" + player.name + bungeeLiveMessage)



                getCamera(1).rotate(null, player)

                // 3秒後にクローンモードに切り替え
                //Bukkit.getScheduler().runTaskLater(Main.plugin, Runnable {
                //    getCamera(1).clone(null, player)
                //}, 20L * 3)
            }

        })

    }

    override fun onDisable() {
        info("Disabling Man10 Camera Plugin")
        running = false
        for (no in 1..cameraCount) {
            getCamera(no).running = false
        }
    }

    fun test() {
        playerMap.toList()
    }

    //region イベントコールバック
    @EventHandler
    fun onPickUp(e: EntityPickupItemEvent) {
        val entity = e.entity
        if (entity !is Player)
            return
        //  カメラはアイテムを拾わない
        for (no in 1..cameraCount) {
            if (entity.uniqueId == getCamera(no).uniqueId)
                e.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        val entity = e.entity
        for (no in 1..cameraCount) {
            if (entity.uniqueId == getCamera(no).uniqueId) {
                e.isCancelled = true
                entity.sendMessage("カメラプレイヤーにされているため死亡をキャンセルしました")
                KitManager.load(entity, "manbo")
            }
        }
    }

    @EventHandler
    fun onPlayerMove(e: PlayerMoveEvent) {
        val uuid = e.player.uniqueId
        playerMap[uuid]?.playerMoveCount = playerMap[uuid]?.playerMoveCount!! + 1
        playerMap[uuid]?.updateTime = System.currentTimeMillis()
    }

    @EventHandler
    fun onPlayerFish(e: PlayerFishEvent) {
        val uuid = e.player.uniqueId
        playerMap[uuid]?.playerFishCount = playerMap[uuid]?.playerFishCount!! + 1
        playerMap[uuid]?.updateTime = System.currentTimeMillis()
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        val uuid = e.whoClicked.uniqueId
        playerMap[uuid]?.inventoryClickCount = playerMap[uuid]?.inventoryClickCount!! + 1
        playerMap[uuid]?.updateTime = System.currentTimeMillis()
    }

    @EventHandler
    fun onBlockBreak(e: BlockBreakEvent) {
        val uuid = e.player.uniqueId
        playerMap[uuid]?.blockBreakCount = playerMap[uuid]?.blockBreakCount!! + 1
        playerMap[uuid]?.updateTime = System.currentTimeMillis()
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        val uuid = e.player.uniqueId
        playerMap.putIfAbsent(uuid, PlayerData())

        if (isCamera(e.player)) {
            taskSwitchCount = 3

            Bukkit.getScheduler().runTaskLater(Main.plugin, Runnable {
                // ログインしたのがカメラプレイヤーなら外見を設定する
                val cam = getCamera(e.player.uniqueId)
                info("camera ${cam?.cameraPlayer?.name} logged in")
                cam?.setAppearance(null)
            }, 20L * 1)
        } else {
            if(e.player.isOp)
                return
            e.player.gameMode = GameMode.SURVIVAL
        }
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        // ログアウトしたユーザーのデータは消去
        playerMap.remove(e.player.uniqueId)
        e.player.gameMode = GameMode.SURVIVAL
    }



    fun setCameraAppearance(player: Player,health : Double) {
        if(health > 17)
            KitManager.load(player, "manbo")
        else if(health > 14.5)
            KitManager.load(player, "love")
        else if(health > 12)
            KitManager.load(player, "question")
        else if(health > 9.5)
            KitManager.load(player, "shock")
        else if(health > 7)
            KitManager.load(player, "angry")
        else if(health > 5)
            KitManager.load(player, "cry")
        else if(health > 2.5)
            KitManager.load(player, "sleep")
        else
            KitManager.load(player, "death")


    }

    @EventHandler
    fun onPlayerRegainHealth(event: EntityRegainHealthEvent) {
        val entity = event.entity
        if (entity is Player) {
            if(!isCamera(entity))
                return
            val amount = event.amount
            val healthBefore = entity.health
            val healthAfter = (healthBefore + amount).coerceAtMost(entity.maxHealth)

            println("プレイヤー ${entity.name}  が体力を回復しました: 回復量=$amount")
            println("回復前のヘルス: $healthBefore, 回復後のヘルス: $healthAfter")
            setCameraAppearance(entity,healthAfter)
        }
    }

    // 銃や弓などのダメージイベント
    @EventHandler
    fun onEntityDamage(e: EntityDamageByEntityEvent) {
        if (e.damager is Projectile && e.entity is Player) {
            //make sure the damager is a snowball & the damaged entity is a Player
            val shooter: ProjectileSource = (e.damager as Projectile).shooter as? Player ?: return
            val p = shooter as Player
            info("onEntityDamage プレイヤー ${p.name} がダメージを与えました: ${e.damage}")
        }
    }
    @EventHandler
    fun onPlayerDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity is Player) {
            if(!isCamera(entity))
                return
            val damage = event.damage
            val finalDamage = event.finalDamage
            val healthBefore = entity.health
            val healthAfter = (healthBefore - finalDamage).coerceAtLeast(0.0)
            info("onPlayerDamage プレイヤー ${entity.name} がダメージを受けました: 総ダメージ=$damage, 最終ダメージ=$finalDamage health:${healthAfter}")

            setCameraAppearance(entity,healthAfter)
        }
    }


    //endregion
    private inner class TeleportTask(
        val player: Player,
        val dest: Location,
        val lookLocation: Location?,
        val sec: Double
    ) : BukkitRunnable() {
        private val startLocation: Location = player.location.clone()
        private val startPitch: Float = player.location.pitch
        private val startYaw: Float = player.location.yaw
        private var tickCount = 0
        private val totalTicks = (sec * 20).toInt()

        override fun run() {
            tickCount++

            // 移動の割合を計算
            val ratio = tickCount.toDouble() / totalTicks.toDouble()

            // 新しい位置を計算
            val newX = startLocation.x + (dest.x - startLocation.x) * ratio
            val newY = startLocation.y + (dest.y - startLocation.y) * ratio
            val newZ = startLocation.z + (dest.z - startLocation.z) * ratio
            val newLocation = Location(player.world, newX, newY, newZ)

            if (lookLocation != null) {
                // 新しいpitch, yawを計算
                newLocation.direction = lookLocation.clone().subtract(newLocation).toVector()
            } else {
                // 旧pitch, yawを線形補間で計算
                newLocation.pitch = (startPitch + (dest.pitch - startPitch) * ratio.toFloat())
                newLocation.yaw = (startYaw + (dest.yaw - startYaw) * ratio.toFloat())
            }

            // プレイヤーを新しい位置に移動
            player.teleport(newLocation)

            // タスクが完了したら、登録から削除
            if (tickCount >= totalTicks) {
                cancel()
                teleportTasks.remove(player)
            }
        }
    }
}
fun sendBungeeCommand(sender:CommandSender,command:String){
    Bukkit.dispatchCommand(sender,"bungeee ${command}")
}
fun sendBungeeMessage(sender: CommandSender,message:String){
    if(Main.configData.broadcast)
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
fun getCamera(uuid:UUID):CameraThread?{
    return when(uuid){
        Main.mc1.uniqueId -> Main.mc1
        Main.mc2.uniqueId -> Main.mc2
        Main.mc3.uniqueId -> Main.mc3
        Main.mc4.uniqueId -> Main.mc4
        else -> null
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

fun loadConfigData(config: FileConfiguration): ConfigData {
    return ConfigData(
        broadcast = config.getBoolean("broadcast", false),
        switchTime = config.getInt("switchTime",30)
    )
}
fun saveConfigData(configData: ConfigData) {
    Main.plugin.config.set("broadcast", configData.broadcast)
    Main.plugin.config.set("switchTime",configData.switchTime)
    Main.plugin.saveConfig()
    showConfigData()
}
fun showConfigData(sender:CommandSender? = null){
    info("broadcast:${Main.configData.broadcast}")
    info("switchTime:${Main.configData.switchTime}")
}

