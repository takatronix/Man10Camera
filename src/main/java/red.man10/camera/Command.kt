package red.man10.camera

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.lang.Exception
import java.util.*


object Command : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
      //  info(args[0],sender)

        when(args[0]){
            "help" -> showHelp(sender)
            "set" -> set(sender,args)
            "follow" -> follow(sender,args)
            "rotate" -> rotate(sender,args)
            "spectator" -> spectator(sender,args)
         //   "look" -> look(sender,args)
         //   "teleport" -> teleport(sender,args)
            "show" -> Main.cameraThread1.show(sender)
            "hide" -> Main.cameraThread1.hide(sender)
        }


        return false
    }

    private fun follow(sender: CommandSender,args: Array<out String>){
        Main.cameraThread1.setMode(sender,CameraMode.FOLLOW)

    }
    private fun rotate(sender: CommandSender,args: Array<out String>){
        Main.cameraThread1.setMode(sender,CameraMode.GOAROUND)
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
            "position" -> setPosition(sender,name)
            "radius" -> setRadius(sender,name)
            "height" -> setHeight(sender,name)
            "nightvision" -> setNightVision(sender,name)

        }
    }
    private fun setPosition(sender: CommandSender,value:String){
        val xyz= value.split(",")
        if(xyz.size == 3){
            try{
                val x = xyz[0].toDouble()
                val y = xyz[1].toDouble()
                val z = xyz[2].toDouble()
                Main.cameraThread1.setRelativePosition(sender,x,y,z)
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

    private fun setRadius(sender: CommandSender,value:String){
        val text= value.split(",")
        if(text.size == 1){
            try{
                val r = text[0].toDouble()
                Main.cameraThread1.setRadius(sender,r)
                return
            }catch (ex:Exception){
                error(ex.localizedMessage)
            }
        }
        error("パラメータは2以上にしてください",sender)
    }

    private fun setHeight(sender: CommandSender,value:String){
        val text= value.split(",")
        if(text.size == 1){
            try{
                val h = text[0].toDouble()
                Main.cameraThread1.setHeight(sender,h)
                return
            }catch (ex:Exception){
                error(ex.localizedMessage)
            }
        }
        error("パラメータは、ひとつだけで",sender)
    }
    private fun setNightVision(sender: CommandSender,value:String){
        val text= value.split(",")
        if(text.size == 1){
            try{
                if(text[0] == "on")
                    Main.cameraThread1.setNightVision(sender,true)
                else
                    Main.cameraThread1.setNightVision(sender,false)
                return
            }catch (ex:Exception){
                error(ex.localizedMessage)
            }
        }
        error("/mcs set nightvision on/off",sender)
    }



    private fun showHelp(sender: CommandSender){
        sender.sendMessage("§b====================[Man10 Camera System]====================")
        sender.sendMessage("[§kカメラモード制御]")
        sender.sendMessage("§a/mcs follow     プレイヤーを追跡する")
        sender.sendMessage("§a/mcs rotate     プレイヤーの周りをまわる")
        sender.sendMessage("§a/mcs spectator  対象の視点を見る(サバイバル)")
        sender.sendMessage("§a/mcs show       カメラの状態のボディをみせる(クリエイティブ)")
        sender.sendMessage("§a/mcs hide       カメラのボディを見せない(クリエイティブ)")

        sender.sendMessage("[§b初期設定コマンド]")
        sender.sendMessage("§a/mcs set target [player]       監視対象を設定する")
        sender.sendMessage("§a/mcs set camera [player]       カメラプレイヤーを設定する")
        sender.sendMessage("§a/mcs set position [x,y,z]      監視対象に対する相対位置を指定")
        sender.sendMessage("§a/mcs set radius [r]            プレイヤーの周りを回る半径を指定")
        sender.sendMessage("§a/mcs set height [h]            カメラの高さを指定")
        sender.sendMessage("§a/mcs set nightvision [on/off]  ナイトビジョン")

        sender.sendMessage("[§b開発中]")
        sender.sendMessage("§a/mcs ")
        sender.sendMessage("§a/mcs teleport [x,y,z] 　　          特定の座標にカメラを移動する")
        sender.sendMessage("§a/mcs look     [x,y,z]or[Player] 　　特定の座標を見る")



        sender.sendMessage("[§b位置ファイル選択] （開発中)")
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
            return listOf("set","follow","rotate","look","spectator","show","hide")
        }

        when(args?.get(0)){
            "set" -> return onTabSet(args)
        }
        return null
    }

    private fun onTabSet(args: Array<out String>?) : List<String>?{
        if(args?.size == 2)
            return listOf("target","camera","position","radius","height","nightvision")
        return null
    }


}