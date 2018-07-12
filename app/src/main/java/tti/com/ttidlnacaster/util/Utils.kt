package tti.com.ttidlnacaster.util

import android.widget.Toast
import tti.com.ttidlnacaster.TTIDLNACaster
import java.net.NetworkInterface
import java.util.*

/**
 * Created by dylan_liang on 2018/2/14.
 */
object Utils {

    fun showToast(stringResource: Int) {
        val context = TTIDLNACaster.context
        Toast.makeText(context, context.getString(stringResource), Toast.LENGTH_SHORT).show()
    }

    fun getIPAddress(useIPv4: Boolean): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.getInetAddresses())
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress()) {
                        val sAddr = addr.getHostAddress()
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        val isIPv4 = sAddr.indexOf(':') < 0

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr
                        } else {
                            if (!isIPv4) {
                                val delim = sAddr.indexOf('%') // drop ip6 zone suffix
                                return if (delim < 0) sAddr.toUpperCase() else sAddr.substring(0, delim).toUpperCase()
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
        }
        // for now eat exceptions
        return ""
    }
}