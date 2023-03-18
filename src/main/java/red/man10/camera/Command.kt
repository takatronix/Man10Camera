package red.man10.camera

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.*
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType


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
            "config" -> config(label,sender,args)
            "follow" -> follow(label,sender,args)
            "back" -> back(label,sender,args)
            "backview" -> backView(label,sender,args)
            "rotate" -> rotate(label,sender,args)
            "clone" -> clone(label,sender,args)
            "look" -> look(label,sender,args)
            "tp" -> tp(label,sender,args)
            "stop" -> stop(label,sender,args)
            "spectate" -> spectate(label,sender,args)
            "show" -> getCamera(label).show(sender)
            "showbody" -> getCamera(label).showBody(sender)
            "hide" -> getCamera(label).hide(sender)
            "title" -> title(label,sender,args)
            "text" -> text(label,sender,args)
            "auto" -> {
                Main.commandSender = sender
                Main.autoTask = Main.autoTask != true
                Main.taskSwitchCount =0
                info("自動モード:${Main.autoTask}",sender)
            }
            "server" -> server(label,sender,args)
            "test" -> test(label,sender,args)
            "switch" -> {Main.taskSwitchCount = 0}

        }

        return false
    }
    private fun test(label:String, sender: CommandSender, args: Array<out String>){
/*
        val player = getCamera(1).cameraPlayer
        sendActionText(player!!,"§f§lアクションテキスト")
        sendTitleText(player!!,"§eTitle")

 */
    }

    private fun follow(label:String,sender: CommandSender,args: Array<out String>){
        getCamera(label).follow(sender, onlinePlayer(sender,args))
    }
    private fun look(label:String,sender: CommandSender,args: Array<out String>){
        getCamera(label).look(sender, onlinePlayer(sender,args))
    }
    private fun back(label:String,sender: CommandSender,args: Array<out String>){
        getCamera(label).back(sender, onlinePlayer(sender,args))
    }
    private fun backView(label:String,sender: CommandSender,args: Array<out String>){
        getCamera(label).backView(sender, onlinePlayer(sender,args))
    }
    private fun clone(label:String,sender: CommandSender,args: Array<out String>){
        getCamera(label).clone(sender, onlinePlayer(sender,args))
    }
    private fun tp(label:String,sender: CommandSender,args: Array<out String>){
        if(args.size > 3 || args.size < 1 ){
            error("コマンドエラー: -> tp (player / w,x,y,z(yaw,pitch)" ,sender)
            return
        }

        if(args.size == 2){
            val player = Bukkit.getPlayer(args[1])
            if(player != null) {
                getCamera(label).cameraPlayer!!.teleport(player.location)
                return
            }
        }
        val wxyzyp= args[1].split(",")
        if(wxyzyp.size <= 4){
            error("コマンドエラー: -> tp (player / w,x,y,z(yaw,pitch)" ,sender)
            return
        }

        // カメラを停止し、遅延後、TPを行う
        getCamera(label).changeMode(CameraMode.STOP)

        Bukkit.getScheduler().runTask(Main.plugin, Runnable {
            val w = wxyzyp[0]
            val x = wxyzyp[1].toDouble()
            val y = wxyzyp[2].toDouble()
            val z = wxyzyp[3].toDouble()
            var pitch = 0.0f
            var yaw = 0.0f
            if(wxyzyp.size >= 5)
                yaw = wxyzyp[4].toFloat()
            if(wxyzyp.size >= 6)
                pitch = wxyzyp[5].toFloat()
            val loc = Location(Bukkit.getWorld(w),x,y,z,yaw,pitch)
            getCamera(label).cameraPlayer!!.teleport(loc)
        })
    }

    private fun title(label:String,sender: CommandSender,args: Array<out String>){
        if(args.size < 2){
            error("コマンドエラー: -> title [title ] [subtitle] [time=3.0])" ,sender)
            return
        }
        var title = args[1]
        var sub =""
        var time = 3.0
        if(args.size >= 3)
             sub = args[2]
        if(args.size >= 4)
             time = args[3].toDouble()
        getCamera(label).sendTitle(title,sub,time)
   }
    private fun text(label:String,sender: CommandSender,args: Array<out String>){
        if(args.size < 2){
            error("コマンドエラー: -> title [title ] [subtitle] [time=3.0])" ,sender)
            return
        }
        var text = args[1]
        var time = 2.0
        if(args.size >= 3)
            time = args[2].toDouble()
        getCamera(label).sendText(text,time)
    }

    private fun rotate(label:String,sender: CommandSender,args: Array<out String>){
        getCamera(label).rotate(sender, onlinePlayer(sender,args))
    }
    private fun spectate(label:String, sender: CommandSender, args: Array<out String>){
        getCamera(label).spectate(sender, onlinePlayer(sender,args))
    }
    private fun stop(label:String,sender: CommandSender,args: Array<out String>){
        getCamera(label).stop(sender, onlinePlayer(sender,args))
    }


    private fun server(label:String, sender: CommandSender, args: Array<out String>){
        if(args.size != 2){
            error("転送先サーバ名を指定してください",sender)
            return
        }
        val serverName = args[1]
        for(no in 1..Main.cameraCount) {
            var uuid = getCamera(no).uniqueId
            if(uuid == null)
                continue
            var player = Bukkit.getOfflinePlayer(uuid)
            sendBungeeCommand(sender,"send ${player.name} $serverName")
        }
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
            "position" -> setPosition(label,sender,name)
            "radius" -> setRadius(label,sender,name)
            "height" -> setHeight(label,sender,name)
            "nightvision" -> setNightVision(label,sender,name)
            "notification" -> setNotification(label,sender,name)
            "title" -> setTitleFlag(label,sender,name)
        }
    }

    // 共通config
    private fun config(label:String,sender: CommandSender,args: Array<out String>){

        if(args.size != 3){
            showHelp(label,sender)
            return
        }

        val key = args[1]
        val value = args[2]
        when(key){
            "broadcast" -> Main.configData.broadcast = value == "on"
        }
        saveConfigData(Main.configData)
        sender.sendMessage("$key->$value saved")
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

    private fun onlinePlayer(sender: CommandSender, args: Array<out String>): Player?{
        if(args.size < 2)
            return null
        val player = getOnlinePlayer(sender,args[1])
        if(player != null){
            sender.sendMessage("${player.name}の追跡を開始")
        }
        return player
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

    private fun setNotification(label:String,sender: CommandSender,value:String){
        when(value){
            "on" -> getCamera(label).setNotification(sender,true)
            else -> getCamera(label).setNotification(sender,false)
        }
    }
    private fun setTitleFlag(label:String,sender: CommandSender,value:String){
        when(value){
            "on" -> getCamera(label).setTitleFlag(sender,true)
            else -> getCamera(label).setTitleFlag(sender,false)
        }
    }
    // ヘルプメッセージ
    private fun showHelp(label:String,sender: CommandSender){
        sender.sendMessage("§b====================[Man10 Camera System]====================")
        sender.sendMessage("§amc1/mc2/mc3/mc4 カメラ1/カメラ2/カメラ3/カメラ4を制御")
        sender.sendMessage("§b[動作モード制御]")
        sender.sendMessage("§a/$label follow (player)    プレイヤーを追跡する")
        sender.sendMessage("§a/$label rotate (player)    プレイヤーの周りをまわる")
        sender.sendMessage("§a/$label spectate (player)  対象のプレイヤーの視点を見る(スペクテーター)")
        sender.sendMessage("§a/$label clone (player)     対象プレーヤーの状態をクローンする")
        sender.sendMessage("§a/$label back (player)      対象プレーヤーの背後につく(左右だけ向く)")
        sender.sendMessage("§a/$label backview (player)  対象プレーヤーの背後から視線を合わせる")
        sender.sendMessage("§a/$label look (player)      停止して対象プレイヤーに注視する")
        sender.sendMessage("§a/$label tp (player/loc(world,x,y,z[,yaw,pitch])  指定位置へテレポート")
        sender.sendMessage("§a/$label title (タイトルメッセージ) サブタイトル [秒数]")
        sender.sendMessage("§a/$label text (アクションテキスト) [秒数]")
        sender.sendMessage("§a/$label stop               停止")

        sender.sendMessage("§b[全体制御]")
        sender.sendMessage("§a/$label auto              自動モード切替")
        sender.sendMessage("§a/$label switch            自動運転のターゲットを切替")
        sender.sendMessage("§a/$label server [サーバ名]   転送先サーバ名")

        sender.sendMessage("§b[共通設定]")
        sender.sendMessage("§a/$label config broadcast [on/off]")



        sender.sendMessage("§b[カメラ別設定コマンド]設定は保存されます")
        sender.sendMessage("§a/$label set target [player]       監視対象を設定する")
        sender.sendMessage("§a/$label set camera [player]       カメラプレイヤーを設定する")
        sender.sendMessage("§a/$label set position [x,y,z]      監視対象に対する相対位置を指定")
        sender.sendMessage("§a/$label set radius [r]            プレイヤーの周りを回る半径を指定")
        sender.sendMessage("§a/$label set height [h]            カメラの高さを指定")
        sender.sendMessage("§a/$label set nightvision [on/off]  ナイトビジョン")
        sender.sendMessage("§a/$label set title [on/off]        タイトルテキストのon/off")
        sender.sendMessage("§a/$label set message [on/off]      個人通知メッセージ")
        sender.sendMessage("§b[表示モード設定]")
        sender.sendMessage("§a/$label showbody   カメラの状態のボディをみせる(クリエイティブ)")
        sender.sendMessage("§a/$label show       カメラをインビジブル状態(クリエイティブ)")
        sender.sendMessage("§a/$label hide       カメラを見せない(スペクテーター)")

        sender.sendMessage("§b[宣伝系]")
        sender.sendMessage("§a/$label live       ライブ配信の告知")

        /*
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
*/
        sender.sendMessage("§b=======[Author: takatronix /  https://man10.red]=============")
    }
    private fun youtube(label:String,sender: CommandSender){
        if(Main.configData.broadcast)
            sendBungeeMessage(sender,Main.youtubeMessage)
        else
            info("broadcastオフのためメッセージを出さない",sender)
    }

    // タブ補完
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>?): List<String>? {

        if(args?.size == 1){
            return listOf("set","config","follow","rotate","clone","back","backview","tp","look","spectate","stop","show","showbody","hide","live","auto","server","switch")
        }

        when(args?.get(0)){
            "set" -> return onTabSet(args)
            "config" -> return onTabConfig(args)
        }
        return null
    }

    private fun onTabSet(args: Array<out String>?) : List<String>?{
        if(args?.size == 2)
            return listOf("target","camera","position","radius","height","nightvision","notification","title")
        return null
    }
    private fun onTabConfig(args: Array<out String>?) : List<String>?{
        if(args?.size == 2)
            return listOf("broadcast")
        return null
    }


    //
    /*
    fun onCommand(sender: CommandSender, command: Command?,
        @NotNull label: Swww
                  tring?,
        @NotNull args: Array<String>
    ): Boolean {
        val p: Player?
        if (args.size == 4) {
            p = Bukkit.getPlayer(args[3])
            if (p == null || !p.isOnline || p.name != args[3]) {
                sender.sendMessage(Man10Raid.prefix + "§c§lプレイヤーが存在しません")
                return false
            }
        } else {
            p = sender as Player
        }

        /*
        if (playerInVision.contains(p.uniqueId)) {
            sender.sendMessage(Man10Raid.prefix + "§c§lプレイヤーはすでにエフェクト付与中です")
            return false
        }
        playerInVision.add(p!!.uniqueId)
        */


        var view: Monster? = null
        if (args[2].equals("creeper", ignoreCase = true)) view =
            p.world.spawnEntity(p.location, EntityType.CREEPER) as Creeper
        if (args[2].equals("enderman", ignoreCase = true)) view =
            p.world.spawnEntity(p.location, EntityType.ENDERMAN) as Enderman
        if (args[2].equals("spider", ignoreCase = true)) view =
            p.world.spawnEntity(p.location, EntityType.SPIDER) as Spider
        if (view == null) return false
        if (view is Creeper) {
            view.maxFuseTicks = 1000
        }
        val finalView: Monster = view
        finalView.isInvisible = true
        finalView.isSilent = true
        finalView.isInvulnerable = true
        finalView.teleport(p.location)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val pastLocation: Location = p.location
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                val directionVector: Vector = p.location.subtract(pastLocation).toVector()
                if (directionVector.length() !== 0) finalView.velocity = directionVector.normalize().multiply(
                    Math.sqrt(
                        pastLocation.distance(
                            p.location
                        )
                    )
                )
                finalView.isAware = false
                val current = p.gameMode
                p.gameMode = GameMode.SPECTATOR
                finalView.setRotation(p.location.yaw, p.location.pitch)
                p.spectatorTarget = finalView
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    p.gameMode = current
                    finalView.remove()
                    playerInVision.remove(p.uniqueId)
                }, 20L * args[1].toInt())
            }, 2)
        }, 1)
        return true
    }
*/
}