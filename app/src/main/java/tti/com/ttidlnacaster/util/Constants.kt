package tti.com.ttidlnacaster.util

import android.net.Uri
import tti.com.ttidlnacaster.TTIDLNACaster
import java.io.File

/**
 * Created by dylan_liang on 2018/2/8.
 */
object Constants {

    val CAST_STREAM_URI = Uri.Builder()
            .scheme("http")
            .encodedAuthority(Utils.getIPAddress(true) + ":8181")
            .appendPath("dlna")
            .appendPath("stream.flv")
            .build().toString()

    val CAST_FOLDER_PATH = TTIDLNACaster.context.getExternalFilesDir(null).canonicalPath +
            File.separator + "cast"

    val CAST_FILE_PATH = CAST_FOLDER_PATH + File.separator + "stream.flv"

    val YOUTUBE_WATCH = "watch?v="
    val YOUTUBE_URL = "https://www.youtube.com"
    val JAVASCRIPT_INTERFACE = "javascript:window.android.onUrlChange(window.location.href);"

    val CAST_ACTION_START = "castservice.action.startcasting"
    val CAST_ACTION_STOP = "castservice.action.stopcasting"
    val CAST_ACTION_DISCONNECT = "castservice.action.disconnecting"

    val RENDER_ACTION_START = "mediaservice.action.startrendering"
    val RENDER_ACTION_CONTROL = "mediaservice.action.controlrendering"
    val RENDER_ACTION_DISCONNECT = "mediaservice.action.disconnecting"
    val RENDER_ACTION_STOP_UPDATE = "mediaservice.action.stopupdating"
    val RENDER_NOTIFICATION_REPLAY = "mediaservice.action.notificationreplaying"
    val RENDER_NOTIFICATION_PLAY_PAUSE = "mediaservice.action.notificationplayingpauseing"
    val RENDER_NOTIFICATION_STOP = "mediaservice.action.notificationstopping"

    enum class CastState {
        PREPARING, STARTED, STOPPED
    }

    enum class RenderState {
        PREPARING, ERROR, STARTED, STOPPED
    }

    enum class ControlCommand {
        PLAY_PAUSE, STOP, SEEK, REPLAY
    }

    val CAST_INTENT_RESULT = "cast_intent_result"
    val CAST_INTENT_DATA = "cast_intent_data"

    val RENDER_INTENT_TITLE = "render_intent_title"
    val RENDER_INTENT_DATA = "render_intent_data"
    val RENDER_INTENT_COMMAND = "render_intent_command"
    val RENDER_INTENT_RELATIVE_TIME = "render_intent_relativetime"

    val CAST_NOTFICATION_ID = 1001
    val CAST_NOTIFICATION_CHANNEL_ID = "cast_notification_channel_id"

    val RENDER_NOTIFICATION_ID = 1002
    val RENDER_NOTIFICATION_CHANNEL_ID = "render_notification_channel_id"
}