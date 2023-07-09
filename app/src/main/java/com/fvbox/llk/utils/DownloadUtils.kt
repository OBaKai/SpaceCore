package com.fvbox.llk.utils

import android.content.Context
import android.util.Log
import com.lijunhuayc.downloader.downloader.*
import java.io.File


class DownloadUtils(ctx: Context,
                    inputMsg: String,
                    private var cb: Callback
) {

    companion object{
        private const val URL = "http://118.31.78.173:9988/getFileUrl?fileName="
        private const val TAR = ".tar"
    }

    interface Callback{
        fun onUpdateProgress(percent: Float)
        fun onSuccess(filePath: String)
        fun onFail()
    }


    private var wolfDownloader: WolfDownloader? = null
    private var isDownloading: Boolean = false

    init {
        val downloadUrl = URL +inputMsg+ TAR
        val saveFilePath = ctx.cacheDir.absolutePath
        Log.e("llk", "init $downloadUrl $saveFilePath")
        wolfDownloader = DownloaderConfig()
            .setThreadNum(3)
            .setDownloadUrl(downloadUrl)
            .setSaveDir(File(saveFilePath))
            .setDownloadListener(object : DownloadProgressListener {
                //设置进度条的最大刻度为文件的长度
                override fun onDownloadTotalSize(totalSize: Int) {
                    Log.e("llk", "onDownloadTotalSize: $totalSize")
                }

                override fun updateDownloadProgress(size: Int, percent: Float, speed: Float) {
                    Log.e("llk", "updateDownloadProgress: $size $percent $speed")
                    isDownloading = true
                    cb.onUpdateProgress(percent)
                }

                override fun onDownloadSuccess(filePath: String) {
                    Log.e("llk", "onDownloadSuccess: $filePath")
                    isDownloading = false
                    cb.onSuccess(filePath)
                }

                override fun onDownloadFailed() {
                    Log.e("llk", "onDownloadFailed")
                    isDownloading = false
                    cb.onFail()
                }

                override fun onPauseDownload() {
                    Log.e("llk", "onPauseDownload")
                    isDownloading = false
                }

                override fun onStopDownload() {
                    Log.e("llk", "onStopDownload")
                    isDownloading = false
                }
            })
            .buildWolf(ctx)
    }

    fun start(){
        wolfDownloader?.startDownload()
        isDownloading = true
    }

    fun isDownloading(): Boolean = isDownloading

    fun release(){
        wolfDownloader?.stopDownload()
        isDownloading = false
    }
}