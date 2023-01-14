package red.man10.camera

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.util.*


object Command : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
      //  info(args[0],sender)

        when(args[0]){
            "help" -> showHelp(sender)
            "set" -> set(sender,args)
            "follow" -> follow(sender,args)
            "spectator" -> spectator(sender,args)
        }


        return false
    }

    private fun follow(sender: CommandSender,args: Array<out String>){
        Main.cameraThread1.setMode(sender,CameraMode.FOLLOW)

    }
    private fun spectator(sender: CommandSender,args: Array<out String>){
        Main.cameraThread1.setMode(sender,CameraMode.SPECTATOR)

    }

    // set [key] [value]
    private fun set(sender: CommandSender,args: Array<out String>){
        info("args.size = ${args.size}")
        if(args.size != 3){
            showHelp(sender)
            return
        }
        val key = args[1]
        val name = args[2]
        when(key){
            "target" -> Main.cameraThread1.setTarget(sender,name)
            "camera" -> Main.cameraThread1.setCamera(sender,name)
            "camera1" -> Main.cameraThread1.setCamera(sender,name)
        }
    }



    private fun showHelp(sender: CommandSender){
        sender.sendMessage("§b====================[Man10 Camera System]====================")
        sender.sendMessage("§b[特定ユーザー追跡]")
        sender.sendMessage("§a/mcs follow [player] <追跡モード:0-1> プレイヤーを追跡する")

        sender.sendMessage("[§b初期設定コマンド]")
        sender.sendMessage("§a/mcs set target [player]  監視対象を設定する")
        sender.sendMessage("§a/mcs set camera [player]  カメラプレイヤーを設定する")

        sender.sendMessage("[§b位置ファイル選択]")
        sender.sendMessage("§a/mcs location file                     位置ファイル一覧")
        sender.sendMessage("§a/mcs location file select [ファイル名]   位置ファイル選択")
        sender.sendMessage("§a/mcs location file delete [ファイル名]   位置ファイル削除")
        sender.sendMessage("[§b位置情報編集] (ファイル選択後有効)")
        sender.sendMessage("§a/mcs location list                登録位置リストを表示")
        sender.sendMessage("§a/mcs location add [位置名]         現在位置を登録する")
        sender.sendMessage("§a/mcs location delete [位置名]      登録位置を削除する")

        sender.sendMessage("§b=======[Author: takatronix /  https://man10.red]=============")
    }

    // タブ補完
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>?): List<String>? {

        if(args?.size == 1){
            return listOf("set","location")
        }

        when(args?.get(0)){
            "set" -> return onTabSet(args)
        }
        return null
    }

    private fun onTabSet(args: Array<out String>?) : List<String>?{
        if(args?.size == 2)
            return listOf("target","camera")
        return null
    }


}