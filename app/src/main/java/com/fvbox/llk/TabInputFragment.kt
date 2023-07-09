package com.fvbox.llk

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.fvbox.R
import com.fvbox.util.showToast
import com.permissionx.guolindev.PermissionX
import com.uuzuche.lib_zxing.activity.CaptureActivity
import com.uuzuche.lib_zxing.activity.CodeUtils


class TabInputFragment : Fragment(R.layout.fragment_homt_tab_input), View.OnClickListener {

    private var waveView: WaveView? = null

    private fun performNextStep(inputMsg: String?){
        if (inputMsg.isNullOrEmpty()){
            showToast("输入信息为空，请重新操作")
            return
        }
        showToast("aaa="+inputMsg)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        v?.findViewById<ImageView>(R.id.ivAdd)?.setOnClickListener(this)
        v?.findViewById<ImageView>(R.id.ivScan)?.setOnClickListener(this)
        waveView = v?.findViewById(R.id.waveView)
        return v
    }

    private fun updateProgress(progress: Float){
        waveView?.apply {
            precent = progress
            start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 999){
            if (null != data) {
                val bundle = data.extras ?: return
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    performNextStep(bundle.getString(CodeUtils.RESULT_STRING))
                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    showToast("解析二维码失败")
                }
            }
        }
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.ivAdd->{
                InputTextDialog(requireContext()) {
                    performNextStep(it)
                }.show()
            }
            R.id.ivScan->{
                PermissionX.init(this)
                    .permissions(Manifest.permission.CAMERA)
                    .request { allGranted, _, _ ->
                        if (allGranted) {
                            val intent = Intent(requireContext(), CaptureActivity::class.java)
                            startActivityForResult(intent, 999)
                        } else {
                            showToast("未获取到相机权限，无法使用该功能")
                        }
                    }
            }
        }
    }
}