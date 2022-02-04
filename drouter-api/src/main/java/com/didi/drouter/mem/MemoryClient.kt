package com.didi.drouter.mem

import android.os.*
import android.system.ErrnoException
import android.system.OsConstants
import androidx.annotation.RequiresApi
import com.didi.drouter.api.DRouter
import com.didi.drouter.api.Extend
import com.didi.drouter.remote.IRemoteCallback
import com.didi.drouter.remote.Strategy
import com.didi.drouter.utils.RouterExecutor
import com.didi.drouter.utils.RouterLogger
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by gaowei on 2022/1/28
 */
@RequiresApi(api = Build.VERSION_CODES.O_MR1)
class MemoryClient(private val authority: String, private val memoryName: String) {

    private companion object {
        // avoid jvm recycle
        val clients = CopyOnWriteArrayList<MemoryClient>()
    }
    private var oriBuffer: ByteBuffer? = null
    private var dataBuffer: ByteBuffer? = null
    private var serial = -1
    private var clientCallback: MemCallback? = null
    // avoid jvm recycle
    private var remoteCallback: IRemoteCallback.Type2<Boolean, Bundle?>? = null
    private val workCount = AtomicInteger(0)
    private var selfClose = false
    private var handlerThread = HandlerThread("d_router_shared_memory_client")
    private var handler: Handler

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        clients.add(this)
    }

    @Synchronized @JvmOverloads
    fun registerObserver(callback: MemCallback, copyMemory: Boolean = false, filterNotify: Int = 0) {
        if (this.clientCallback == null) {
            RouterExecutor.worker {
                remoteCallback = object : IRemoteCallback.Type2<Boolean, Bundle?>() {
                    override fun callback(isServerClose: Boolean, config: Bundle?) {
                        synchronized(this@MemoryClient) {
                            if (selfClose) {
                                clear()
                                return
                            }
                            if (isServerClose) {
                                clientCallback?.onServerClosed()
                                clear()
                                close()
                                return
                            }
                            if (serial != -1 && dataBuffer != null && oriBuffer != null) {
                                workCount.incrementAndGet()
                                var realBuffer = dataBuffer!!
                                if (copyMemory) {
                                    realBuffer = dataBuffer!!.duplicate()
                                    clear()
                                }
                                if (workCount.get() > 10) {
                                    RouterLogger.getCoreLogger().w(
                                            "[Client][SharedMemory] client waiting works: ${workCount.get()}")
                                }
                                handler.post {
                                    realBuffer.clear()
                                    clientCallback?.onNotify(realBuffer, config)
                                    if (!copyMemory) {
                                        clear()
                                    }
                                    workCount.decrementAndGet()
                                }
                            } else {
                                close()
                            }
                        }
                    }
                    override fun mode(): Int {
                        return Extend.Thread.POSTING
                    }
                }
                synchronized(this) {
                    val serverMemory = DRouter.build(ISharedMemory::class.java)
                            .setRemote(Strategy(authority))
                            .getService()
                            .acquireMemory(memoryName, filterNotify, remoteCallback!!)
                    try {
                        serverMemory?.let {
                            serial = it.serial
                            dataBuffer = it.memory.map(OsConstants.PROT_READ,
                                    0, it.memory.size - serverMemory.maxClient)
                            oriBuffer = it.memory.map(OsConstants.PROT_READ or OsConstants.PROT_WRITE,
                                    0, it.memory.size)
                        }
                    } catch (e: ErrnoException) {
                        RouterLogger.getCoreLogger().e(
                                "[Client][SharedMemory] client $serial map buffer error: %s", e)
                    }
                }
            }
        }
        this.clientCallback = callback
    }

    @Synchronized
    fun close() {
        if (!selfClose) {
            selfClose = true
            handler.removeCallbacksAndMessages(null)
            // wait if there is a task is still running.
            handler.post {
                RouterLogger.getCoreLogger().w("[Client][SharedMemory] client $serial real close")
                DRouter.build(ISharedMemory::class.java)
                        .setRemote(Strategy(authority).setCallAsync(true))
                        .getService()
                        .release(memoryName, serial)
                dataBuffer?.let { SharedMemory.unmap(it) }
                oriBuffer?.let { SharedMemory.unmap(it) }
                dataBuffer = null
                oriBuffer = null
                remoteCallback = null
                serial = -1
                clients.remove(this)
                handlerThread.quitSafely()
            }
        }
    }

    @Synchronized
    private fun clear() {
        oriBuffer?.put(dataBuffer!!.capacity() + serial, 0)
    }

    abstract class MemCallback {
        abstract fun onNotify(buffer: ByteBuffer, config: Bundle?)
        open fun onServerClosed() {}
    }
}