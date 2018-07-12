package tti.com.ttidlnacaster.mvp.presenter

import org.fourthline.cling.model.meta.Device
import tti.com.ttidlnacaster.dlna.SystemManager
import tti.com.ttidlnacaster.mvp.contract.MainContract

/**
 * Created by dylan_liang on 2018/2/9.
 */
class MainPresenter: MainContract.Presenter {

    private var view: MainContract.View? = null

    override fun bindView(view: MainContract.View) {
        this.view = view
    }

    override fun browseDevices() {
        val devices = SystemManager.instance.dmrDevices.toTypedArray()
        val titleArray = Array<String>(devices.size, { i -> devices[i].details.friendlyName })
        view?.showDevices(devices, titleArray)
    }

    override fun selectDevice(device: Device<*, *, *>) {
        SystemManager.instance.selectedDevice = device
        view?.showSelectedDevice(device.details.friendlyName)
    }
}