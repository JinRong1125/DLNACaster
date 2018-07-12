package tti.com.ttidlnacaster.dlna.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaFormat
import android.os.*
import android.support.v4.app.NotificationCompat
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable
import org.fourthline.cling.support.contentdirectory.DIDLParser
import org.fourthline.cling.support.model.*
import org.fourthline.cling.support.model.item.Movie
import tti.com.ttidlnacaster.R
import tti.com.ttidlnacaster.TTIDLNACaster
import tti.com.ttidlnacaster.dlna.SystemManager
import tti.com.ttidlnacaster.ui.activity.MainActivity
import tti.com.ttidlnacaster.util.Constants
import tti.com.ttidlnacaster.util.PlayBackCommand
import tti.com.ttidlnacaster.util.Utils

/**
 * Created by dylan_liang on 2018/2/27.
 */
class MediaService: Service() {

    private var disconnectTimer: DisconnectTimer? = null
    private val notificationManager by lazy { setNotificationManager() }

    private var commandHandler: CommandHandler? = null
    private var renderEventHandler: RenderEventHandler? = null

    private var mediaTitle = ""
    private var mediaUrl = ""
    private var isReStarted = false
    private var beDisconnect = false

    companion object {
        private val FLAG_STOP_MEDIA by lazy { 202 }
        private val FLAG_STOP_CONNECT by lazy { 203 }
        private val FLAG_CONNECT_FAILED by lazy { 204 }
        private val FLAG_CONNECT_TIMEOUT by lazy { 205 }

        private val MESSAGE_GET_POSITION by lazy { 301 }
        private val FLAG_GET_TRANSPORT by lazy { 302 }
        private val FLAG_CONTROL_PLAY_PAUSE by lazy { 303 }

        private val CONNECT_DELAY: Long = 500
        private val CONNECT_TIMEOUT: Long = 20000
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        notificationManager.cancel(Constants.RENDER_NOTIFICATION_ID)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.RENDER_ACTION_START -> prepareRender(intent)
            Constants.RENDER_ACTION_CONTROL -> controlRender(intent)
            Constants.RENDER_ACTION_DISCONNECT -> stopRender(FLAG_STOP_CONNECT)
            Constants.RENDER_ACTION_STOP_UPDATE -> stopUpdatePosition()
            Constants.RENDER_NOTIFICATION_REPLAY -> startRender()
            Constants.RENDER_NOTIFICATION_PLAY_PAUSE ->
                PlayBackCommand.getTransportInfo(commandHandler, FLAG_CONTROL_PLAY_PAUSE)
            Constants.RENDER_NOTIFICATION_STOP -> stopRender(FLAG_STOP_MEDIA)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return MediaBinder()
    }

    inner class MediaBinder: Binder() {
        val mediaService: MediaService
            get() = this@MediaService
    }

    fun createMetaData(mediaTitle: String, mediaUrl: String): String {
        val content = DIDLContent()
        content.addItem(
                Movie("", "", mediaTitle, "",
                        Res(MediaFormat.MIMETYPE_VIDEO_AVC, 0, "00:00:00", 0, mediaUrl)))
        return DIDLParser().generate(content)
    }

    fun prepareRender(intent: Intent) {
        if (SystemManager.instance.renderState == Constants.RenderState.PREPARING) {
            mediaTitle = intent.getStringExtra(Constants.RENDER_INTENT_TITLE)
            mediaUrl = intent.getStringExtra(Constants.RENDER_INTENT_DATA)

            Handler().postDelayed(Runnable {
                startRender()
            }, CONNECT_DELAY)
        }
    }

    fun startRender() {
        commandHandler = CommandHandler()
        SystemManager.instance.systemService.eventHandler = null

        PlayBackCommand.playNewMedia(commandHandler, mediaUrl, createMetaData(mediaTitle, mediaUrl))

        beDisconnect = false
        disconnectTimer = DisconnectTimer()
        disconnectTimer?.start()
    }

    fun controlRender(intent: Intent) {
        if (SystemManager.instance.renderState == Constants.RenderState.STARTED) {
            val command = intent.getSerializableExtra(Constants.RENDER_INTENT_COMMAND)

            when (command) {
                Constants.ControlCommand.PLAY_PAUSE ->
                    PlayBackCommand.getTransportInfo(commandHandler, FLAG_CONTROL_PLAY_PAUSE)
                Constants.ControlCommand.STOP ->
                    stopRender(FLAG_STOP_MEDIA)
                Constants.ControlCommand.SEEK -> {
                    val relativeTime = intent.getStringExtra(Constants.RENDER_INTENT_RELATIVE_TIME)
                    PlayBackCommand.seek(commandHandler, relativeTime)
                }
                Constants.ControlCommand.REPLAY -> {
                    if (!isReStarted) {
                        isReStarted = true
                        startRender()
                    }
                }
            }
        }
    }

    fun stopRender(flag: Int) {
        disconnectTimer?.cancel()

        commandHandler?.removeCallbacksAndMessages(null)
        renderEventHandler?.removeCallbacksAndMessages(null)

        SystemManager.instance.systemService.eventHandler = null
        SystemManager.instance.renderState = Constants.RenderState.STOPPED
        SystemManager.instance.mediaView?.updateUIState()

        isReStarted = false

        when (flag) {
            FLAG_STOP_MEDIA -> {
                notificationManager.cancel(Constants.RENDER_NOTIFICATION_ID)
                PlayBackCommand.stop(null)
                Utils.showToast(R.string.render_stopped)
            }
            FLAG_STOP_CONNECT -> beDisconnect = true
            FLAG_CONNECT_FAILED -> Utils.showToast(R.string.connect_failed)
            FLAG_CONNECT_TIMEOUT -> Utils.showToast(R.string.connection_timed_out)
        }
    }

    fun stopUpdatePosition() {
        commandHandler?.removeMessages(MESSAGE_GET_POSITION)
    }

    inner class DisconnectTimer: CountDownTimer(CONNECT_TIMEOUT, CONNECT_TIMEOUT) {
        override fun onFinish() {
            stopRender(FLAG_CONNECT_TIMEOUT)
        }

        override fun onTick(millisUntilFinished: Long) {
        }
    }

    inner class CommandHandler: Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                PlayBackCommand.PLAY_NEW_MEDIA_SUCCEEDED -> {
                    if (!beDisconnect) {
                        isReStarted = false
                        disconnectTimer?.cancel()
                        SystemManager.instance.renderState = Constants.RenderState.STARTED
                        SystemManager.instance.mediaView?.updateUIState()
                        createRenderNotification(R.drawable.icon_pause, getString(R.string.pause))
                        renderEventHandler = RenderEventHandler()
                        SystemManager.instance.systemService.eventHandler = renderEventHandler
                        commandHandler?.sendEmptyMessage(MESSAGE_GET_POSITION)
                    }
                }
                PlayBackCommand.PLAY_NEW_MEDIA_FAILED ->
                    stopRender(FLAG_CONNECT_FAILED)
                PlayBackCommand.GET_TRANSPORT_SUCCEEDED -> {
                    if (FLAG_CONTROL_PLAY_PAUSE == msg.arg1) {
                        val transportInfo = msg.obj as TransportInfo
                        when (transportInfo.currentTransportState) {
                            TransportState.PLAYING -> PlayBackCommand.pause(null)
                            TransportState.PAUSED_PLAYBACK -> PlayBackCommand.play(null)
                            else -> { }
                        }
                    }
                }
                PlayBackCommand.GET_POSITION_SUCCEEDED ->
                    SystemManager.instance.mediaView?.updatePositionInfo(msg.obj as PositionInfo)
                PlayBackCommand.SEEK_SUCCEEDED ->
                    commandHandler?.sendEmptyMessage(MESSAGE_GET_POSITION)
                PlayBackCommand.SEEK_FAILED ->
                    commandHandler?.sendEmptyMessage(MESSAGE_GET_POSITION)
                MESSAGE_GET_POSITION -> {
                    PlayBackCommand.getPositionInfo(commandHandler)
                    commandHandler?.sendEmptyMessageDelayed(MESSAGE_GET_POSITION, 1000)
                }
            }
        }
    }

    inner class RenderEventHandler: Handler() {
        override fun handleMessage(msg: Message) {
            if (SystemManager.instance.renderState == Constants.RenderState.STARTED
                    && msg.what == SystemManager.instance.MESSAGE_RENDER_EVENT) {
                val transportState = msg.obj as AVTransportVariable.TransportState
                SystemManager.instance.mediaView?.updatePanelState(transportState)

                when (transportState.value) {
                    TransportState.PLAYING ->
                        createRenderNotification(R.drawable.icon_pause, getString(R.string.pause))
                    TransportState.PAUSED_PLAYBACK ->
                        createRenderNotification(R.drawable.icon_play, getString(R.string.play))
                    else -> { }
                }
            }
        }
    }

    fun createRenderNotification(playPauseDrawable: Int, playPauseString: String) {
        val replayIntent = Intent(TTIDLNACaster.context, this::class.java)
        replayIntent.action = Constants.RENDER_NOTIFICATION_REPLAY
        val playPauseIntent = Intent(TTIDLNACaster.context, this::class.java)
        playPauseIntent.action = Constants.RENDER_NOTIFICATION_PLAY_PAUSE
        val stopIntent = Intent(TTIDLNACaster.context, this::class.java)
        stopIntent.action = Constants.RENDER_NOTIFICATION_STOP

        val replayAction = NotificationCompat.Action.Builder(
                R.drawable.icon_replay, getString(R.string.replay),
                PendingIntent.getService(TTIDLNACaster.context, 0, replayIntent, 0)).build()
        val playPauseAction = NotificationCompat.Action.Builder(
                playPauseDrawable, playPauseString,
                PendingIntent.getService(TTIDLNACaster.context, 0, playPauseIntent, 0)).build()
        val stopAction = NotificationCompat.Action.Builder(
                R.drawable.icon_stop, getString(R.string.stop),
                PendingIntent.getService(TTIDLNACaster.context, 0, stopIntent, 0)).build()
        val applicationIntent = PendingIntent.getActivity(
                this, 0, Intent(TTIDLNACaster.context, MainActivity::class.java), 0)
        val notification = NotificationCompat.Builder(TTIDLNACaster.context, Constants.RENDER_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.rendering))
                .setSmallIcon(R.drawable.icon_dlna)
                .setContentIntent(applicationIntent)
                .setOngoing(true)
                .addAction(replayAction)
                .addAction(playPauseAction)
                .addAction(stopAction)
                .build()
        notificationManager.notify(Constants.RENDER_NOTIFICATION_ID, notification)
    }

    fun setNotificationManager(): NotificationManager {
        return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}