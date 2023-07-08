package com.fvbox.llk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.fvbox.R

class TabInputFragment : Fragment(R.layout.fragment_homt_tab_input), View.OnClickListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        v?.findViewById<ImageView>(R.id.ivAdd)?.setOnClickListener(this)
        v?.findViewById<ImageView>(R.id.ivScan)?.setOnClickListener(this)
        return v
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.ivAdd->{

            }
            R.id.ivScan->{

            }
        }
    }
}