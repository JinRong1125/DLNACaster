package tti.com.ttidlnacaster.base

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View

/**
 * Created by dylan_liang on 2018/2/8.
 */
abstract class BaseFragment: Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUp()
    }

    abstract fun setUp()
}