package com.fvbox.llk

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.EditText
import com.fvbox.R
import com.fvbox.util.extension.dp
import com.fvbox.util.showToast


class InputTextDialog(context: Context, private val callback: (String)->Unit) : Dialog(context, R.style.DialogFragmentStyle) {

    private val rootView : View

    init {
        rootView = View.inflate(context, R.layout.dialog_input_text, null)
        setContentView(rootView)

        val window = window
        val params = window!!.attributes
        params.width = 330.dp.toInt()
        params.height = LayoutParams.WRAP_CONTENT
        window.attributes = params
        window.setGravity(Gravity.CENTER)

        setCancelable(true)
        setCanceledOnTouchOutside(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val et = rootView.findViewById<EditText>(R.id.et)

        rootView.findViewById<View>(R.id.btn).setOnClickListener {
            if (et.text.isNullOrEmpty()){
                showToast("请输入内容")
                return@setOnClickListener
            }

            callback(et.text.toString())
            dismiss()
        }
    }
}
