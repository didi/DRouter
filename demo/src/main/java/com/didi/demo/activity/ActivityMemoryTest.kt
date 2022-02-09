package com.didi.demo.activity

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
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

@Router(uri = "/activity/memory_test")
class ActivityMemoryTest : AppCompatActivity() {

    private var server: MemoryServer? = null
    private var stop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_test)
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

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onDestroy() {
        super.onDestroy()
        stop = true
    }

    private fun launchMemory() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (server == null) {
                RouterExecutor.worker {
                    val retriever = MediaMetadataRetriever()
                    val fd = resources.openRawResourceFd(R.raw.big_buck_bunny)
                    retriever.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    val option = MediaMetadataRetriever.BitmapParams()
                    option.preferredConfig = Bitmap.Config.RGB_565
                    var bitmap: Bitmap
                    server = MemoryServer.create("host", 5 * 1024 * 1024, 32)  // 5MB
                    val config = Bundle()
                    var index = 0
                    while (!stop) {
                        var time = System.currentTimeMillis()
                        try {
                            bitmap = retriever.getFrameAtIndex(index++, option)!!
                        } catch (e: Exception) {
                            index = 0
                            continue
                        }
                        val byteBuffer = server!!.acquireBuffer()
                        byteBuffer?.let {
                            bitmap.copyPixelsToBuffer(it)
                            config.putInt("width", bitmap.width)
                            config.putInt("height", bitmap.height)
                            time = System.currentTimeMillis()
                            server!!.notifyClient(config)
                        }
                        time = System.currentTimeMillis() - time
                        Thread.sleep(if (time >= 16) 0 else 16 - time)
                    }
                    server?.close()
                }
            }
            DRouter.build("/activity/remote_mem").start(this)
//            startService(Intent(this@ActivityMemoryTest, RemoteService::class.java))

        }
    }
}