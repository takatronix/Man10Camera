package red.man10.kit

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import red.man10.camera.Main
import red.man10.camera.info
import java.io.File

class Kit {

    //      キットを保存する
    fun save(p: Player, kitName: String): Boolean {
        val userdata = File(Main.plugin.dataFolder, File.separator + "kits")
        val f = File(userdata, File.separator + kitName + ".yml")
        val data: FileConfiguration = YamlConfiguration.loadConfiguration(f)
        if (!f.exists()) {
            try {
                data["creator"] = p.name
                data["inventory"] = p.inventory.contents
                data["armor"] = p.inventory.armorContents
                data.save(f)
                info( "キットを保存しました:$kitName",p)
            } catch (exception: Exception) {
                error("キットの保存に失敗した" + exception.message)
            }
        }
        return true
    }

    //      キットを読み込む
    fun load(p: Player, kitName: String): Boolean {
        val userdata =
            File(Bukkit.getServer().pluginManager.getPlugin("Kit")!!.dataFolder, File.separator + "Kits")
        val f = File(userdata, File.separator + kitName + ".yml")
        val data: FileConfiguration = YamlConfiguration.loadConfiguration(f)
        if (!f.exists()) {
            p.sendMessage("キットは存在しない:$kitName")
            return false
        }
        val inv = data["inventory"]
        val arm = data["armor"]
        if (inv == null || arm == null) {
            error("保存されたインベントリがない$kitName")
            return true
        }
        var inventory: Array<ItemStack?>? = null
        var armor: Array<ItemStack?>? = null

        if (inv is Array<*> && inv.isArrayOf<ItemStack>()) {
            inventory = inv as Array<ItemStack?>?
        } else if (inv is List<*>) {
            inventory = inv.toTypedArray() as Array<ItemStack?>
        }
        if (arm is Array<*> && arm.isArrayOf<ItemStack>()) {
            armor = arm as Array<ItemStack?>?
        } else if (arm is List<*>) {
            armor = arm.toTypedArray() as Array<ItemStack?>
        }
        p.inventory.clear()
        (inventory as Array<out ItemStack>?)?.let { p.inventory.setContents(it) }
        p.inventory.setArmorContents(armor)
        info( "${kitName}キットを装備しました",p)
        return true
    }

    //      キット一覧
    fun showlist(p: CommandSender): Boolean {
        val folder = File(Main.plugin.dataFolder, File.separator + "kits")
        p.sendMessage("§e§l========== 登録済みのキット =========")
        var n = 1
        val files = folder.listFiles()
        for (f in files) {
            if (f.isFile) {
                var filename = f.name
                //      隠しファイルは無視
                if (filename.substring(0, 1).equals(".", ignoreCase = true)) {
                    continue
                }
                val point = filename.lastIndexOf(".")
                if (point != -1) {
                    filename = filename.substring(0, point)
                }
                p.sendMessage("§e§l$n: §f§l$filename")
                n++
            }
        }
        return true
    }





}