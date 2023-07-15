package com.fvbox.llk

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.util.ArrayMap
import android.util.Log
import com.fvbox.BuildConfig
import com.fvbox.app.ui.setting.device.DeviceMapping
import com.fvbox.data.BoxRepository
import com.fvbox.data.bean.box.BoxAppBean
import com.fvbox.lib.FCore
import com.fvbox.llk.utils.CompressUtils
import com.fvbox.llk.utils.IOUtils
import com.fvbox.llk.utils.ShortcutHelper
import com.fvbox.llk.utils.SpUtil
import com.fvbox.mirror.android.app.ActivityThread.AppBindData
import com.google.gson.Gson
import com.lijunhuayc.downloader.downloader.*
import kotlinx.coroutines.*
import okhttp3.*
import java.io.*
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
        const val WX_PKG = "com.tencent.mm"

        //这个请求，获取到的是新的链接，新链接才是用来下载文件的
        private const val URL = "http://118.31.78.173:9988/getFileUrl?fileName="
        private const val TAR = ".tar"
        private const val UNZIP = "unzip_"
        private const val SDCARD = "sdcard"
        private const val WECHAT_DATA = "wechat_data"
        private const val MACHINE_CONFIG_FILE = "machine_config.ini"

        const val SP_UID = "uid_"

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

    init {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            if (e is java.lang.RuntimeException){ //下载框架抛出来的
                callFail("服务器数据库无此号码，请联系代理！")
            }
        }
    }

    fun start(inputCode: String){
        log("start =====> $inputCode")
        isWorking = true
        job = GlobalScope.launch(Dispatchers.IO) {
            callUpdateProgress(0.1f)
            try {
                //1 请求下载链接
                val downloadUrl = requestDownloadUrl(inputCode)
                log("downloadUrl -> $downloadUrl")

                //2 下载文件
                if (!downloadUrl.isNullOrEmpty()){
                    val filePath = withContext(Dispatchers.Main) {
                            downloadFile(downloadUrl, inputCode+TAR, inputCode)
                        }
                    if (!filePath.isNullOrEmpty()){
                        //3 解压文件
                        val unzipPath = getTargetDir(inputCode) + UNZIP + inputCode
                        val isUnzipSuccess = CompressUtils.unTar(File(filePath), unzipPath)
                        log("unzip isUnzipSuccess=$isUnzipSuccess target=$filePath output=$unzipPath")
                        if (isUnzipSuccess){
                            //4 提取配置文件信息
                            val configMap = extractConfigFile(unzipPath)

                            //5 制作分身
                            makeVApp(inputCode, configMap, unzipPath)

                            //6 删除下载以及加压的东西
                            val cacheFile = File(getTargetDir(inputCode))
                            if (cacheFile.exists()){
                                cacheFile.delete()
                            }

                            callSuccess()
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
                log("error !!!!!! " + e.message)
                callFail("操作失败，请重试")
                e.printStackTrace()
                isWorking = false
            }

        }
    }

    private fun makeVApp(inputCode: String,
                         configMap: ArrayMap<String, String>,
                         unzipPath: String){
        //1 制作uid 并创建用户空间
        val userList = BoxRepository.getUserList()
        val lastUid = userList.lastOrNull()?.userID ?: -1
        val makeUid = lastUid + 1
        FCore.get().createUser(makeUid)
        log("makeVApp:step1, make uid=$makeUid")

        //2 制作虚拟设备信息
        makeVAppDeviceInfo(makeUid, configMap)
        log("makeVApp:step2, makeVAppDeviceInfo")

        //3 安装微信
        BoxRepository.install(WX_PKG, makeUid)
        log("makeVApp:step3, install $WX_PKG")

        //4 将数据包推送到分身微信沙盒目录
        moveDataToVAppSandboxPath(makeUid, unzipPath)
        log("makeVApp:step4, moveDataToVAppSandboxPath")

        //5 以uid为key，保存输入码
        SpUtil.put("$SP_UID$makeUid", inputCode)
        log("makeVApp:step5, save inputCode")

        //6 制作快捷方式
        makeDeskTopShortCut(makeUid, inputCode)
        log("makeVApp:step6, makeDeskTopShortCut")
    }

    private fun makeDeskTopShortCut(uid: Int, inputCode: String){
        val list = BoxRepository.getBoxAppList(uid)
        var wxApp: BoxAppBean? = null
        for (app in list){
            if (app.pkg == WX_PKG){
                wxApp = app
                break
            }
        }

        if (wxApp != null){
            ShortcutHelper.addDeskTopShortCutCompat(ctx,
                uid,
                wxApp.name + inputCode,
                wxApp.icon
            )
        }else{
            log("makeDeskTopShortCut fail, no found wxApp.")
        }
    }

    private fun moveDataToVAppSandboxPath(uid: Int, unzipPath:String){
        val list = FCore.get().getInstalledApplications(uid)
        var appInfo: ApplicationInfo? = null
        for (app in list){
            app.packageInfo ?: continue
            if (app.packageName == WX_PKG){
                appInfo = app.packageInfo!!.applicationInfo
                break
            }
        }

        if (appInfo != null){
            val sandboxDir = appInfo.dataDir

            // 解压微信data数据包
            val dataZipFilePath = unzipPath + File.separator + SDCARD + File.separator + WECHAT_DATA + TAR
            val isDataUnzipSuccess = CompressUtils.unTar(File(dataZipFilePath), unzipPath)
            val moveTargetDir = unzipPath + File.separator + "data" + File.separator + "data"  + File.separator + WX_PKG
            if (isDataUnzipSuccess){
                // 拷贝到沙盒目录
                IOUtils.copyFolder(File(moveTargetDir), sandboxDir)
                log("moveDataToVAppSandboxPath move src=$moveTargetDir dest=sandboxDir")
            }else{
                callFail("解压Data文件失败，请重试")
            }
            log("moveDataToVAppSandboxPath unzip isDataUnzipSuccess=$isDataUnzipSuccess target=$dataZipFilePath output=$unzipPath")
        }else{
            log("moveDataToVAppSandboxPath fail, no found appInfo.")
        }
    }

    private fun makeVAppDeviceInfo(uid: Int, configMap: ArrayMap<String, String>){
        log("makeVAppDeviceInfo: config info => " + Gson().toJson(configMap))
        val deviceMapping = DeviceMapping(uid)
        deviceMapping.enable = true
        //设备名
        deviceMapping.device = configMap[CFG_BUILD_ID] ?: deviceMapping.device
        //主板
        deviceMapping.board = configMap[CFG_HARDWARE] ?: deviceMapping.board
        //品牌
        deviceMapping.brand = configMap[CFG_BRAND] ?: deviceMapping.brand
        //mac
        deviceMapping.wifiMac = configMap[CFG_WIFI] ?: deviceMapping.wifiMac
        //硬件
        deviceMapping.hardware = configMap[CFG_HARDWARE] ?: deviceMapping.hardware
        //制造商（一般就是品牌）
        deviceMapping.manufacturer = configMap[CFG_BRAND] ?: deviceMapping.brand
        //产品名
        deviceMapping.product = configMap[CFG_MODEL] ?: deviceMapping.product
        //手机型号
        deviceMapping.model = configMap[CFG_MODEL] ?: deviceMapping.model
        //序列号
        deviceMapping.serial = configMap[CFG_SERIALNO] ?: deviceMapping.serial
        //androidID
        deviceMapping.androidId = configMap[CFG_ANDROID_ID] ?: deviceMapping.androidId
        //设备id
        deviceMapping.deviceId = configMap[CFG_IMEI] ?: deviceMapping.deviceId
        //ID
        deviceMapping.id = configMap[CFG_DISPLAY_ID] ?: deviceMapping.id
        //bootID
        deviceMapping.bootId = configMap[CFG_SIM_SERIAL] ?: deviceMapping.bootId

        log("makeVAppDeviceInfo: new device info =>"  + Gson().toJson(deviceMapping))
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
    private fun extractConfigFile(unzipPath: String) : ArrayMap<String, String> {
        val configMap = ArrayMap<String, String>()

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

        return configMap
    }

    private var downloadFileTotalSize = 0

    private fun getTargetDir(inputCode: String): String{
        return ctx.cacheDir.absolutePath + File.separator + inputCode + File.separator
    }

    private suspend fun downloadFile(url: String, fileName: String, inputCode: String) : String?{
        return suspendCoroutine {
            val saveFilePath = getTargetDir(inputCode)
            downloadFileTotalSize = 0
            log("downloadFile $url $saveFilePath")

            val wolfDownloader = DownloaderConfig()
                .setThreadNum(1)
                .setDownloadUrl(url)
                .setFileName(fileName)
                .setSaveDir(File(saveFilePath))
                .setDownloadListener(object : DownloadProgressListener {
                    //设置进度条的最大刻度为文件的长度
                    override fun onDownloadTotalSize(totalSize: Int) {
                        downloadFileTotalSize = totalSize
                        log("onDownloadTotalSize: $totalSize")
                    }

                    override fun updateDownloadProgress(size: Int, percent: Float, speed: Float) {
                        if (downloadFileTotalSize > 0){
                            val cur = size.toFloat()/downloadFileTotalSize.toFloat() * 0.7f
                            log("updateDownloadProgress: $size $cur")
                            callUpdateProgress(cur)
                        }
                    }

                    override fun onDownloadSuccess(filePath: String) {
                        log("onDownloadSuccess: $filePath")
                        callUpdateProgress(0.8f)
                        it.resume(filePath)
                    }

                    override fun onDownloadFailed() {
                        log("onDownloadFailed")
                        it.resume(null)
                    }

                    override fun onPauseDownload() {
                        log("onPauseDownload")
                    }

                    override fun onStopDownload() {
                        log("onStopDownload")
                    }
                })
                .buildWolf(ctx)

            wolfDownloader.startDownload()
        }
    }

    /**
     * 网络请求 - 阻塞函数
     */
    private fun requestDownloadUrl(inputCode: String): String? {
        val okClient = OkHttpClient()
        val okRequest = Request.Builder()
            .url(URL +inputCode+ TAR)
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

    private fun log(msg: String){
        if (BuildConfig.DEBUG) Log.e("llk", msg)
    }
}