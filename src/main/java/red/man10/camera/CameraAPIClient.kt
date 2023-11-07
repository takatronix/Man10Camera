package red.man10.camera

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.bukkit.Bukkit
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class CameraAPIClient {
    private lateinit var client : OkHttpClient
    lateinit var gson : Gson
    var url = "http://zfx:7000"

    // コンストラクタ
    init {
        setup()
        gson = Gson()
    }
    //      GET
    fun getRequest(path:String): String? {

   //     loggerDebug("GetRequest:${path}")

        val request = Request.Builder()
            .url(url+path)
            .build()

        var result : String? = null

        try {
            val response = client.newCall(request).execute()
            result = response.body?.string()
            Bukkit.getLogger().info("ResponseBody:$result")
            response.close()
        }catch (e:Exception){
            Bukkit.getLogger().severe(e.message)
        }

        return result
    }
    fun setup() : Boolean{

        if (::client.isInitialized){
            client.connectionPool.evictAll()
        }

        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
        )

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())


        client = OkHttpClient.Builder()
            .cache(null)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10,TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory,trustAllCerts[0] as X509TrustManager)
            .build()

        return true

        }

    fun takePhoto(mode:String,mcid:String):String?{
        var result = getRequest("/Camera?key=$mode&mcid=$mcid")
        return result
    }

}