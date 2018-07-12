package tti.com.ttidlnacaster.mvp.contract

import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable.TransportState
import org.fourthline.cling.support.model.PositionInfo
import tti.com.ttidlnacaster.base.BasePresenter
import tti.com.ttidlnacaster.base.BaseView
import tti.com.ttidlnacaster.util.Constants

/**
 * Created by dylan_liang on 2018/2/9.
 */
interface MediaContract {

    interface View: BaseView<Presenter> {
        fun updateCanBeRender(canBeRender: Boolean)
        fun updateMediaTitle(mediaTitle: String)
        fun updatePanelState(transportState: TransportState)
        fun updatePositionInfo(positionInfo: PositionInfo)
    }

    interface Presenter: BasePresenter<View> {
        fun checkBrowserUrl(browserUrl: String)
        fun prepareRender()
        fun startRender(mediaTitle: String, mediaUrl: String)
        fun controlRender(controlCommand: Constants.ControlCommand)
        fun stopUpdateRenderPosition()
        fun seekRender(relativeTime: String)
        fun disconnect()
    }
}