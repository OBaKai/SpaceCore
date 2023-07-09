package com.fvbox.llk.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.lijunhuayc.downloader.downloader.*
import kotlinx.coroutines.*
import okhttp3.*
import java.io.File
import java.nio.charset.StandardCharsets
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
    }

    interface MakerCallback{
        fun onUpdateProgress(percent: Float) //0-1
        fun onFail(err: String)
        fun onSuccess()
    }

    private var job: Job? = null

    private var curProgress = 0f

    private var isWorking = false

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
                            downloadFile(downloadUrl)
                        }
                    Log.e("llk", "aaaaa " + Thread.currentThread().name)
                    if (!filePath.isNullOrEmpty()){
                        //3 解压文件
                        val unzipPath = ctx.cacheDir.absolutePath + File.pathSeparator + inputMsg
                        val isUnzipSuccess = ZipUtils.unZipFolderByZipFile(filePath, unzipPath)
                        if (isUnzipSuccess){
                            Log.e("llk", "start: ")
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

    private suspend fun downloadFile(url: String) : String?{
        return suspendCoroutine {
            val saveFilePath = ctx.cacheDir.absolutePath
            Log.e("llk", "downloadFile $url $saveFilePath " + Thread.currentThread().name)

            val wolfDownloader = DownloaderConfig()
                .setThreadNum(3)
                .setDownloadUrl(url)
                .setSaveDir(File(saveFilePath))
                .setDownloadListener(object : DownloadProgressListener {
                    //设置进度条的最大刻度为文件的长度
                    override fun onDownloadTotalSize(totalSize: Int) {
                        Log.e("llk", "onDownloadTotalSize: $totalSize")
                    }

                    override fun updateDownloadProgress(size: Int, percent: Float, speed: Float) {
                        Log.e("llk", "updateDownloadProgress: $percent" + Thread.currentThread().name)
                        callUpdateProgress(percent)
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