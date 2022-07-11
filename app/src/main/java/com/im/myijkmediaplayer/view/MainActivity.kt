package com.im.myijkmediaplayer.view

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.material.button.MaterialButton
import com.im.myijkmediaplayer.R
import com.im.myijkmediaplayer.viewmodel.PlayerViewModel
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val REQUEST_CODE = 123

    @BindView(R.id.progressBar)
    lateinit var progressBar: ProgressBar

    @BindView(R.id.playerFrame)
    lateinit var playerFrame: FrameLayout

    @BindView(R.id.surfaceView)
    lateinit var surfaceView: SurfaceView

    @BindView(R.id.seekbar)
    lateinit var seekBar: SeekBar

    @BindView(R.id.tv_start)
    lateinit var tv_start: TextView

    @BindView(R.id.tv_end)
    lateinit var tv_end: TextView

    @BindView(R.id.play)
    lateinit var play: MaterialButton

    @BindView(R.id.pause)
    lateinit var pause: MaterialButton

    @BindView(R.id.stop)
    lateinit var stop: MaterialButton

    private var timer: Timer? = null
    private var isSeekbarChaning = false
    private lateinit var playerViewModel: PlayerViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        checkMyPermission()
    }

    private fun resizePlayer(width: Int, height: Int){
        if (width == 0 || height == 0) return
        surfaceView.layoutParams = FrameLayout.LayoutParams(
            playerFrame.height * width / height,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        //屏幕 横版的时候隐藏 转态栏
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            hideSystemUI()
        }
    }

    private fun hideSystemUI(){
        //Enables regulapr immersive mode.
        //For "lean back" mode, remove SYSTEM_ _UI_ FLAG_ IMMERSIVE.
        //or for "sticky immersive, replace it with SYSTEM_ UI_ FLAG_ IMMERSIVE_ STICKY
        window.decorView.systemUiVisibility =(View.SYSTEM_UI_FLAG_IMMERSIVE
                //Set the content to appear under the system bars S0 that the
                //Content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_LAYOUT_FLAGS
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                //Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    private fun checkMyPermission(){
        val allPermissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this@MainActivity, allPermissions, REQUEST_CODE)
        } else {
            initView()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    initView()
                } else {
                    //未授权
                    Toast.makeText(this, "授权被拒绝！", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initView(){
        initViewModel()
        initVideo()
        initListener()
    }

    private fun initViewModel(){
        playerViewModel = ViewModelProvider(this).get(PlayerViewModel::class.java)
    }

    private fun initVideo(){
        playerViewModel.progressBarVisibility.observe(this@MainActivity, Observer {
            progressBar.visibility = it
        })
        //resize
        playerViewModel.videoResolution.observe(this, Observer {
            playerFrame.post {  //post 是将里面的函数 放在最后执行（等View 都加载完了）
                resizePlayer(it.first,it.second)
            }
        })
        lifecycle.addObserver(playerViewModel.mediaPlayer) //感应生命周期
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(p0: SurfaceHolder, format: Int, width: Int, height: Int) {
                playerViewModel.mediaPlayer.setDisplay(p0)
                playerViewModel.mediaPlayer.setScreenOnWhilePlaying(true) //播放时屏幕点亮
            }
            override fun surfaceDestroyed(p0: SurfaceHolder) {}
            override fun surfaceCreated(p0: SurfaceHolder) {}
        })

        playerViewModel.myDuration.observe(this, Observer {
            seekBar.max = it.MAX_VALUE
            Log.d("lvzw", "myDuration: ${seekBar.max}")
        })

        playerViewModel.myCurrentPosition.observe(this, Observer {
            seekBar.progress = it.MAX_VALUE
            Log.d("lvzw", "myCurrentPosition: ${seekBar.max}")
        })
    }

    private fun initListener(){
        play.setOnClickListener(this)
        stop.setOnClickListener(this)
        pause.setOnClickListener(this)
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val duration2: Int = playerViewModel.mediaPlayer.duration / 1000 //获取音乐总时长
                val position: Int = playerViewModel.mediaPlayer.currentPosition //获取当前播放的位置
                tv_start.text = calculateTime(position / 1000) //开始时间
                tv_end.text = calculateTime(duration2) //总时长
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSeekbarChaning = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isSeekbarChaning = false
                playerViewModel.mediaPlayer.seekTo(seekBar.progress) //在当前位置播放
                tv_start.text = calculateTime(playerViewModel.mediaPlayer.currentPosition / 1000)
            }
        })
    }

    fun calculateTime(time: Int): String? {
        val minute: Int
        val second: Int
        if (time > 60) {
            minute = time / 60
            second = time % 60
            //分钟再0~9
            return if (minute in 0..9) {
                //判断秒
                if (second in 0..9) {
                    "0$minute:0$second"
                } else {
                    "0$minute:$second"
                }
            } else {
                //分钟大于10再判断秒
                if (second in 0..9) {
                    "$minute:0$second"
                } else {
                    "$minute:$second"
                }
            }
        } else if (time < 60) {
            second = time
            return if (second in 0..9) {
                "00:0$second"
            } else {
                "00:$second"
            }
        }
        return null
    }

    override fun onClick(p0: View?) {
        when (p0!!.id) {
            R.id.play -> if (!playerViewModel.mediaPlayer.isPlaying) {
                playerFrame.visibility = View.VISIBLE
                playerViewModel.mediaPlayer.start() //开始播放
                playerViewModel.setLoop(true) //自动播放下一集
                val duration: Int = playerViewModel.mediaPlayer.duration //获取音乐总时间
                seekBar.max = duration //将音乐总时间设置为Seekbar的最大值
                timer = Timer()
                timer!!.schedule(object : TimerTask() {
                    override fun run() {
                        if (!isSeekbarChaning) {
                            seekBar.progress = playerViewModel.mediaPlayer.currentPosition
                        }
                    }
                }, 0, 50)
            }
            R.id.pause -> if (playerViewModel.mediaPlayer.isPlaying) {
                playerViewModel.mediaPlayer.pause() //暂停播放
            }
            R.id.stop -> {
                playerViewModel.mediaPlayer.reset() //中止播放
                //initMediaPlayer()
                playerViewModel.reloadVideo()
                playerFrame.visibility = View.INVISIBLE
            }
            else -> {}
        }
    }
}