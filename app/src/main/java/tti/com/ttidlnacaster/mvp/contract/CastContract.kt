package tti.com.ttidlnacaster.mvp.contract

import android.content.Intent
import android.media.projection.MediaProjectionManager
import tti.com.ttidlnacaster.base.BasePresenter
import tti.com.ttidlnacaster.base.BaseView

/**
 * Created by dylan_liang on 2018/2/9.
 */
interface CastContract {

    interface View: BaseView<Presenter> {}

    interface Presenter: BasePresenter<View> {
        fun prepareCast(mediaProjectionManager: MediaProjectionManager,
                        permissionCode: Int,
                        captureCode: Int)
        fun requestCast(mediaProjectionManager: MediaProjectionManager, captureCode: Int)
        fun startCast(resultCode: Int, data: Intent?)
        fun stopCast()
        fun disconnect()
        fun showConnecting()
    }
}