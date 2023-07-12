package com.fvbox.llk.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.fvbox.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.gyf.immersionbar.ktx.immersionBar
import com.uuzuche.lib_zxing.activity.ZXingLibrary

class HomeActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    private val fragments by lazy {
        arrayListOf(
            TabInputFragment(),
            TabListFragment()
        )
    }

    private val titles by lazy {
        arrayListOf(
            "导入",
            "列表"
        )
    }

    private val icons by lazy {
        arrayListOf(
            getDrawable(R.drawable.ic_tab_1),
            getDrawable(R.drawable.ic_tab_2)
        )
    }

    companion object {
        fun start(ctx: Context) {
            ctx.startActivity(Intent(ctx, HomeActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //扫描二维码库初始化
        ZXingLibrary.initDisplayOpinion(this)

        immersionBar {
            fitsSystemWindows(true, R.color.white)
            statusBarDarkFont(true)
        }

        viewPager = findViewById(R.id.viewpager)
        tabLayout = findViewById(R.id.tabLayout)

        viewPager.adapter = object : FragmentStateAdapter(this@HomeActivity) {
            override fun getItemCount(): Int {
                return fragments.size
            }

            override fun createFragment(position: Int): Fragment {
                return fragments[position]
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
            tab.icon = icons[position]
        }.attach()
    }
}