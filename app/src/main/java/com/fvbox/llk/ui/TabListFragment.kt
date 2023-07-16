package com.fvbox.llk.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.view.animation.LinearInterpolator
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
import com.fvbox.llk.utils.ShortcutHelper
import com.fvbox.llk.utils.SpUtil
import com.gxz.library.SwapRecyclerView
import com.gxz.library.SwapWrapperUtils
import com.gxz.library.SwipeMenuBuilder
import com.gxz.library.bean.SwipeMenu
import com.gxz.library.bean.SwipeMenuItem
import com.gxz.library.view.SwipeMenuLayout
import com.gxz.library.view.SwipeMenuView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TabListFragment : Fragment(R.layout.fragment_homt_tab_list), SwipeMenuBuilder {

    companion object{
        private class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val icon: ImageView
            val name: TextView
            val itemLayout: View

            init {
                icon = view.findViewById(R.id.icon)
                name = view.findViewById(R.id.name)
                itemLayout = view.findViewById(R.id.clLayout)
            }
        }
    }

    private var listView: SwapRecyclerView? = null
    private var tvEmpty: TextView? = null

    private val listAdapter by lazy {
        object : RecyclerView.Adapter<AppViewHolder>() {

            private val swipeMenuBuilder: SwipeMenuBuilder = this@TabListFragment

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

            fun removeData(pos: Int){
                datas.removeAt(pos)
                notifyDataSetChanged()
            }

            override fun getItemCount(): Int = datas.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder{
                val menuView: SwipeMenuView = swipeMenuBuilder.create()
                val swipeMenuLayout: SwipeMenuLayout = SwapWrapperUtils.wrap(
                    parent,
                    R.layout.item_tab_list_app,
                    menuView,
                    BounceInterpolator(),
                    LinearInterpolator()
                )
               return AppViewHolder(swipeMenuLayout)
            }

            override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
                val item = datas[position]

                holder.icon.setImageDrawable(item.icon)

                val inputCode = SpUtil.getString("${WeChatVAMaker.SP_UID}${item.userID}")
                holder.name.text = "${item.name}${inputCode ?: ""}"

                holder.itemLayout.setOnClickListener {
                    itemClick?.invoke(position)
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

            listAdapter.setItemClick {position->
                val data = listAdapter.getData(position)
                BoxRepository.launchApp(data.pkg, data.userID)
            }
        }

        tvEmpty = v?.findViewById(R.id.tvEmpty)
        return v
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onPause() {
        super.onPause()
        for (i in 0 until listAdapter.itemCount){
            listView?.smoothCloseMenu(i)
        }
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
                ShortcutHelper.removeDeskTopShortCutCompat(requireContext(), uid)
                listView?.smoothCloseMenu(pos)
                listAdapter.removeData(pos)
                isRemoveing = false

                if (listAdapter.itemCount == 0){
                    listView?.isVisible = false
                    tvEmpty?.isVisible = true
                }
            }
        }
    }

    private val mOnSwipeItemClickListener =
        SwipeMenuView.OnMenuItemClickListener { pos, _, _ ->
            val data = listAdapter.getData(pos)
            removeVApp(data.userID, pos)
        }

    override fun create(): SwipeMenuView {
        val menu = SwipeMenu(requireContext())
        val item = SwipeMenuItem(requireContext())
        item.setTitle("删除")
            .setTitleColor(Color.WHITE)
            .background = ColorDrawable(Color.RED)
        menu.addMenuItem(item)
        val menuView = SwipeMenuView(menu)
        menuView.setOnMenuItemClickListener(mOnSwipeItemClickListener)
        return menuView
    }
}