package tti.com.ttidlnacaster.dlna.service

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.support.v4.app.NotificationCompat
import android.view.Surface
import org.fourthline.cling.support.contentdirectory.DIDLParser
import org.fourthline.cling.support.model.DIDLContent
import org.fourthline.cling.support.model.Res
import org.fourthline.cling.support.model.item.Movie
import tti.com.ttidlnacaster.R
import tti.com.ttidlnacaster.TTIDLNACaster
import tti.com.ttidlnacaster.dlna.SystemManager
import tti.com.ttidlnacaster.flv.FlvPacker
import tti.com.ttidlnacaster.flv.Packer
import tti.com.ttidlnacaster.ui.activity.MainActivity
import tti.com.ttidlnacaster.util.Constants
import tti.com.ttidlnacaster.util.PlayBackCommand
import tti.com.ttidlnacaster.util.Utils
import java.io.File
import java.io.RandomAccessFile

/**
 * Created by dylan_liang on 2018/2/13.
 */
class CastService: Service() {

    private var mediaCodec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null
    private val notificationManager by lazy { setNotificationManager() }

    private var castFolder: File? = null
    private var castFile: File? = null
    private var randomAccessFile: RandomAccessFile? = null

    private var flvPacker: FlvPacker? = null
    private var writeHandler: WriteHandler? = null
    private var connectHandler: ConnectHandler? = null
    private var commandHandler: CommandHandler? = null
    private var disconnectTimer: DisconnectTimer? = null

    private var tempData: ByteArray = ByteArray(0)
    private var limitLength: Long = 0
    private var dataLength: Long = 0
    private var sentOffset: Long = 0
    private var isStarted = false
    private var beDisconnect = false

    companion object {
        private val SCREEN_WIDTH by lazy { 1280 }
        private val SCREEN_HEIGHT by lazy { 720 }
        private val FRAME_RATE by lazy { 25 }
        private val BIT_RATE by lazy { 1024 * 1024 * 2 }
        private val KEY_I_FRAME by lazy { 1 }

        private val FLAG_STOP_CAST by lazy { 101 }
        private val FLAG_STOP_CONNECT by lazy { 102 }
        private val FLAG_CONNECT_FAILED by lazy { 103 }
        private val FLAG_CONNECT_TIMEOUT by lazy { 104 }

        private val FLAG_NEW_DATA by lazy { 201 }
        private val FLAG_TEMP_DATA by lazy { 202 }

        private val CONNECT_DELAY: Long = 2000
        private val CONNECT_TIMEOUT: Long = 20000
        private val TEMP_DATA_DELAY: Long = 3000
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        castFolder?.delete()
        castFile?.delete()
        notificationManager.cancel(Constants.CAST_NOTFICATION_ID)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.CAST_ACTION_START -> startCast(intent)
            Constants.CAST_ACTION_STOP -> stopCast(FLAG_STOP_CAST)
            Constants.CAST_ACTION_DISCONNECT -> stopCast(FLAG_STOP_CONNECT)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return CastBinder()
    }

    inner class CastBinder: Binder() {
        val castService: CastService
            get() = this@CastService
    }

    fun createMetaData(): String {
        val content = DIDLContent()
        content.addItem(
                Movie("", "", "Stream", "",
                        Res(MediaFormat.MIMETYPE_VIDEO_AVC, 0, "00:00:00", 0, Constants.CAST_STREAM_URI)))
        return DIDLParser().generate(content)
    }

    fun startCast(intent: Intent) {
        if (SystemManager.instance.castState == Constants.CastState.PREPARING) {
            limitLength = StatFs(Environment.getDataDirectory().path).totalBytes / 10

            val mediaFormat = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_AVC, SCREEN_WIDTH, SCREEN_HEIGHT)
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, KEY_I_FRAME)

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec?.setCallback(EncodeCallback())
            mediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = mediaCodec?.createInputSurface()
            mediaCodec?.start()

            flvPacker = FlvPacker()
            flvPacker?.initVideoParams(SCREEN_WIDTH, SCREEN_HEIGHT, FRAME_RATE)
            flvPacker?.setPacketListener(PacketListener())
            flvPacker?.start()

            writeHandler = WriteHandler()
            connectHandler = ConnectHandler()
            commandHandler = CommandHandler()

            castFolder = File(Constants.CAST_FOLDER_PATH)
            castFolder?.mkdirs()
            castFile = File(Constants.CAST_FILE_PATH)
            randomAccessFile = RandomAccessFile(castFile, "rw")

            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val result = intent.getIntExtra(Constants.CAST_INTENT_RESULT, Activity.RESULT_OK)
            val data: Intent = intent.getParcelableExtra(Constants.CAST_INTENT_DATA)
            mediaProjection =  mediaProjectionManager.getMediaProjection(result, data)

            virtualDisplay = mediaProjection?.createVirtualDisplay(getString(R.string.app_name),
                    SCREEN_WIDTH, SCREEN_HEIGHT, resources.displayMetrics.densityDpi, 0, inputSurface,
                    null, null)
        }
    }

    fun stopCast(flag: Int) {
        disconnectTimer?.cancel()

//        mediaCodec?.reset()
        mediaCodec?.release()
        inputSurface?.release()
        flvPacker?.stop()
        virtualDisplay?.release()
        mediaProjection?.stop()

        writeHandler?.removeCallbacksAndMessages(null)
        connectHandler?.removeCallbacksAndMessages(null)
        commandHandler?.removeCallbacksAndMessages(null)
        randomAccessFile?.close()
        castFolder?.delete()
        castFile?.delete()

        SystemManager.instance.castState = Constants.CastState.STOPPED
        SystemManager.instance.castView?.updateUIState()

        limitLength = 0
        dataLength = 0
        sentOffset = 0
        isStarted = false

        when (flag) {
            FLAG_STOP_CAST -> {
                notificationManager.cancel(Constants.CAST_NOTFICATION_ID)
                PlayBackCommand.stop(null)
                Utils.showToast(R.string.cast_stopped)
            }
            FLAG_STOP_CONNECT -> beDisconnect = true
            FLAG_CONNECT_FAILED -> Utils.showToast(R.string.connect_failed)
            FLAG_CONNECT_TIMEOUT -> Utils.showToast(R.string.connection_timed_out)
        }
    }

    inner class EncodeCallback: MediaCodec.Callback() {
        override fun onOutputBufferAvailable(mediaCodec: MediaCodec, i: Int, bufferInfo: MediaCodec.BufferInfo) {
            try {
                val outputBuffer = mediaCodec.getOutputBuffer(i)
                if (bufferInfo.size > 0)
                    flvPacker?.onVideoData(outputBuffer, bufferInfo)
                mediaCodec.releaseOutputBuffer(i, false)
            } catch (exception: Exception) {
            }
        }

        override fun onInputBufferAvailable(mediaCodec: MediaCodec, i: Int) {
        }

        override fun onOutputFormatChanged(mediaCodec: MediaCodec, mediaFormat: MediaFormat) {
        }

        override fun onError(mediaCodec: MediaCodec, codecException: MediaCodec.CodecException) {
        }
    }

    inner class PacketListener: Packer.OnPacketListener  {
        override fun onPacket(data: ByteArray, packetType: Int) {
            val message = Message.obtain(writeHandler, FLAG_NEW_DATA, data)
            message.sendToTarget()
        }
    }

    fun writeData() {
        writeHandler?.removeMessages(FLAG_TEMP_DATA)

        randomAccessFile?.seek(dataLength)
        randomAccessFile?.write(tempData)
        dataLength += tempData.size

        if (sentOffset < SystemManager.instance.sentLength) {
            sentOffset = SystemManager.instance.sentLength
            if (sentOffset in BIT_RATE..(dataLength - 1)) {
                val buffer = ByteArray((dataLength - sentOffset).toInt())
                randomAccessFile?.seek(sentOffset)
                randomAccessFile?.read(buffer)
                randomAccessFile?.setLength(0)
                randomAccessFile?.seek(sentOffset)
                randomAccessFile?.write(buffer)
            }
        }

        if (!isStarted) {
            isStarted = true
            connectHandler?.sendEmptyMessageDelayed(0, CONNECT_DELAY)
        }

        writeHandler?.sendEmptyMessageDelayed(FLAG_TEMP_DATA, TEMP_DATA_DELAY)
    }

    inner class WriteHandler: Handler() {
        override fun handleMessage(msg: Message) {
            if (StatFs(Environment.getDataDirectory().path).availableBytes > limitLength) {
                when (msg.what) {
                    FLAG_NEW_DATA -> {
                        tempData = msg.obj as ByteArray
                        writeData()
                    }
                    FLAG_TEMP_DATA ->
                        writeData()
                }
            }
            else
                stopCast(FLAG_STOP_CAST)
        }
    }

    inner class ConnectHandler: Handler() {
        override fun handleMessage(msg: Message) {
            beDisconnect = false
            disconnectTimer = DisconnectTimer()
            disconnectTimer?.start()

            PlayBackCommand.playNewMedia(commandHandler, Constants.CAST_STREAM_URI, createMetaData())
        }
    }

    inner class CommandHandler: Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                PlayBackCommand.PLAY_NEW_MEDIA_SUCCEEDED -> {
                    if (!beDisconnect) {
                        disconnectTimer?.cancel()
                        SystemManager.instance.castState = Constants.CastState.STARTED
                        SystemManager.instance.castView?.updateUIState()
                        createCastNotification()
                    }
                }
                PlayBackCommand.PLAY_NEW_MEDIA_FAILED -> {
                    stopCast(FLAG_CONNECT_FAILED)
                }
            }
        }
    }

    inner class DisconnectTimer: CountDownTimer(CONNECT_TIMEOUT, CONNECT_TIMEOUT) {
        override fun onFinish() {
            stopCast(FLAG_CONNECT_TIMEOUT)
        }

        override fun onTick(millisUntilFinished: Long) {
        }
    }

    fun createCastNotification() {
        val stopIntent = Intent(TTIDLNACaster.context, this::class.java)
        stopIntent.action = Constants.CAST_ACTION_STOP
        val stopAction = NotificationCompat.Action.Builder(
                R.drawable.icon_cast, getString(R.string.stop),
                PendingIntent.getService(TTIDLNACaster.context, 0, stopIntent, 0)).build()
        val applicationIntent = PendingIntent.getActivity(
                this, 0, Intent(TTIDLNACaster.context, MainActivity::class.java), 0)
        val notification = NotificationCompat.Builder(TTIDLNACaster.context, Constants.CAST_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.casting))
                .setSmallIcon(R.drawable.icon_cast)
                .setContentIntent(applicationIntent)
                .setOngoing(true)
                .addAction(stopAction)
                .build()
        notificationManager.notify(Constants.CAST_NOTFICATION_ID, notification)
    }

    fun setNotificationManager(): NotificationManager {
        return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}