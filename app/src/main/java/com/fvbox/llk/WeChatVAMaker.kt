package com.fvbox.llk

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.collection.arrayMapOf
import com.fvbox.llk.utils.CompressUtils
import com.lijunhuayc.downloader.downloader.*
import kotlinx.coroutines.*
import okhttp3.*
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val h = Handler(Looper.getMainLooper())

fun isMainThread(): Boolean {
    return Thread.currentThread() === Looper.getMainLooper().thread
}

fun runOnMainThread(runnable: java.lang.Runnable){
    if (isMainThread()){
        runnable.run()
    }else{
        h.post(runnable)
    }
}

class WeChatVAMaker(
    private val ctx: Context,
    private val cb: MakerCallback,
) {

    companion object {
        //这个请求，获取到的是新的链接，新链接才是用来下载文件的
        private const val URL = "http://118.31.78.173:9988/getFileUrl?fileName="
        private const val TAR = ".tar"
        private const val UNZIP = "unzip_"
        private const val SDCARD = "sdcard"
        private const val WECHAT_DATA = "wechat_data"
        private const val MACHINE_CONFIG_FILE = "machine_config.ini"

        private const val CFG_SERIALNO = "serialno"
        private const val CFG_SSID = "ssid"
        private const val CFG_BRAND = "brand"
        private const val CFG_SIM_IMSI = "sim_imsi"
        private const val CFG_HARDWARE = "hardware"
        private const val CFG_BUILD_ID = "build_id"
        private const val CFG_MODEL = "model"
        private const val CFG_DISPLAY_ID = "display_id"
        private const val CFG_BLUETOOTH = "bluetooth"
        private const val CFG_IMEI = "imei"
        private const val CFG_BSSID = "bssid"
        private const val CFG_WIFI = "wifi"
        private const val CFG_SIM_SERIAL = "sim_serial"
        private const val CFG_ANDROID_ID = "android_id"
    }

    interface MakerCallback{
        fun onUpdateProgress(percent: Float) //0-1
        fun onFail(err: String)
        fun onSuccess()
    }

    private var job: Job? = null

    private var curProgress = 0f

    private var isWorking = false

    private val configMap = arrayMapOf<String, String>()

    init {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            if (e is java.lang.RuntimeException){ //下载框架抛出来的
                callFail("服务器数据库无此号码，请联系代理！")
            }
        }
    }

    fun start(inputMsg: String){
        isWorking = true
        job = GlobalScope.launch(Dispatchers.IO) {
            callUpdateProgress(0.1f)
            try {
                //1 请求下载链接
                val downloadUrl = requestDownloadUrl(inputMsg)
                Log.e("llk", "downloadUrl -> $downloadUrl")

                //2 下载文件
                if (!downloadUrl.isNullOrEmpty()){
                    val filePath = withContext(Dispatchers.Main) {
                            downloadFile(downloadUrl, inputMsg+TAR)
                        }
                    if (!filePath.isNullOrEmpty()){
                        Log.e("llk", "unzip $filePath")
                        //3 解压文件
                        val unzipPath = ctx.cacheDir.absolutePath + File.separator + UNZIP + inputMsg
                        val isUnzipSuccess = CompressUtils.unTar(File(filePath), unzipPath)
                        Log.e("llk", "unzip isUnzipSuccess=$isUnzipSuccess target=$filePath output=$unzipPath")
                        if (isUnzipSuccess){
                            //4 提取配置文件信息
                            extractConfigFile(unzipPath)

                            for (a in configMap){
                                Log.e("llk", "start: " + a.key + " " + a.value)
                            }

                            //5 解压微信data数据包
                            val dataZipFilePath = unzipPath + File.separator + SDCARD + File.separator + WECHAT_DATA + TAR
                            val isDataUnzipSuccess = CompressUtils.unTar(File(dataZipFilePath), unzipPath)
                            if (isDataUnzipSuccess){

                            }else{
                                callFail("解压Data文件失败，请重试")
                            }
                        }else{
                            callFail("解压文件失败，请重试")
                        }
                    }else{
                        callFail("网络连接失败，请检查设备的网络环境是否通畅！")
                    }
                }else{
                    callFail("服务器数据库无此号码，请联系代理！")
                }
            }catch (e: Exception){
                Log.e("llk", "error !!!")
                callFail("操作失败，请重试")
                e.printStackTrace()
                isWorking = false
            }

        }
    }

    /**
     *  machine_config.ini 内容如下
        [common]
        serialno = e709d70e596fc
        ssid = TP-LINK_okagD0
        brand = ZTE
        sim_imsi = 460001749562864
        hardware = MSM8974AB
        build_id = IML74K
        model = Q505T
        display_id = Q505T_984393.LFU9L
        bluetooth = 4c:a4:a3:9c:ea:62
        imei = 301544347681366
        bssid = f9:36:d6:fd:a1:0b
        wifi = 2c:08:24:e0:e2:d7
        sim_serial = 06cb3c8602
        android_id = 895792441039f470
     */
    private fun extractConfigFile(unzipPath: String){
        val cfgFile = unzipPath + File.separator + SDCARD + File.separator + MACHINE_CONFIG_FILE

        val fis: FileInputStream?
        var reader: BufferedReader? = null
        try {
            fis = FileInputStream(cfgFile)
            reader = BufferedReader(InputStreamReader(fis))
            var line: String?
            do{
                line = reader.readLine()
                if (line != null){
                    if (line.contains(" = ")){
                        val strs = line.split(" = ")
                        if (strs.size == 2) configMap[strs[0]] = strs[1]
                    }
                }else{
                    break
                }
            }while (true)
        } catch (e: IOException) {
           e.printStackTrace()
        } finally {
            try {
                reader?.close()
            } catch (_: IOException) { }
        }

    }

    private var downloadFileTotalSize = 0

    private suspend fun downloadFile(url: String, fileName: String) : String?{
        return suspendCoroutine {
            val saveFilePath = ctx.cacheDir.absolutePath
            downloadFileTotalSize = 0
            Log.e("llk", "downloadFile $url $saveFilePath")

            val wolfDownloader = DownloaderConfig()
                .setThreadNum(1)
                .setDownloadUrl(url)
                .setFileName(fileName)
                .setSaveDir(File(saveFilePath))
                .setDownloadListener(object : DownloadProgressListener {
                    //设置进度条的最大刻度为文件的长度
                    override fun onDownloadTotalSize(totalSize: Int) {
                        downloadFileTotalSize = totalSize
                        Log.e("llk", "onDownloadTotalSize: $totalSize")
                    }

                    override fun updateDownloadProgress(size: Int, percent: Float, speed: Float) {
                        if (downloadFileTotalSize > 0){
                            val cur = size.toFloat()/downloadFileTotalSize.toFloat() * 0.7f
                            Log.e("llk", "updateDownloadProgress: $size $cur")
                            callUpdateProgress(cur)
                        }
                    }

                    override fun onDownloadSuccess(filePath: String) {
                        Log.e("llk", "onDownloadSuccess: $filePath")
                        callUpdateProgress(0.8f)
                        it.resume(filePath)
                    }

                    override fun onDownloadFailed() {
                        Log.e("llk", "onDownloadFailed")
                        it.resume(null)
                    }

                    override fun onPauseDownload() {
                        Log.e("llk", "onPauseDownload")
                    }

                    override fun onStopDownload() {
                        Log.e("llk", "onStopDownload")
                    }
                })
                .buildWolf(ctx)

            wolfDownloader.startDownload()
        }
    }

    /**
     * 网络请求 - 阻塞函数
     */
    private fun requestDownloadUrl(inputMsg: String): String? {
        val okClient = OkHttpClient()
        val okRequest = Request.Builder()
            .url(URL +inputMsg+ TAR)
            .build()
        val call = okClient.newCall(okRequest)
        val response = call.execute()
        return response.body?.string()
    }

    private fun callUpdateProgress(progress: Float){
        runOnMainThread{
            curProgress += progress
            cb.onUpdateProgress(curProgress)
        }
    }

    private fun callSuccess(){
        runOnMainThread{
            isWorking = false
            curProgress = 1.0f
            cb.onUpdateProgress(curProgress)

            cb.onSuccess()
        }
    }

    private fun callFail(msg: String){
        runOnMainThread{
            isWorking = false
            cb.onFail(msg)
        }
    }

    fun isWorking() = isWorking

    fun release(){
        job?.cancel()
    }
}