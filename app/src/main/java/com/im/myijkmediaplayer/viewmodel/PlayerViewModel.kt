package com.im.myijkmediaplayer.viewmodel

import android.media.MediaPlayer
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.im.myijkmediaplayer.utils.MyMediaPlayerLifecycle
import java.io.File

/**
 *  Author : lchiway
 *  Date   : 2022/6/2
 *  Desc   :
 */
class PlayerViewModel: ViewModel() {

    val mediaPlayer = MyMediaPlayerLifecycle() //使用绑定 生命周期的 自定义 MyMediaPlayerLifecycle
    private val _progressBarVisibility = MutableLiveData(View.VISIBLE)
    private val _duration = MutableLiveData(Int)
    private val _currentPosition = MutableLiveData(Int)
    val progressBarVisibility : LiveData<Int> = _progressBarVisibility //对加载 progressBar数据进行，保存
    val myDuration : LiveData<Int.Companion> = _duration
    val myCurrentPosition : LiveData<Int.Companion> = _currentPosition

    private val _videoResolution = MutableLiveData(Pair(0,0)) //对加载 视频宽高 数据进行，保存
    val videoResolution :LiveData<Pair<Int,Int>> = _videoResolution
    private var startIdx = 0
    private var asynCount = 0
    private var isLoop = false
    init{
        loadVideo()
    }

    fun reloadVideo(){
        loadVideo()
    }

    fun setLoop(switch: Boolean) {
        isLoop = switch
    }

    private fun loadVideo(){
        mediaPlayer.apply{
            _progressBarVisibility.value = View.VISIBLE
            setSourcePath(startIdx)
            setOnPreparedListener{ // 准备监听
                _progressBarVisibility.value = View.INVISIBLE
                _duration.value.apply { duration }
                _currentPosition.value.apply { currentPosition }
                //isLooping = true //从头开始播放
                if(isLoop)
                    it.start()//播放
            }
            setOnVideoSizeChangedListener { _, width, height -> //尺寸监听
                _videoResolution.value = Pair(width,height)
            }
            setOnCompletionListener {

                Log.d(javaClass.name, "setOnCompletionListener: $currentPosition : $duration")
                if (duration != 0) {
                    when (currentPosition / 1000) {
                        duration / 1000 -> {
                            reset()
                            setSourcePath(startIdx)
                            prepareAsync()
                            //start()
                        }
                    }
                }
            }
            prepareAsync()
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.release()
    }

    private fun setSourcePath(idx: Int){
        var file: File?
        for(i in (idx+1)..104) {
            file = File("/sdcard/LocalVideo/超级宝贝JOJO第${i}话.mp4")
            if (file.exists()) {
                startIdx = i
                break
            }
        }
        Log.d(javaClass.name, "setSourcePath: $startIdx")
        if(startIdx == 104)
            return
        mediaPlayer.setDataSource("/sdcard/LocalVideo/超级宝贝JOJO第${startIdx}话.mp4")
    }
}