package tti.com.ttidlnacaster.ui.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import tti.com.ttidlnacaster.ui.fragment.CastFragment
import tti.com.ttidlnacaster.ui.fragment.YoutubeFragment
import android.view.ViewGroup



/**
 * Created by dylan_liang on 2018/2/8.
 */
class TabAdapter(fragmentManager: FragmentManager): FragmentStatePagerAdapter(fragmentManager) {

    private val pagesMap by lazy { mutableMapOf<Int, Fragment>() }

    companion object {
        private val TAB_COUNT by lazy { 2 }
    }

    override fun getCount(): Int {
        return TAB_COUNT
    }

    override fun getItem(position: Int): Fragment? {
        return when (position) {
            0 -> {
                val fragment = CastFragment.newInstance()
                pagesMap.put(position, fragment)
                fragment
            }
            1 -> {
                val fragment = YoutubeFragment.newInstance()
                pagesMap.put(position, fragment)
                fragment
            }
            else -> null
        }
    }

    override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
        pagesMap.remove(position)
    }

    fun getFragment(position: Int): Fragment? {
        return pagesMap[position]
    }
}
