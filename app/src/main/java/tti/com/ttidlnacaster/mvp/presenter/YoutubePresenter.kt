package tti.com.ttidlnacaster.mvp.presenter

import android.content.Intent
import android.util.Patterns
import com.commit451.youtubeextractor.YouTubeExtractor
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import tti.com.ttidlnacaster.R
import tti.com.ttidlnacaster.TTIDLNACaster
import tti.com.ttidlnacaster.dlna.SystemManager
import tti.com.ttidlnacaster.dlna.service.CastService
import tti.com.ttidlnacaster.dlna.service.MediaService
import tti.com.ttidlnacaster.mvp.contract.MediaContract
import tti.com.ttidlnacaster.util.Constants
import tti.com.ttidlnacaster.util.Utils

/**
 * Created by dylan_liang on 2018/2/9.
 */
class YoutubePresenter : MediaContract.Presenter {

    private var view: MediaContract.View? = null
    private var browserUrl: String = ""

    override fun bindView(view: MediaContract.View) {
        this.view = view
        SystemManager.instance.mediaView = view
    }

    override fun checkBrowserUrl(browserUrl: String) {
        this.browserUrl = browserUrl

        if (browserUrl.contains(Constants.YOUTUBE_WATCH)) {
            val idIndex = browserUrl.indexOf(Constants.YOUTUBE_WATCH) + Constants.YOUTUBE_WATCH.length
            val youtubeId = browserUrl.substring(idIndex, idIndex + 11)
            Thread(Runnable {
                YouTubeExtractor.create().extract(youtubeId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            extraction ->
                            if (!extraction.videoStreams.isEmpty()) {
                                val videoUrl = extraction.videoStreams.first().url
                                if (Patterns.WEB_URL.matcher(videoUrl).matches()) {
                                    view?.updateCanBeRender(true)
                                    return@subscribe
                                }
                            }
                            view?.updateCanBeRender(false)
                        }, {
                            view?.updateCanBeRender(false)
                        })
            }).start()
        }
        else
            view?.updateCanBeRender(false)
    }

    override fun prepareRender() {
        if (SystemManager.instance.selectedDevice != null) {
            if (browserUrl.contains(Constants.YOUTUBE_WATCH)) {
                SystemManager.instance.renderState = Constants.RenderState.PREPARING
                view?.updateUIState()
                val idIndex = browserUrl.indexOf(Constants.YOUTUBE_WATCH) + Constants.YOUTUBE_WATCH.length
                val youtubeId = browserUrl.substring(idIndex, idIndex + 11)
                Thread(Runnable {
                    YouTubeExtractor.create().extract(youtubeId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                extraction ->
                                if (!extraction.videoStreams.isEmpty()) {
                                    val videoTitle = extraction.title
                                    val videoUrl = extraction.videoStreams.first().url
                                    if (Patterns.WEB_URL.matcher(videoUrl).matches()) {
                                        view?.updateMediaTitle(videoTitle)
                                        startRender(videoTitle, videoUrl)
                                        return@subscribe
                                    }
                                }
                                SystemManager.instance.renderState = Constants.RenderState.ERROR
                                view?.updateUIState()
                            }, {
                                SystemManager.instance.renderState = Constants.RenderState.ERROR
                                view?.updateUIState()
                            })
                }).start()
            }
            else
                Utils.showToast(R.string.media_not_found)
        }
        else
            Utils.showToast(R.string.choose_device)
    }

    override fun startRender(mediaTitle: String, mediaUrl: String) {
        if (SystemManager.instance.castState == Constants.CastState.STARTED) {
            val castService = Intent(TTIDLNACaster.context, CastService::class.java)
            castService.action = Constants.CAST_ACTION_STOP
            TTIDLNACaster.context.startService(castService)
        }

        val mediaService = Intent(TTIDLNACaster.context, MediaService::class.java)
        mediaService.action = Constants.RENDER_ACTION_START
        mediaService.putExtra(Constants.RENDER_INTENT_TITLE, mediaTitle)
        mediaService.putExtra(Constants.RENDER_INTENT_DATA, mediaUrl)
        TTIDLNACaster.context.startService(mediaService)
    }

    override fun controlRender(controlCommand: Constants.ControlCommand) {
        val mediaService = Intent(TTIDLNACaster.context, MediaService::class.java)
        mediaService.action = Constants.RENDER_ACTION_CONTROL
        mediaService.putExtra(Constants.RENDER_INTENT_COMMAND, controlCommand)
        TTIDLNACaster.context.startService(mediaService)
    }

    override fun seekRender(relativeTime: String) {
        val mediaService = Intent(TTIDLNACaster.context, MediaService::class.java)
        mediaService.action = Constants.RENDER_ACTION_CONTROL
        mediaService.putExtra(Constants.RENDER_INTENT_COMMAND, Constants.ControlCommand.SEEK)
        mediaService.putExtra(Constants.RENDER_INTENT_RELATIVE_TIME, relativeTime)
        TTIDLNACaster.context.startService(mediaService)
    }

    override fun stopUpdateRenderPosition() {
        val mediaService = Intent(TTIDLNACaster.context, MediaService::class.java)
        mediaService.action = Constants.RENDER_ACTION_STOP_UPDATE
        TTIDLNACaster.context.startService(mediaService)
    }

    override fun disconnect() {
        val mediaService = Intent(TTIDLNACaster.context, MediaService::class.java)
        mediaService.action = Constants.RENDER_ACTION_DISCONNECT
        TTIDLNACaster.context.startService(mediaService)
    }
}