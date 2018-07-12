package tti.com.ttidlnacaster.ui.fragment

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_cast.*
import tti.com.ttidlnacaster.R
import tti.com.ttidlnacaster.base.BaseFragment
import tti.com.ttidlnacaster.dlna.SystemManager
import tti.com.ttidlnacaster.mvp.contract.CastContract
import tti.com.ttidlnacaster.mvp.presenter.CastPresenter
import tti.com.ttidlnacaster.util.Constants
import tti.com.ttidlnacaster.util.Utils

/**
 * Created by dylan_liang on 2018/2/8.
 */
class CastFragment: BaseFragment(), CastContract.View {

    private val presenter by lazy { CastPresenter() }
    private val mediaProjectionManager by lazy { setMediaProjectionManager() }
    private val progressDialog by lazy { setProgressDialog() }

    companion object {
        fun newInstance(): CastFragment {
            return CastFragment()
        }

        private val PERMISSION_REQUEST_CODE by lazy { 101 }
        private val CAPTURE_REQUEST_CODE by lazy { 102 }
    }

    init {
        presenter.bindView(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            = inflater.inflate(R.layout.fragment_cast, container, false)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CAPTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK)
            presenter.startCast(resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.requestCast(mediaProjectionManager, CAPTURE_REQUEST_CODE)
            }
        }
    }

    override fun setUp() {
        startCastButton.setOnClickListener({
            when (SystemManager.instance.castState) {
                Constants.CastState.PREPARING -> presenter.showConnecting()
                Constants.CastState.STARTED -> presenter.stopCast()
                Constants.CastState.STOPPED -> presenter.prepareCast(mediaProjectionManager, PERMISSION_REQUEST_CODE, CAPTURE_REQUEST_CODE)
            }
        })
    }

    override fun updateUIState() {
        activity.runOnUiThread(Runnable {
            when (SystemManager.instance.castState) {
                Constants.CastState.PREPARING -> progressDialog.show()
                Constants.CastState.STARTED -> {
                    castButtonLayout.setBackgroundResource(R.drawable.cast_button_red)
                    Utils.showToast(R.string.cast_started)
                    progressDialog.dismiss()
                    activity.moveTaskToBack(true)
                }
                Constants.CastState.STOPPED -> {
                    castButtonLayout.setBackgroundResource(R.drawable.cast_button_blue)
                    progressDialog.dismiss()
                }
            }
        })
    }

    fun setMediaProjectionManager(): MediaProjectionManager {
        return activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    fun setProgressDialog(): ProgressDialog {
        val progressDialog = ProgressDialog(context, ProgressDialog.STYLE_SPINNER)
        progressDialog.setMessage(getString(R.string.connecting))
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setOnCancelListener({
            presenter.disconnect()
        })
        return progressDialog
    }
}