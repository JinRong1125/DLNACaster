package tti.com.ttidlnacaster.dlna.server

import org.eclipse.jetty.util.resource.FileResource
import java.net.URL

/**
 * Created by dylan_liang on 2018/2/14.
 */
class StreamResource(url: URL): FileResource(url) {

    private var length: Long = 0

    override fun length(): Long {
        if (file.length() > 0) {
            length = file.length()
            return length + 1
        }
        return length++
    }
}