package tti.com.ttidlnacaster.mvp.presenter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.support.v4.content.ContextCompat
import tti.com.ttidlnacaster.R
import tti.com.ttidlnacaster.TTIDLNACaster
import tti.com.ttidlnacaster.dlna.SystemManager
import tti.com.ttidlnacaster.dlna.service.CastService
import tti.com.ttidlnacaster.dlna.service.MediaService
import tti.com.ttidlnacaster.mvp.contract.CastContract
import tti.com.ttidlnacaster.ui.fragment.CastFragment
import tti.com.ttidlnacaster.util.Constants
import tti.com.ttidlnacaster.util.Utils

/**
 * Created by dylan_liang on 2018/2/9.
 */
class CastPresenter: CastContract.Presenter {

    private var view: CastContract.View? = null

    override fun bindView(view: CastContract.View) {
        this.view = view
        SystemManager.instance.castView = view
    }

    override fun prepareCast(mediaProjectionManager: MediaProjectionManager,
                             permissionCode: Int, captureCode: Int) {
        if (SystemManager.instance.selectedDevice != null) {
            if (ContextCompat.checkSelfPermission(TTIDLNACaster.context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(TTIDLNACaster.context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED)
                requestCast(mediaProjectionManager, captureCode)
            else {
                val castFragment = view as CastFragment
                castFragment.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE), permissionCode)
            }
        }
        else
            Utils.showToast(R.string.choose_device)
    }

    override fun requestCast(mediaProjectionManager: MediaProjectionManager, captureCode: Int) {
        val castFragment = view as CastFragment
        castFragment.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), captureCode)
    }

    override fun startCast(resultCode: Int, data: Intent?) {
        SystemManager.instance.castState = Constants.CastState.PREPARING
        view?.updateUIState()

        if (SystemManager.instance.renderState == Constants.RenderState.STARTED) {
            val mediaService = Intent(TTIDLNACaster.context, MediaService::class.java)
            mediaService.action = Constants.RENDER_ACTION_CONTROL
            mediaService.putExtra(Constants.RENDER_INTENT_COMMAND, Constants.ControlCommand.STOP)
            TTIDLNACaster.context.startService(mediaService)
        }
        val castService = Intent(TTIDLNACaster.context, CastService::class.java)
        castService.action = Constants.CAST_ACTION_START
        castService.putExtra(Constants.CAST_INTENT_RESULT, resultCode)
        castService.putExtra(Constants.CAST_INTENT_DATA, data)
        TTIDLNACaster.context.startService(castService)
    }

    override fun stopCast() {
        val castService = Intent(TTIDLNACaster.context, CastService::class.java)
        castService.action = Constants.CAST_ACTION_STOP
        TTIDLNACaster.context.startService(castService)
    }

    override fun disconnect() {
        val castService = Intent(TTIDLNACaster.context, CastService::class.java)
        castService.action = Constants.CAST_ACTION_DISCONNECT
        TTIDLNACaster.context.startService(castService)
    }

    override fun showConnecting() {
        Utils.showToast(R.string.connecting)
    }
}