package com.im.myijkmediaplayer.utils

import android.media.MediaPlayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**
 *  Author : lchiway
 *  Date   : 2022/6/2
 *  Desc   : 将控件 与 生命周期绑定
 */
class MyMediaPlayerLifecycle: MediaPlayer(), LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun pausePlayer(){
        pause()
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resumePlayer(){
        start()
    }
}