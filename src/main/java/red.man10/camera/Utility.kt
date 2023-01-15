package red.man10.camera

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException


// オフラインのプレイヤーを返す
fun getOfflinePlayer(sender: CommandSender, name:String?): Player?{
    if(name == null){
        error("名前が指定されていません",sender)
        return null
    }
    val player = Bukkit.getOfflinePlayerIfCached(name)
    if(player == null){
        error("プレイヤーが存在しません",sender)
        return null
    }
    return player.player
}
// オンラインのプレイヤーを返す
fun getOnlinePlayer(sender: CommandSender, name:String?): Player?{
    if(name == null){
        error("名前が指定されていません",sender)
        return null
    }
    val player = Bukkit.getPlayer(name)
    if(player == null){
        error("プレイヤーが見つかりません",sender)
        return null
    }
    if(!player.isOnline){
        error("プレイヤーがオンラインではありません",sender)
        return null
    }
    return player
}
fun toRadian(angle: Double): Double {
    return angle * Math.PI / 180f
}
class Utility {

    // 角度->ラジアン

    // ラジアン->角度
    fun toAngle(radian: Double): Double {
        return radian * 180 / Math.PI
    }
    /*
    private fun getAngle(player: Player): Double {
        var rotation = ((player.location.yaw - 90) % 360).toDouble()
        if (rotation < 0) {
            rotation += 360.0
        }
        return rotation
    }
*/
    /*
    fun sendTitle(player: Player, text: String, fadeInTime: Int, showTime: Int, fadeOutTime: Int, color: net.md_5.bungee.api.ChatColor) {
        val chatTitle: IChatBaseComponent =
            ChatSerializer.a("{\"text\": \"" + text + "\",color:" + color.name().toLowerCase() + "}")
        val title = PacketPlayOutTitle(EnumTitleAction.TITLE, chatTitle)
        val length = PacketPlayOutTitle(fadeInTime, showTime, fadeOutTime)
        (player as CraftPlayer).getHandle().playerConnection.sendPacket(title)
        (player as CraftPlayer).getHandle().playerConnection.sendPacket(length)
    }

    fun sAB(player: Player, message: String) {
        val p: CraftPlayer = player as CraftPlayer
        val b: ChatMessageType = net.minecraft.server.v1_14_R1.ChatMessageType.GAME_INFO
        val cbc: IChatBaseComponent = ChatSerializer.a("{\"text\": \"$message\"}")
        val ppoc = PacketPlayOutChat(cbc, b)
        (p as CraftPlayer).getHandle().playerConnection.sendPacket(ppoc)
    }
*/
    @Throws(IllegalStateException::class)
    fun toBase64(inventory: Inventory): String? {
        return try {
            val outputStream = ByteArrayOutputStream()
            val dataOutput = BukkitObjectOutputStream(outputStream)

            // Write the size of the inventory
            dataOutput.writeInt(inventory.size)

            // Save every element in the list
            for (i in 0 until inventory.size) {
                dataOutput.writeObject(inventory.getItem(i))
            }

            // Serialize that array
            dataOutput.close()
            Base64Coder.encodeLines(outputStream.toByteArray())
        } catch (e: java.lang.Exception) {
            throw IllegalStateException("Unable to save item stacks.", e)
        }
    }

    @Throws(IOException::class)
    fun fromBase64(data: String?): Inventory? {
        return try {
            val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(data))
            val dataInput = BukkitObjectInputStream(inputStream)
            val inventory = Bukkit.getServer().createInventory(null, dataInput.readInt())

            // Read the serialized inventory
            for (i in 0 until inventory.size) {
                inventory.setItem(i, dataInput.readObject() as ItemStack)
            }
            dataInput.close()
            inventory
        } catch (e: ClassNotFoundException) {
            throw IOException("Unable to decode class type.", e)
        }
    }
    @Throws(IllegalStateException::class)
    fun itemStackArrayToBase64(items: Array<ItemStack?>): String? {
        return try {
            val outputStream = ByteArrayOutputStream()
            val dataOutput = BukkitObjectOutputStream(outputStream)
            // Write the size of the inventory
            dataOutput.writeInt(items.size)
            // Save every element in the list
            for (i in items.indices) {
                dataOutput.writeObject(items[i])
            }
            // Serialize that array
            dataOutput.close()
            Base64Coder.encodeLines(outputStream.toByteArray())
        } catch (e: Exception) {
            throw IllegalStateException("Unable to save item stacks.", e)
        }
    }

    @Throws(IOException::class)
    fun itemStackArrayFromBase64(data: String?): Array<ItemStack?>? {
        return try {
            val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(data))
            val dataInput = BukkitObjectInputStream(inputStream)
            val items = arrayOfNulls<ItemStack>(dataInput.readInt())

            // Read the serialized inventory
            for (i in items.indices) {
                items[i] = dataInput.readObject() as ItemStack
            }
            dataInput.close()
            items
        } catch (e: ClassNotFoundException) {
            throw IOException("Unable to decode class type.", e)
        }
    }

}