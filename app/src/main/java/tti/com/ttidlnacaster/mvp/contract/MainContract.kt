package tti.com.ttidlnacaster.mvp.contract

import org.fourthline.cling.model.meta.Device
import tti.com.ttidlnacaster.base.BasePresenter
import tti.com.ttidlnacaster.base.BaseView

/**
 * Created by dylan_liang on 2018/2/9.
 */
interface MainContract {

    interface View: BaseView<Presenter> {
        fun showDevices(devices: Array<Device<*, *, *>>, titles: Array<String>)
        fun showSelectedDevice(title: String)
    }

    interface Presenter: BasePresenter<View> {
        fun browseDevices()
        fun selectDevice(device: Device<*, *, *>)
    }
}
