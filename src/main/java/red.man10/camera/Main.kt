package red.man10.camera

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityPickupItemEvent

class Main : JavaPlugin() ,Listener {
    companion object {
        val prefix = "§e[MCR]"
        lateinit var plugin: JavaPlugin
        lateinit var cameraThread1 : CameraThread
    }
    override fun onEnable() {
        plugin = this
        saveDefaultConfig()
        getCommand("mcs")!!.setExecutor(Command)

        plugin.server.pluginManager.registerEvents(this, plugin)

        cameraThread1 = CameraThread()
        cameraThread1.cameraName = "camera1"
        cameraThread1.load()
        cameraThread1.start()

        info("Man10 Camera Plugin Enabled")
    }

    override fun onDisable() {
        info("Disabling Man10 Camera Plugin")
        cameraThread1.running = false
    }
    @EventHandler
    fun onPickUp(e: EntityPickupItemEvent){
        val entity=e.entity
        if(entity !is Player)
            return
        //  カメラをアイテムを拾わない
        if(entity.uniqueId == cameraThread1.uniqueId)
            e.isCancelled = true
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


