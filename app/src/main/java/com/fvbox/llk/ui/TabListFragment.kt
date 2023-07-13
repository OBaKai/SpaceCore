package com.fvbox.llk.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fvbox.R
import com.fvbox.data.BoxRepository
import com.fvbox.data.bean.box.BoxAppBean
import com.fvbox.llk.WeChatVAMaker
import com.fvbox.llk.utils.SpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TabListFragment : Fragment(R.layout.fragment_homt_tab_list) {

    companion object{
        private class AppViewHolder: RecyclerView.ViewHolder {

            val icon: ImageView
            val name: TextView
            val layout: View

            constructor(view: View) : super(view) {
                icon = view.findViewById(R.id.icon)
                name = view.findViewById(R.id.name)
                layout = view.findViewById(R.id.clLayout)
            }
        }
    }

    private var listView: RecyclerView? = null
    private var tvEmpty: TextView? = null

    private val listAdapter by lazy {
        object : RecyclerView.Adapter<AppViewHolder>() {

            private val datas = mutableListOf<BoxAppBean>()

            private var itemClick: ((Int)->Unit)? = null

            fun setItemClick(cb: ((Int)->Unit)?){
                itemClick = cb
            }

            fun setDatas(list: List<BoxAppBean>){
                datas.clear()
                datas.addAll(list)
                notifyDataSetChanged()
            }

            fun getData(pos: Int) : BoxAppBean{
                return datas[pos]
            }

            override fun getItemCount(): Int = datas.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder =
                AppViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tab_list_app, parent, false))

            override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
                val item = datas[position]

                holder.icon.setImageDrawable(item.icon)

                val inputCode = SpUtil.getString("${WeChatVAMaker.SP_UID}${item.userID}")
                holder.name.text = "${item.name}${inputCode}"

                holder.layout.setOnClickListener {
                    itemClick?.invoke(position)
                }
            }

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        listView = v?.findViewById(R.id.listView)
        tvEmpty = v?.findViewById(R.id.tvEmpty)
        return v
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listView?.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = listAdapter

            listAdapter.setItemClick {position->
                val data = listAdapter.getData(position)
                BoxRepository.launchApp(data.pkg, data.userID)
            }
        }

        GlobalScope.launch(Dispatchers.IO) {
            val appList = mutableListOf<BoxAppBean>()

            BoxRepository.getUserList().forEach { userInfo->
                val uid = userInfo.userID
                Log.e("llk", "getAppList request userInfo, uid=$uid")
                val apps = BoxRepository.getBoxAppList(uid)
                if (apps.isNotEmpty()){
                    appList.addAll(apps)
                }
            }

            Log.e("llk", "getAppList appList size=${appList.size}")

            withContext(Dispatchers.Main){
                if (appList.isEmpty()){
                    listView?.isVisible = false
                    tvEmpty?.isVisible = true
                }else{
                    listView?.isVisible = true
                    tvEmpty?.isVisible = false
                    listAdapter.setDatas(appList)
                }
            }
        }
    }
}