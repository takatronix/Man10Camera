package red.man10.camera

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.lang.Exception


object Command : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if(!sender.hasPermission("red.man10.camera.op")){
            sender.sendMessage("このコマンドを実行する権限がない")
            return  false
        }

        if(args.isEmpty()){
            showHelp(label,sender)
            return true
        }

        when(args[0]){
            "help" -> showHelp(label,sender)
            "live" -> youtube(label,sender)
            "set" -> set(label,sender,args)
            "follow" -> follow(label,sender,args)
            "rotate" -> rotate(label,sender,args)
            "stop" -> stop(label,sender,args)
            "spectator" -> spectator(label,sender,args)
            "show" -> getCamera(label).show(sender)
            "showbody" -> getCamera(label).showBody(sender)
            "hide" -> getCamera(label).hide(sender)
            "auto" -> {
                Main.commandSender = sender
                Main.autoTask = Main.autoTask != true
                info("自動モード:${Main.autoTask}",sender)
            }
        }

        return false
    }

    private fun follow(label:String,sender: CommandSender,args: Array<out String>){
        getCamera(label).follow(sender, onlinePlayer(sender,args))
    }
    private fun rotate(label:String,sender: CommandSender,args: Array<out String>){
        getCamera(label).rotate(sender, onlinePlayer(sender,args))
    }
    private fun spectator(label:String,sender: CommandSender,args: Array<out String>){
        getCamera(label).spectator(sender, onlinePlayer(sender,args))
    }
    private fun stop(label:String,sender: CommandSender,args: Array<out String>){
        getCamera(label).stop(sender, onlinePlayer(sender,args))
    }
    private fun onlinePlayer(sender: CommandSender, args: Array<out String>): Player?{
        if(args.size < 2)
            return null
        val player = getOnlinePlayer(sender,args[1])
        if(player != null){
            sender.sendMessage("${player.name}の追跡を開始")
        }
        return player
    }

    // set [key] [value]　でカメラ設定を保存
    private fun set(label:String,sender: CommandSender,args: Array<out String>){

        if(args.size != 3){
            showHelp(label,sender)
            return
        }
        val key = args[1]
        val name = args[2]
        when(key){
            "target" -> getCamera(label).setTarget(sender,name)
            "camera" -> getCamera(label).setCamera(sender,name)
            "camera1" -> getCamera(label).setCamera(sender,name)
            "position" -> setPosition(label,sender,name)
            "radius" -> setRadius(label,sender,name)
            "height" -> setHeight(label,sender,name)
            "nightvision" -> setNightVision(label,sender,name)
            "broadcast" -> setBroadcast(label,sender,name)
            "notification" -> setNotification(label,sender,name)

        }
    }
    private fun setPosition(label:String,sender: CommandSender,value:String){
        val xyz= value.split(",")
        if(xyz.size == 3){
            try{
                val x = xyz[0].toDouble()
                val y = xyz[1].toDouble()
                val z = xyz[2].toDouble()
                getCamera(label).setRelativePosition(sender,x,y,z)
                return
            }catch (ex:Exception){
                error(ex.localizedMessage)
            }
        }
        error("パラメータは、x,y,zの相対座標で指定してください。",sender)
    }
    private fun look(sender: CommandSender,value:String){
        val xyz= value.split(",")
        if(xyz.size == 3){
            try{
                val x = xyz[0].toDouble()
                val y = xyz[1].toDouble()
                val z = xyz[2].toDouble()
                return
            }catch (ex:Exception){
                error(ex.localizedMessage)
            }
        }
        error("パラメータは、x,y,zの相対座標で指定してください。",sender)
    }
    private fun teleport(sender: CommandSender,value:String){
        val xyz= value.split(",")
        if(xyz.size == 3){
            try{
                val x = xyz[0].toDouble()
                val y = xyz[1].toDouble()
                val z = xyz[2].toDouble()
               // Main.cameraThread1.teleport(sender,x,y,z)
                return
            }catch (ex:Exception){
                error(ex.localizedMessage)
            }
        }
        error("パラメータは、x,y,zの相対座標で指定してください。",sender)
    }

    private fun setRadius(label:String,sender: CommandSender,value:String){
        val text= value.split(",")
        if(text.size == 1){
            try{
                val r = text[0].toDouble()
                getCamera(label).setRadius(sender,r)
                return
            }catch (ex:Exception){
                error(ex.localizedMessage)
            }
        }
        error("パラメータは2以上にしてください",sender)
    }
    private fun setHeight(label:String,sender: CommandSender,value:String){
        val text= value.split(",")
        if(text.size == 1){
            try{
                val h = text[0].toDouble()
                getCamera(label).setHeight(sender,h)
                return
            }catch (ex:Exception){
                error(ex.localizedMessage)
            }
        }
        error("パラメータは、ひとつだけで",sender)
    }
    private fun setNightVision(label:String,sender: CommandSender,value:String){
        when(value){
            "on" -> getCamera(label).setNightVision(sender,true)
            else -> getCamera(label).setNightVision(sender,false)
        }
    }
    private fun setBroadcast(label:String,sender: CommandSender,value:String){
        when(value){
            "on" -> getCamera(label).setBroadcast(sender,true)
            else -> getCamera(label).setBroadcast(sender,false)
        }
    }
    private fun setNotification(label:String,sender: CommandSender,value:String){
        when(value){
            "on" -> getCamera(label).setNotification(sender,true)
            else -> getCamera(label).setNotification(sender,false)
        }
    }
    // ヘルプメッセージ
    private fun showHelp(label:String,sender: CommandSender){
        sender.sendMessage("§b====================[Man10 Camera System]====================")
        sender.sendMessage("§amc1/mc2/mc3/mc4 カメラ1/カメラ2/カメラ3/カメラ4を制御")
        sender.sendMessage("§b[動作モード制御]")
        sender.sendMessage("§a/$label follow (player)    プレイヤーを追跡する")
        sender.sendMessage("§a/$label rotate (player)    プレイヤーの周りをまわる")
        sender.sendMessage("§a/$label spectator (player) 対象の視点を見る(スペクテーター専用)")
        sender.sendMessage("§a/$label stop               停止")
        sender.sendMessage("§a/$label auto              　自動モード切替")

        sender.sendMessage("§b[設定コマンド]設定は保存されます")
        sender.sendMessage("§a/$label set target [player]       監視対象を設定する")
        sender.sendMessage("§a/$label set camera [player]       カメラプレイヤーを設定する")
        sender.sendMessage("§a/$label set position [x,y,z]      監視対象に対する相対位置を指定")
        sender.sendMessage("§a/$label set radius [r]            プレイヤーの周りを回る半径を指定")
        sender.sendMessage("§a/$label set height [h]            カメラの高さを指定")
        sender.sendMessage("§a/$label set nightvision [on/off]  ナイトビジョン")
        sender.sendMessage("§a/$label set broadcast [on/off]    通知メッセージの全体通知on/off")
        sender.sendMessage("§a/$label set message [on/off]      個人通知メッセージ")
        sender.sendMessage("§b[表示モード設定]")
        sender.sendMessage("§a/$label showbody   カメラの状態のボディをみせる(クリエイティブ)")
        sender.sendMessage("§a/$label show       カメラをインビジブル状態(クリエイティブ)")
        sender.sendMessage("§a/$label hide       カメラを見せない(スペクテーター)")

        sender.sendMessage("§b[宣伝系]")
        sender.sendMessage("§a/$label live      　　　ライブ配信の告知")

        sender.sendMessage("§b[開発中]")
        sender.sendMessage("§a/$label teleport [x,y,z] or(player)    特定の座標にカメラを移動する")
        sender.sendMessage("§a/$label look     [x,y,z]or[Player] 　　特定の座標を見る")

        sender.sendMessage("[§bカメラファイル](未完成)")
        sender.sendMessage("§a/$label camera files                     カメラ設定一覧" )
        sender.sendMessage("§a/$label camera select [カメラ設定ファイル]  カメラ設定選択 ")
        sender.sendMessage("§a/$label camera delete [カメラ設定ファイル]  カメラ設定削除 ")

        sender.sendMessage("[§b位置ファイル選択] （開発中)")
        sender.sendMessage("§a/$label location files               位置ファイル一覧")
        sender.sendMessage("§a/$label location select [ファイル名]   位置ファイル選択")
        sender.sendMessage("§a/$label location delete [ファイル名]   位置ファイル削除")
        sender.sendMessage("[§b位置情報編集] (位置ファイル選択後有効)")
        sender.sendMessage("§a/$label location list                登録位置リストを表示")
        sender.sendMessage("§a/$label location add [位置名]         現在位置を登録する")
        sender.sendMessage("§a/$label location delete [位置名]      登録位置を削除する")

        sender.sendMessage("§b=======[Author: takatronix /  https://man10.red]=============")
    }
    private fun youtube(label:String,sender: CommandSender){

        sendBungeeMessage(sender,Main.youtubeMessage)
        /*
        Bukkit.getOnlinePlayers().forEach {
            player -> player.sendMessage(Main.broadcastMessage)
        }*/
    }

    // タブ補完
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>?): List<String>? {

        if(args?.size == 1){
            return listOf("set","follow","rotate","look","spectator","stop","show","showbody","hide","live","auto")
        }

        when(args?.get(0)){
            "set" -> return onTabSet(args)
        }
        return null
    }

    private fun onTabSet(args: Array<out String>?) : List<String>?{
        if(args?.size == 2)
            return listOf("target","camera","position","radius","height","nightvision","notification","broadcast")
        return null
    }


}