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
            val itemLayout: View
            val delLayout: View

            constructor(view: View) : super(view) {
                icon = view.findViewById(R.id.icon)
                name = view.findViewById(R.id.name)
                itemLayout = view.findViewById(R.id.ll_item)
                delLayout = view.findViewById(R.id.txt_delete)
            }
        }
    }

    private var listView: RecyclerView? = null
    private var tvEmpty: TextView? = null

    private val listAdapter by lazy {
        object : RecyclerView.Adapter<AppViewHolder>() {

            private val datas = mutableListOf<BoxAppBean>()

            private var itemClick: ((Int, Boolean)->Unit)? = null

            fun setItemClick(cb: ((Int, Boolean)->Unit)?){
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

            fun removeData(pos: Int){
                datas.removeAt(pos)
                notifyDataSetChanged()
            }

            override fun getItemCount(): Int = datas.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder =
                AppViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tab_list_app, parent, false))

            override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
                val item = datas[position]

                holder.icon.setImageDrawable(item.icon)

                val inputCode = SpUtil.getString("${WeChatVAMaker.SP_UID}${item.userID}")
                holder.name.text = "${item.name}${inputCode ?: ""}"

                holder.itemLayout.setOnClickListener {
                    itemClick?.invoke(position, false)
                }
                holder.delLayout.setOnClickListener {
                    itemClick?.invoke(position, true)
                }
            }

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        listView = v?.findViewById(R.id.listView)
        listView?.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = listAdapter

            listAdapter.setItemClick {position, isDel->
                val data = listAdapter.getData(position)
                if (isDel){
                    removeVApp(data.userID, position)
                }else{
                    BoxRepository.launchApp(data.pkg, data.userID)
                }
            }
        }

        tvEmpty = v?.findViewById(R.id.tvEmpty)
        return v
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private var isLoading = false
    private var isRemoveing = false

    private fun loadData(){
        if (isLoading) return

        isLoading = true

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
                isLoading = false
            }
        }
    }

    private fun removeVApp(uid: Int, pos: Int){
        if (isRemoveing) return
        isRemoveing = true
        Log.e("llk", "removeVApp uid=$uid")
        GlobalScope.launch(Dispatchers.IO) {
            BoxRepository.uninstall(WeChatVAMaker.WX_PKG, uid)
            BoxRepository.deleteUser(uid)
            SpUtil.remove("${WeChatVAMaker.SP_UID}$uid")

            withContext(Dispatchers.Main){
                listAdapter.removeData(pos)
                isRemoveing = false

                if (listAdapter.itemCount == 0){
                    listView?.isVisible = false
                    tvEmpty?.isVisible = true
                }
            }
        }
    }
}