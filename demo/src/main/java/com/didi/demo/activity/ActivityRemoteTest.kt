package com.didi.demo.activity

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.didi.drouter.annotation.Router
import com.didi.drouter.api.DRouter
import com.didi.drouter.demo.R
import com.didi.drouter.mem.MemoryServer
import com.didi.drouter.module_base.remote.IRemoteFunction
import com.didi.drouter.module_base.remote.RemoteFeature
import com.didi.drouter.remote.Strategy
import com.didi.drouter.utils.RouterExecutor
import java.nio.ByteBuffer

@Router(uri = "/activity/remote_test_activity")
class ActivityRemoteTest : AppCompatActivity() {

    private var server1: MemoryServer? = null
    private var server2: MemoryServer? = null
    private var stop = false

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote_test)
//        startService(Intent(this@ActivityMemoryTest, RemoteService::class.java))
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.start_remote_page -> DRouter.build("/activity/remote").start(this)
            R.id.start_remote_test -> {
                val feature = RemoteFeature()
                feature.a = 1
                feature.b = "1"
                DRouter.build(IRemoteFunction::class.java)
                        .setFeature(feature)
                        .setAlias("remote")
                        .setRemote(Strategy("com.didi.drouter.remote.demo.remote"))
                        .getService()
                        .call()
            }
            R.id.start_remote_memory -> {
                launchMemory()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stop = true
        stopService(Intent(this, MyService::class.java))
    }

    private fun launchMemory() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (server1 == null) {
                startForegroundService(Intent(this, MyService::class.java))
                RouterExecutor.worker {
                    server1 = MemoryServer.create("host1", 10, 128)
                    while (!stop) {
                        // 耗时0.x毫秒
                        val byteBuffer = server1!!.acquireBuffer()
                        byteBuffer?.let {
                            increase(byteBuffer)
                            server1!!.notifyClient()
                        }
                        Thread.sleep(2)
                    }
                    server1?.close()
                }

                RouterExecutor.worker {
                    val retriever = MediaMetadataRetriever()
                    val fd = resources.openRawResourceFd(R.raw.big_buck_bunny)
                    retriever.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    val option = MediaMetadataRetriever.BitmapParams()
                    option.preferredConfig = Bitmap.Config.RGB_565

                    val config = Bundle()
                    var bitmap = retriever.getFrameAtIndex(0, option)!!
                    config.putInt("width", bitmap.width)
                    config.putInt("height", bitmap.height)

                    server2 = MemoryServer.create("host2", 5 * 1024 * 1024, 128, config)  // 5MB
                    var index = 0
                    // 一帧大概10ms
                    while (!stop) {
                        val time = System.currentTimeMillis()
                        try {
                            bitmap = retriever.getFrameAtIndex(index++, option)!!
                        } catch (e: Exception) {
                            index = 0
                            continue
                        }
                        val byteBuffer = server2!!.acquireBuffer()
                        if (byteBuffer != null) {
                            bitmap.copyPixelsToBuffer(byteBuffer)
                            server2!!.notifyClient()
                        }
                        val time2 = System.currentTimeMillis() - time
                        Thread.sleep(if (time2 >= 30) 0 else 30 - time2)
                    }
                    server2?.close()
                }
            }
            DRouter.build("/activity/remote_mem").start(this)
//            startService(Intent(this@ActivityMemoryTest, RemoteService::class.java))
        }
    }

    private fun increase(num: ByteBuffer): Boolean {
        val n = num.capacity()
        var carry = 1 //进位标志，每轮都加1
        for (i in n - 1 downTo 0) {
            if (carry == 0) break
            val next = (num[i] + carry)
            if (i == 0 && next > 9) {
                return false
            }
            if (next > 9) {
                num.put(i, 0)
                carry = 1 //进位
            } else {
                num.put(i, next.toByte())
                carry = 0
            }
        }
        return true
    }

    // 提高后台进程中线程的优先级
    class MyService: Service() {
        override fun onBind(intent: Intent?): IBinder? {
            TODO("Not yet implemented")
        }

        override fun onCreate() {
            super.onCreate()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel("id", "a", NotificationManager.IMPORTANCE_HIGH);
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(notificationChannel);
                val notification: Notification = Notification.Builder(this, notificationChannel.id).build()
                startForeground(1, notification)
            }
        }

    }
}