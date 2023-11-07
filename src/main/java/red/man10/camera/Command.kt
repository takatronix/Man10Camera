package red.man10.camera

import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.*
import org.bukkit.util.Vector
import kotlin.concurrent.thread

object Command : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (label == "manbo") {
            sender.sendMessage("§b§l[まんぼ]§f質問するには「まんぼ、銀行はどこ？」「丸石をうりたいまんぼ」のように「まんぼ」をつけて質問してください。")
            return false
        }

        if (!sender.hasPermission("red.man10.camera.op")) {
            sender.sendMessage("このコマンドを実行する権限がない")
            return false
        }

        if (args.isEmpty()) {
            showHelp(label, sender)
            return true
        }

        when (args[0]) {
            "help" -> showHelp(label, sender)
            "set" -> set(label, sender, args)
            "kit" -> kit(label, sender, args)
            "location" -> location(label, sender, args)
            "follow" -> follow(label, sender, args)
            "back" -> back(label, sender, args)
            "face" -> face(label, sender, args)
            "backview" -> backView(label, sender, args)
            "front" -> front(label, sender, args)
            "rotate" -> rotate(label, sender, args)
            "clone" -> clone(label, sender, args)
            "look" -> look(label, sender, args)
            "tp" -> tp(label, sender, args)
            "stop" -> stop(label, sender, args)
            "spectate" -> spectate(label, sender, args)
            "show" -> getCamera(label).show(sender)
            "showbody" -> getCamera(label).showBody(sender)
            "hide" -> getCamera(label).hide(sender)
            "title" -> title(label, sender, args)
            "text" -> text(label, sender, args)
            "photo" -> photo(label, sender, args)
            "set" -> set(label, sender, args)

            //
            "auto" -> {
                Main.commandSender = sender
                Main.autoTask = Main.autoTask != true
                Main.taskSwitchCount = 0
                info("自動モード:${Main.autoTask}", sender)
            }

            "server" -> server(label, sender, args)
            "test" -> test(label, sender, args)

            "switch" -> {
                Main.taskSwitchCount = 0
            }

            "config" -> config(label, sender, args)
            "movie" -> movie(label, sender, args)
            "vision" -> vision(label, sender, args)
            "freeze" -> freeze(label, sender, args)

        }

        return false
    }

    private fun photo(label: String, sender: CommandSender, args: Array<out String>) {
        val player = Bukkit.getPlayer(args[1])
        if (player != null) {
            var camera = getCamera(label)
            camera.takePhoto(sender, player,"binbo")
            return
        }


    }

    // JSONをマッピングするためのデータクラス
    private fun test(label: String, sender: CommandSender, args: Array<out String>) {

        var camera = getCamera(label)
        camera.test(sender)
    }

    private fun follow(label: String, sender: CommandSender, args: Array<out String>) {
        getCamera(label).follow(sender, onlinePlayer(sender, args))
    }

    private fun look(label: String, sender: CommandSender, args: Array<out String>) {
        getCamera(label).look(sender, onlinePlayer(sender, args))
    }

    private fun back(label: String, sender: CommandSender, args: Array<out String>) {
        getCamera(label).back(sender, onlinePlayer(sender, args))
    }

    private fun front(label: String, sender: CommandSender, args: Array<out String>) {
        getCamera(label).front(sender, onlinePlayer(sender, args))
    }

    private fun face(label: String, sender: CommandSender, args: Array<out String>) {
        getCamera(label).face(sender, onlinePlayer(sender, args))
    }

    private fun backView(label: String, sender: CommandSender, args: Array<out String>) {
        getCamera(label).backView(sender, onlinePlayer(sender, args))
    }

    private fun clone(label: String, sender: CommandSender, args: Array<out String>) {
        getCamera(label).clone(sender, onlinePlayer(sender, args))
    }

    private fun tp(label: String, sender: CommandSender, args: Array<out String>) {
        if (args.size > 3 || args.size < 1) {
            error("コマンドエラー: -> tp (player / w,x,y,z(yaw,pitch)", sender)
            return
        }

        if (args.size == 2) {
            val player = Bukkit.getPlayer(args[1])
            if (player != null) {
                getCamera(label).cameraPlayer!!.teleport(player.location)
                return
            }
        }
        val wxyzyp = args[1].split(",")
        if (wxyzyp.size <= 4) {
            error("コマンドエラー: -> tp (player / w,x,y,z(yaw,pitch)", sender)
            return
        }

        // カメラを停止し、遅延後、TPを行う
        getCamera(label).changeMode(CameraMode.STOP)

        Bukkit.getScheduler().runTask(Main.plugin!!, Runnable {
            val w = wxyzyp[0]
            val x = wxyzyp[1].toDouble()
            val y = wxyzyp[2].toDouble()
            val z = wxyzyp[3].toDouble()
            var pitch = 0.0f
            var yaw = 0.0f
            if (wxyzyp.size >= 5)
                yaw = wxyzyp[4].toFloat()
            if (wxyzyp.size >= 6)
                pitch = wxyzyp[5].toFloat()
            val loc = Location(Bukkit.getWorld(w), x, y, z, yaw, pitch)
            getCamera(label).cameraPlayer!!.teleport(loc)
        })
    }

    private fun title(label: String, sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            error("コマンドエラー: -> title [title ] [subtitle] [time=3.0])", sender)
            return
        }
        var title = args[1]
        var sub = ""
        var time = 3.0
        if (args.size >= 3)
            sub = args[2]
        if (args.size >= 4)
            time = args[3].toDouble()
        getCamera(label).sendTitle(title, sub, time)
    }

    private fun text(label: String, sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            error("コマンドエラー: -> title [title ] [subtitle] [time=3.0])", sender)
            return
        }
        var text = args[1]
        var time = 2.0
        if (args.size >= 3)
            time = args[2].toDouble()
        getCamera(label).sendText(text, time)
    }

    private fun rotate(label: String, sender: CommandSender, args: Array<out String>) {
        getCamera(label).rotate(sender, onlinePlayer(sender, args))
    }

    private fun spectate(label: String, sender: CommandSender, args: Array<out String>) {
        getCamera(label).spectate(sender, onlinePlayer(sender, args))
    }

    private fun stop(label: String, sender: CommandSender, args: Array<out String>) {
        getCamera(label).stop(sender, onlinePlayer(sender, args))
    }


    private fun server(label: String, sender: CommandSender, args: Array<out String>) {
        if (args.size != 2) {
            error("転送先サーバ名を指定してください", sender)
            return
        }
        val serverName = args[1]
        for (no in 1..Main.cameraCount) {
            val uuid = getCamera(no).uniqueId ?: continue

            if (getCamera(no).cameraLabel.equals(label) || label == "mc") {
                val player = Bukkit.getOfflinePlayer(uuid)
                sendBungeeCommand(sender, "send ${player.name} $serverName")
            }
        }
    }

    // set [key] [value]　でカメラ設定を保存
    private fun set(label: String, sender: CommandSender, args: Array<out String>) {

        if (args.size != 3) {
            showHelp(label, sender)
            return
        }
        val key = args[1]
        val name = args[2]
        when (key) {
            "target" -> getCamera(label).setTarget(sender, name)
            "camera" -> getCamera(label).setCamera(sender, name)
            "position" -> setPosition(label, sender, name)
            "radius" -> setRadius(label, sender, name)
            "height" -> setHeight(label, sender, name)
            "nightvision" -> setNightVision(label, sender, name)
            "notification" -> setNotification(label, sender, name)
            "title" -> setTitleFlag(label, sender, name)
            "kit" -> getCamera(label).setKit(sender, name)
        }
    }

    private fun kit(label: String, sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            showHelp(label, sender)
            return
        }

        // カメラの場合は、カメラのプレイヤーにキットを適用
        if (label != "mc") {
            val kit = args[1]
            getCamera(label).setKit(sender, kit, false)
            return
        }


        val key = args[1]
        if (args.size == 2) {
            when (key) {
                "list" -> {
                    KitManager.showList(sender)
                }
            }
            return
        }
        val name = args[2]
        when (key) {
            "save" -> {
                KitManager.save(sender, name)
            }

            "delete" -> {
                KitManager.delete(sender, name)
            }

            "load" -> {
                KitManager.load(sender, name)
            }
        }
    }

    private fun location(label: String, sender: CommandSender, args: Array<out String>) {

        if (label != "mc") {
            return
        }


        val key = args[1]
        if (args.size == 2) {
            when (key) {
                "list" -> {
                    Main.locationManager.showList(sender)
                }
            }
            return
        }
        val name = args[2]
        when (key) {
            "save" -> {
                Main.locationManager.addLocation(sender as Player, name)
            }

            "delete" -> {
                Main.locationManager.deleteLocation(sender as Player, name)
            }

            "tp" -> {
                Main.locationManager.teleport(sender, sender, name)
            }
        }
    }

    // 共通config
    private fun config(label: String, sender: CommandSender, args: Array<out String>) {

        if (args.size != 3) {
            showHelp(label, sender)
            return
        }

        val key = args[1]
        val value = args[2]
        when (key) {
            "broadcast" -> Main.configData.broadcast = value == "on"
            "switchTime" -> Main.configData.switchTime = value.toInt()
        }
        saveConfigData(Main.configData)
        sender.sendMessage("$key->$value saved")
    }

    private fun vision(label: String, sender: CommandSender, args: Array<out String>) {
        if (args.size != 4) {
            sender.sendMessage("$label vision [creeper/enderman/spider] mcid (秒)")
            return
        }

        val mode = args[1]
        val player = args[2]
        val sec = args[3].toInt()

        info("$mode,$player,$sec vision start")
        setVision(sender, player, mode, sec)
    }

    // 特定の場所にすべてのプレイヤーをTPさせて見させる
    private fun movie(label: String, sender: CommandSender, args: Array<out String>) {
        if (args.size > 3 || args.size < 1) {
            error("movie [w,x,y,z,yaw,pitch] 秒数", sender)
            return
        }

        var player: Player? = null
        if (args.size == 2) {
            player = Bukkit.getPlayer(args[1])
            if (player != null) {
                getCamera(label).cameraPlayer!!.teleport(player.location)
                return
            }
        }

        val wxyzyp = args[1].split(",")
        if (wxyzyp.size <= 4) {
            error("movie [w,x,y,z,yaw,pitch] 秒数", sender)
            return
        }
        val sec = args[2].toInt()

        // 引数から転送先の座標を作成する
        val w = wxyzyp[0]
        val x = wxyzyp[1].toDouble()
        val y = wxyzyp[2].toDouble()
        val z = wxyzyp[3].toDouble()
        var pitch = 0.0f
        var yaw = 0.0f
        if (wxyzyp.size >= 5)
            yaw = wxyzyp[4].toFloat()
        if (wxyzyp.size >= 6)
            pitch = wxyzyp[5].toFloat()

        if (Bukkit.getWorld(w) == null) {
            sender.sendMessage("指定されたworld:'$w'は無効です")
            return
        }

        // コマンドで指定されたロケーション
        val loc = Location(Bukkit.getWorld(w), x, y, z, yaw, pitch)
        // 特定の場所にmobをわかせる
        var mob = spawnMob(sender, "", sec, loc)
        Bukkit.getOnlinePlayers().forEach { p ->
//            if(!p.isOp){
            if (true) {
                // ゲームモードを指定秒数後に戻して視界を戻す
                val pastLocation = p.location
                p.teleport(loc)

                Bukkit.getOnlinePlayers().forEach { p2 ->
                    p.hidePlayer(Main.plugin!!, p2)
                }
                //
                Bukkit.getScheduler().runTaskLater(Main.plugin!!, Runnable {
                    //  p.gameMode = current
                    p.gameMode = GameMode.SURVIVAL
                    p.teleport(pastLocation)
                }, 20L * sec)
            }
        }


        Bukkit.getScheduler().runTaskLater(Main.plugin!!, Runnable {
            mob?.remove()
            Bukkit.getOnlinePlayers().forEach { p ->
                Bukkit.getOnlinePlayers().forEach { p2 ->
                    p.showPlayer(Main.plugin!!, p2)
                }
            }

        }, 20L * sec)

    }

    private fun freeze(label: String, sender: CommandSender, args: Array<out String>) {
        info("${args.size}")
        if (args.size <= 2 || args.size > 5) {
            sender.sendMessage("$label freeze プレイヤー メッセージ サブテキスト (秒数)")
            return
        }

        val mcid = args[1]
        val sec = args[2].toInt()
        var message = ""
        var subtext = ""
        if (args.size >= 4)
            message = args[3]
        if (args.size >= 5)
            subtext = args[4]

        Bukkit.getScheduler().runTask(Main.plugin!!, Runnable {
            val tick = sec * 20
            val p = Bukkit.getPlayer(mcid)
            p?.sendTitle(message.replace("&", "§"), subtext.replace("&", "§"), 10, tick.toInt(), 10)
        })
        setVision(sender, mcid, "creeper", sec)
    }

    private fun setPosition(label: String, sender: CommandSender, value: String) {
        val xyz = value.split(",")
        if (xyz.size == 3) {
            try {
                val x = xyz[0].toDouble()
                val y = xyz[1].toDouble()
                val z = xyz[2].toDouble()
                getCamera(label).setRelativePosition(sender, x, y, z)
                return
            } catch (ex: Exception) {
                error(ex.localizedMessage)
            }
        }
        error("パラメータは、x,y,zの相対座標で指定してください。", sender)
    }

    private fun onlinePlayer(sender: CommandSender, args: Array<out String>): Player? {
        if (args.size < 2)
            return null
        val player = getOnlinePlayer(sender, args[1])
        if (player != null) {
            sender.sendMessage("${player.name}の追跡を開始")
            return player
        }


        return player
    }

    private fun setRadius(label: String, sender: CommandSender, value: String) {
        val text = value.split(",")
        if (text.size == 1) {
            try {
                val r = text[0].toDouble()
                getCamera(label).setRadius(sender, r)
                return
            } catch (ex: Exception) {
                error(ex.localizedMessage)
            }
        }
        error("パラメータは2以上にしてください", sender)
    }

    private fun setHeight(label: String, sender: CommandSender, value: String) {
        val text = value.split(",")
        if (text.size == 1) {
            try {
                val h = text[0].toDouble()
                getCamera(label).setHeight(sender, h)
                return
            } catch (ex: Exception) {
                error(ex.localizedMessage)
            }
        }
        error("パラメータは、ひとつだけで", sender)
    }

    private fun setNightVision(label: String, sender: CommandSender, value: String) {
        when (value) {
            "on" -> getCamera(label).setNightVision(sender, true)
            else -> getCamera(label).setNightVision(sender, false)
        }
    }

    private fun setNotification(label: String, sender: CommandSender, value: String) {
        when (value) {
            "on" -> getCamera(label).setNotification(sender, true)
            else -> getCamera(label).setNotification(sender, false)
        }
    }

    private fun setTitleFlag(label: String, sender: CommandSender, value: String) {
        when (value) {
            "on" -> getCamera(label).setTitleFlag(sender, true)
            else -> getCamera(label).setTitleFlag(sender, false)
        }
    }

    // ヘルプメッセージ
    private fun showHelp(label: String, sender: CommandSender) {
        sender.sendMessage("§b===============[Man10 Camera System ver.${Main.version}]====================")
        sender.sendMessage("§amc1/mc2/mc3/mc4 カメラ1/カメラ2/カメラ3/カメラ4を制御")
        sender.sendMessage("§b[動作モード制御]")
        sender.sendMessage("§a/$label follow (player)    プレイヤーを追跡する")
        sender.sendMessage("§a/$label rotate (player)    プレイヤーの周りをまわる")
        sender.sendMessage("§a/$label spectate (player)  対象のプレイヤーの視点を見る(スペクテーター)")
        sender.sendMessage("§a/$label clone (player)     対象プレーヤーの状態をクローンする")
        sender.sendMessage("§a/$label back (player)      対象プレーヤーの背後につく(左右だけ向く)")
        sender.sendMessage("§a/$label backview (player)  対象プレーヤーの背後から視線を合わせる(スペクテーター)")
        sender.sendMessage("§a/$label look (player)      停止して対象プレイヤーに注視する")
        sender.sendMessage("§a/$label front (player)     プレイヤー前方に現れる")
        sender.sendMessage("§a/$label face (player)      プレイヤーの視線の先に現れる")
        sender.sendMessage("§a/$label tp (player/loc(world,x,y,z[,yaw,pitch])  指定位置へテレポート")
        sender.sendMessage("§a/$label title (タイトルメッセージ) サブタイトル [秒数]")
        sender.sendMessage("§a/$label text (アクションテキスト) [秒数]")
        sender.sendMessage("§a/$label stop               停止")


        sender.sendMessage("§b[カメラ別設定コマンド]設定は保存されます")
        sender.sendMessage("§a/$label set target [player]       監視対象を設定する")
        sender.sendMessage("§a/$label set camera [player]       カメラプレイヤーを設定する")
        sender.sendMessage("§a/$label set position [x,y,z]      監視対象に対する相対位置を指定")
        sender.sendMessage("§a/$label set radius [r]            プレイヤーの周りを回る半径を指定")
        sender.sendMessage("§a/$label set height [h]            カメラの高さを指定")
        sender.sendMessage("§a/$label set nightvision [on/off]  ナイトビジョン")
        sender.sendMessage("§a/$label set title [on/off]        タイトルテキストのon/off")
        sender.sendMessage("§a/$label set notification [on/off] 通知メッセージ")
        sender.sendMessage("§a/$label set kit [name]            デフォルトキット登録")

        sender.sendMessage("§b[表示モード設定]")
        sender.sendMessage("§a/$label showbody   カメラの状態のボディをみせる(クリエイティブ)")
        sender.sendMessage("§a/$label show       カメラをインビジブル状態(クリエイティブ)")
        sender.sendMessage("§a/$label hide       カメラを見せない(スペクテーター)")
        sender.sendMessage("§a/$label kit [name]  登録済みのkitを設定")


        sender.sendMessage("§c[外見制御]")
        sender.sendMessage("§c/mc kit list          登録済みのKitのリスト")
        sender.sendMessage("§c/mc kit save [name]   現在の装備(Inv含む)を保存")
        sender.sendMessage("§c/mc kit load [name]   登録済みのkitを設定(自分に)")
        sender.sendMessage("§c/mc kit delete [name] 登録済みのkitを削除")

        sender.sendMessage("§c[位置保存]")
        sender.sendMessage("§c/mc location save [name]     場所を保存")
        sender.sendMessage("§c/mc location delete [name]   場所を削除")
        sender.sendMessage("§c/mc location 場所リストを表示")

        sender.sendMessage("§c[共通制御]")
        sender.sendMessage("§c/mc auto              自動モード切替")
        sender.sendMessage("§c/mc switch            自動運転のターゲットを切替")
        sender.sendMessage("§c/mc server [サーバ名]   転送先サーバ名")
        sender.sendMessage("§c/mc freeze [player] 秒数 (メッセージ) (サブタイトル)  フリーズメッセージ表示")
        sender.sendMessage("§a/mc movie world,x,y,z,yaw,pitch(秒数)  指定位置へテレポートし指定秒数そこを見させる(heist用)")
        sender.sendMessage("§c/mc vision [creeper/enderman/spider] [player] 秒数   プレイヤーの視界を切り替える")

        sender.sendMessage("§c[共通設定]")
        sender.sendMessage("§c/mc config broadcast [on/off]")

        sender.sendMessage("§c[宣伝系]")
        sender.sendMessage("§c/mc live       ライブ配信の告知")

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

    private fun youtube(label: String, sender: CommandSender) {
        if (Main.configData.broadcast)
            sendBungeeMessage(sender, Main.youtubeMessage)
        else
            info("broadcastオフのためメッセージを出さない", sender)
    }

    // タブ補完
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>?
    ): List<String>? {

        if (alias == "manbo") {
            return null
        }

        if (args?.size == 1) {
            return listOf(
                "set",
                "config",
                "follow",
                "rotate",
                "clone",
                "back",
                "backview",
                "kit",
                "tp",
                "look",
                "spectate",
                "stop",
                "front",
                "face",
                "show",
                "showbody",
                "hide",
                "live",
                "test",
                "auto",
                "server",
                "switch",
                "vision",
                "freeze",
                "movie",
                "location",
                "photo",
                "text",
                "title"
            )
        }
        when (args?.get(0)) {
            "set" -> return onTabSet(args)
            "kit" -> return onTabKit(args, alias)
            "location" -> return onTabLocation(args, alias)
            "config" -> return onTabConfig(args)
            "vision" -> return onTabVision(args)
        }
        return null
    }

    private fun onTabSet(args: Array<out String>?): List<String>? {
        if (args?.size == 2)
            return listOf(
                "target",
                "camera",
                "position",
                "radius",
                "height",
                "nightvision",
                "notification",
                "title",
                "kit"
            )
        if (args?.size == 3) {
            if (args[1] == "kit")
                return KitManager.getList()
        }
        return null
    }

    private fun onTabKit(args: Array<out String>?, alias: String): List<String>? {
        if (alias != "mc") {
            return KitManager.getList()
        }
        if (args?.size == 2)
            return listOf("list", "save", "load", "delete")
        return null
    }

    private fun onTabLocation(args: Array<out String>?, alias: String): List<String>? {
        if (alias != "mc") {
            return Main.locationManager.getList()
        }

        if (args?.size == 2)
            return listOf("list", "save", "teleport", "delete")

        if (args?.size == 3)
            return Main.locationManager.getList()
        return null
    }

    private fun onTabConfig(args: Array<out String>?): List<String>? {
        if (args?.size == 2)
            return listOf("broadcast", "switchTime")
        return null
    }

    private fun onTabVision(args: Array<out String>?): List<String>? {
        if (args?.size == 2)
            return listOf("creeper", "enderman", "spider")
        return null
    }

    private fun setArmorStandView(sender: CommandSender, mcid: String, location: Location, sec: Int): Boolean {

        // プレイヤー確認
        val p: Player? = Bukkit.getPlayer(mcid)
        if (p == null || !p.isOnline || p.name != mcid) {
            error("プレイヤーが存在しない")
            return false
        }

        //  アーマースタンド作成
        val armorStand = location.world.spawn(location, ArmorStand::class.java)
        armorStand.setGravity(false)
        armorStand.isVisible = false
        armorStand.isMarker = true
        armorStand.isSilent = true
        armorStand.isInvulnerable = true
        armorStand.setGravity(false)

        // 戻るべき座標
        val pastLocation: Location = p.location.clone()

        Bukkit.getScheduler().runTaskLater(Main.plugin!!, Runnable {
            val directionVector: Vector = p.location.subtract(pastLocation).toVector()
            if (directionVector.length().toInt() != 0)
                armorStand.velocity = directionVector.normalize().multiply(Math.sqrt(pastLocation.distance(p.location)))

            // ゲームモードを指定秒数後に戻して視界を戻す
            val current = p.gameMode
            p.gameMode = GameMode.SPECTATOR
            armorStand.setRotation(p.location.yaw, p.location.pitch)

            p.spectatorTarget = armorStand
            Bukkit.getScheduler().runTaskLater(Main.plugin!!, Runnable {
                p.teleport(pastLocation)
                p.gameMode = current
                armorStand.remove()
            }, 20L * sec)
        }, 2)

        return true
    }


    fun spawnMob(sender: CommandSender, mode: String, sec: Int, loc: Location): Mob? {
        val mob: Mob = loc.world.spawnEntity(loc, EntityType.ALLAY) as Mob
        mob.isInvisible = true
        mob.isSilent = true
        mob.isInvulnerable = true
        mob.setAI(false)
        mob.setGravity(false)
        mob.isCollidable = false
        mob.teleport(loc)
        mob.setRotation(loc.yaw, loc.pitch)
        return mob
    }


    // 指定秒数間エフェクトをかける
    // "creeper","enderman","spider"
    fun setVision(sender: CommandSender, mcid: String, mode: String, sec: Int, loc: Location? = null): Boolean {
        val p: Player? = Bukkit.getPlayer(mcid)
        if (p == null || !p.isOnline || p.name != mcid) {
            error("プレイヤーが存在しない", sender)
            return false
        }

        var view: Mob? = null
        if (mode.equals("creeper", ignoreCase = true))
            view = p.world.spawnEntity(p.location, EntityType.CREEPER) as Creeper
        if (mode.equals("enderman", ignoreCase = true))
            view = p.world.spawnEntity(p.location, EntityType.ENDERMAN) as Enderman
        if (mode.equals("spider", ignoreCase = true))
            view = p.world.spawnEntity(p.location, EntityType.SPIDER) as Spider

        if (mode.equals("allay", ignoreCase = true))
            view = p.world.spawnEntity(p.location, EntityType.ALLAY) as Allay
        if (mode.equals("bat", ignoreCase = true))
            view = p.world.spawnEntity(p.location, EntityType.BAT) as Bat

        if (view == null) {
            error("mob view作成失敗:$mode", sender)
            return false
        }

        val finalView: Mob = view
        finalView.isInvisible = true
        finalView.isSilent = true
        finalView.isInvulnerable = true
        finalView.setAI(false)
        finalView.setGravity(false)
        finalView.isCollidable = false
        if (view is Creeper) {
            view.fuseTicks = Int.MAX_VALUE
        }

        if (loc != null)
            finalView.teleport(loc)
        else
            finalView.teleport(p.location)
        Bukkit.getScheduler().runTaskLater(Main.plugin!!, Runnable {
            val pastLocation: Location = p.location
            Bukkit.getScheduler().runTaskLater(Main.plugin!!, Runnable {
                val directionVector: Vector = p.location.subtract(pastLocation).toVector()
                if (directionVector.length().toInt() != 0)
                    finalView.velocity =
                        directionVector.normalize().multiply(Math.sqrt(pastLocation.distance(p.location)))

                finalView.isAware = false

                // ゲームモードを指定秒数後に戻して視界を戻す
                val current = p.gameMode
                p.gameMode = GameMode.SPECTATOR
                finalView.setRotation(p.location.yaw, p.location.pitch)
                p.spectatorTarget = finalView
                Bukkit.getScheduler().runTaskLater(Main.plugin!!, Runnable {
                    p.gameMode = current
                    p.teleport(pastLocation)
                    finalView.remove()
                }, 20L * sec)
            }, 2)
        }, 1)
        return true
    }

}