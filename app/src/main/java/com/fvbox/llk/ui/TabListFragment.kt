package com.fvbox.llk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fvbox.R

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

            private val datas = mutableListOf<String>()

            private var itemClick: ((Int)->Unit)? = null

            fun setItemClick(cb: ((Int)->Unit)?){
                itemClick = cb
            }

            fun setDatas(list: List<String>?){
                datas.clear()
                if (!list.isNullOrEmpty()){
                    datas.addAll(list)
                }
                notifyDataSetChanged()
            }

            fun getData(pos: Int) : String?{
                return if (datas.isNotEmpty()) datas[pos] else null
            }

            override fun getItemCount(): Int = datas.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder =
                AppViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tab_list_app, parent, false))

            override fun onBindViewHolder(holder: AppViewHolder, position: Int) {

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
            }
        }
    }
}