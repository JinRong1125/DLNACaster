package tti.com.ttidlnacaster.ui.activity

import android.content.DialogInterface
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.view.Menu
import kotlinx.android.synthetic.main.activity_main.*
import tti.com.ttidlnacaster.R
import tti.com.ttidlnacaster.base.BaseActivity
import tti.com.ttidlnacaster.mvp.contract.MainContract
import tti.com.ttidlnacaster.ui.adapter.TabAdapter
import android.support.v7.app.AlertDialog
import android.view.MenuItem
import kotlinx.android.synthetic.main.fragment_youtube.*
import net.skoumal.fragmentback.BackFragmentHelper
import org.fourthline.cling.model.meta.Device
import tti.com.ttidlnacaster.dlna.ServiceManager
import tti.com.ttidlnacaster.mvp.presenter.MainPresenter
import tti.com.ttidlnacaster.ui.fragment.YoutubeFragment

/**
 * Created by dylan_liang on 2018/2/8.
 */
class MainActivity: BaseActivity(), MainContract.View {

    private val presenter by lazy { MainPresenter() }
    private val tabAdapter by lazy { TabAdapter(supportFragmentManager) }
    private var devicesDialog: AlertDialog? = null

    init {
        ServiceManager().startService()
        presenter.bindView(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUp()
    }

    override fun onBackPressed() {
        if (!BackFragmentHelper.fireOnBackPressedEvent(this))
            moveTaskToBack(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.searchDeviceButton -> presenter.browseDevices()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    override fun showDevices(devices: Array<Device<*, *, *>>, titles: Array<String>) {
        runOnUiThread(Runnable {
            devicesDialog = AlertDialog.Builder(this)
                    .setTitle(getString(R.string.active_devices))
                    .setItems(titles, DialogInterface.OnClickListener { dialog, position ->
                        presenter.selectDevice(devices[position])
                    })
                    .setPositiveButton(getString(R.string.refresh), DialogInterface.OnClickListener { dialog, button ->
                        devicesDialog?.hide()
                        presenter.browseDevices()
                    })
                    .create()
            devicesDialog?.show()
        })
    }

    override fun showSelectedDevice(title: String) {
        runOnUiThread(Runnable {
            selectedDeviceTitle.text = title
        })
    }

    private fun setUp() {
        val layoutParams = selectedDeviceTitle.layoutParams
        layoutParams.width = resources.displayMetrics.widthPixels / 3
        selectedDeviceTitle.layoutParams = layoutParams

        setSupportActionBar(toolbar)
        viewPager.adapter = tabAdapter
        viewPager.offscreenPageLimit = tabLayout.tabCount
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
                val fragment = tabAdapter.getFragment(1)
                if (fragment != null) {
                    val youtubeFragment = fragment as YoutubeFragment
                    val youtubeWebView = youtubeFragment.youtubeWebView
                    when (tab.position) {
                        0 -> youtubeWebView.onPause()
                        1 -> youtubeWebView.onResume()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {

            }
            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })
    }

    override fun updateUIState() {
    }
}