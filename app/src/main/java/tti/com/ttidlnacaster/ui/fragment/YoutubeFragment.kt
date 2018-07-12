package tti.com.ttidlnacaster.ui.fragment

import android.app.ProgressDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_youtube.*
import tti.com.ttidlnacaster.R
import tti.com.ttidlnacaster.base.BaseFragment
import android.webkit.WebViewClient
import tti.com.ttidlnacaster.util.Constants
import android.webkit.WebView
import android.webkit.JavascriptInterface
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_main.*
import net.skoumal.fragmentback.BackFragment
import org.fourthline.cling.model.ModelUtil
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable
import org.fourthline.cling.support.model.PositionInfo
import org.fourthline.cling.support.model.TransportState
import tti.com.ttidlnacaster.dlna.SystemManager
import tti.com.ttidlnacaster.mvp.contract.MediaContract
import tti.com.ttidlnacaster.mvp.presenter.YoutubePresenter
import tti.com.ttidlnacaster.util.Utils

/**
 * Created by dylan_liang on 2018/2/8.
 */
class YoutubeFragment: BaseFragment(), MediaContract.View, BackFragment {

    private val presenter by lazy { YoutubePresenter() }
    private val progressDialog by lazy { setProgressDialog() }

    private var trackDurationSeconds = 0

    companion object {
        fun newInstance(): YoutubeFragment {
            return YoutubeFragment()
        }
    }

    init {
        presenter.bindView(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            = inflater.inflate(R.layout.fragment_youtube, container, false)

    override fun onBackPressed(): Boolean {
        return when {
            activity.viewPager.currentItem != 1 -> false
            SystemManager.instance.renderState == Constants.RenderState.STARTED -> {
                presenter.controlRender(Constants.ControlCommand.STOP)
                true
            }
            youtubeWebView.canGoBack() -> {
                youtubeWebView.goBack()
                youtubeWebView.loadUrl(Constants.JAVASCRIPT_INTERFACE)
                true
            }
            else -> false
        }
    }

    override fun getBackPriority(): Int {
        return BackFragment.NORMAL_BACK_PRIORITY
    }

    override fun setUp() {
        youtubeWebView.settings.javaScriptEnabled = true
        youtubeWebView.addJavascriptInterface(UrlChangedInterface(), "android")
        youtubeWebView.webViewClient = (object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                view.loadUrl(Constants.JAVASCRIPT_INTERFACE)
            }
        })
        youtubeWebView.loadUrl(Constants.YOUTUBE_URL)

        startRenderButton.setOnClickListener({
            presenter.prepareRender()
        })
        playPauseButton.setOnClickListener({
            presenter.controlRender(Constants.ControlCommand.PLAY_PAUSE)
        })
        replayButton.setOnClickListener({
            presenter.controlRender(Constants.ControlCommand.REPLAY)
        })
        stopButton.setOnClickListener({
            presenter.controlRender(Constants.ControlCommand.STOP)
        })
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val seekSeconds = trackDurationSeconds * (seekBar.progress.toFloat() / seekBar.max)
                    val timeString = ModelUtil.toTimeString(seekSeconds.toLong())
                    activity.runOnUiThread(Runnable {
                        positionText.text = timeString
                    })
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                presenter.stopUpdateRenderPosition()
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val seekSeconds = trackDurationSeconds * (seekBar.progress.toFloat() / seekBar.max)
                presenter.seekRender(ModelUtil.toTimeString(seekSeconds.toLong()))
            }
        })
    }

    override fun updateUIState() {
        activity.runOnUiThread(Runnable {
            when (SystemManager.instance.renderState) {
                Constants.RenderState.PREPARING -> {
                    progressDialog.show()
                    youtubeWebView.onPause()
                    titleText.text = ""
                    seekBar.progress = 0
                    seekBar.max = 0
                    durationText.text = "00:00:00"
                    positionText.text = "00:00:00"
                }
                Constants.RenderState.ERROR -> {
                    SystemManager.instance.renderState = Constants.RenderState.STOPPED
                    youtubeWebView.onResume()
                    progressDialog.dismiss()
                    Utils.showToast(R.string.media_not_found)
                }
                Constants.RenderState.STARTED -> {
                    panelLayout.visibility = View.VISIBLE
                    renderingLayout.visibility = View.VISIBLE
                    youtubeWebView.visibility = View.GONE
                    startRenderButton.visibility = View.GONE
                    playPauseButton.setBackgroundResource(R.drawable.icon_pause)
                    progressDialog.dismiss()
                    Utils.showToast(R.string.render_started)
                }
                Constants.RenderState.STOPPED -> {
                    panelLayout.visibility = View.GONE
                    renderingLayout.visibility = View.GONE
                    youtubeWebView.onResume()
                    youtubeWebView.visibility = View.VISIBLE
                    startRenderButton.visibility = View.VISIBLE
                    progressDialog.dismiss()
                }
            }
        })
    }

    override fun updateCanBeRender(canBeRender: Boolean) {
        activity.runOnUiThread(Runnable {
            if (canBeRender)
                startRenderButton.backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.colorAccent))
            else
                startRenderButton.backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.gray))
        })
    }

    override fun updateMediaTitle(mediaTitle: String) {
        activity.runOnUiThread(Runnable {
            titleText.text = mediaTitle
        })
    }

    override fun updatePanelState(transportState: AVTransportVariable.TransportState) {
        activity.runOnUiThread(Runnable {
            when (transportState.value) {
                TransportState.PLAYING ->
                    playPauseButton.setBackgroundResource(R.drawable.icon_pause)
                TransportState.PAUSED_PLAYBACK ->
                    playPauseButton.setBackgroundResource(R.drawable.icon_play)
                TransportState.STOPPED ->
                    presenter.controlRender(Constants.ControlCommand.STOP)
                else -> { }
            }
        })
    }

    override fun updatePositionInfo(positionInfo: PositionInfo) {
        trackDurationSeconds = positionInfo.trackDurationSeconds.toInt()
        seekBar.max = trackDurationSeconds
        seekBar.progress = positionInfo.trackElapsedSeconds.toInt()

        activity.runOnUiThread(Runnable {
            durationText.text = positionInfo.trackDuration
            positionText.text = positionInfo.relTime
        })
    }

    inner class UrlChangedInterface {
        @JavascriptInterface
        fun onUrlChange(url: String) {
            presenter.checkBrowserUrl(url)
        }
    }

    fun setProgressDialog(): ProgressDialog {
        val progressDialog = ProgressDialog(context, ProgressDialog.STYLE_SPINNER)
        progressDialog.setMessage(getString(R.string.connecting))
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setOnCancelListener({
            youtubeWebView.onResume()
            presenter.disconnect()
        })
        return progressDialog
    }
}