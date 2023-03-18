package red.man10.camera
data class ConfigData (
    var broadcast:Boolean = false,
    var serverName:String = "",
    var switchTime: Int = 30                 // タスク切替タイミング
)