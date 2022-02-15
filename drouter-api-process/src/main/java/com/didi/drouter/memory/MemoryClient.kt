package com.didi.drouter.memory

import android.net.LocalServerSocket
import android.os.*
import android.system.ErrnoException
import android.system.OsConstants
import androidx.annotation.RequiresApi
import com.didi.drouter.api.DRouter
import com.didi.drouter.api.Strategy
import com.didi.drouter.utils.RouterExecutor
import com.didi.drouter.utils.RouterLogger
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by gaowei on 2022/1/28
 *
 * @param serverAcquireDelay default 0; server will delay for how many frames for client.
 * @param serverNotifyFilter default 0; how many frames will be skipped to notify. when set, async will be true auto.
 * @param clientAsync use message queue to handle, it will have to copy memory to clear tag.
 */
@RequiresApi(api = Build.VERSION_CODES.O_MR1)
class MemoryClient @JvmOverloads constructor(
    private val authority: String, private val memoryName: String,
    private val serverAcquireDelay:Int = 0, private val serverNotifyFilter: Int = 0,
    private var clientAsync: Boolean = false) {

    companion object {
        const val MAX_MEMORY_COPY = 50 * 1024 * 1024
        const val MAX_QUEUE_SIZE = 1000
    }

    private lateinit var socketServer: LocalServerSocket
    private lateinit var oriBuffer: ByteBuffer
    private lateinit var dataBuffer: ByteBuffer
    private var serial = -1
    private var clientCallback: MemCallback? = null
    private var selfClose = false
    private var workCount: AtomicInteger? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var clientName = "${memoryName}_no_serial"

    init {
        if (serverNotifyFilter > 0) {
            clientAsync = true
        }
    }

    @Synchronized
    fun registerObserver(callback: MemCallback) {
        if (this.clientCallback == null) {
            RouterExecutor.worker {
                val serverMemory = DRouter.build(ISharedMemory::class.java)
                    .setRemote(Strategy(authority))
                    .getService()
                    .acquireMemory(memoryName, serverAcquireDelay, serverNotifyFilter)
                if (serverMemory != null) {
                    try {
                        dataBuffer = serverMemory.memory.map(
                            OsConstants.PROT_READ,
                            0, serverMemory.memory.size - serverMemory.maxClient)
                        oriBuffer = serverMemory.memory.map(
                            OsConstants.PROT_READ or OsConstants.PROT_WRITE,
                            0, serverMemory.memory.size)
                    } catch (e: ErrnoException) {
                        RouterLogger.getCoreLogger().e(
                            "[Client][SharedMemory] $clientName map buffer error: %s", e)
                        close()
                        return@worker
                    }
                    serial = serverMemory.serial
                    clientName = "${memoryName}_$serial"
                    callback.onConnected(serverMemory.info)

                    if (clientAsync) {
                        workCount = AtomicInteger(0)
                        handlerThread = HandlerThread("router_shared_memory_$clientName")
                        handlerThread?.let {
                            it.start()
                            handler = Handler(it.looper)
                        }
                    }
                    socketServer = LocalServerSocket(clientName)
                    DRouter.build(ISharedMemory::class.java)
                        .setRemote(Strategy(authority).setCallAsync(true))
                        .getService()
                        .openSocket(memoryName, serial, clientName)
                    val socket = socketServer.accept()
                    while (true) {
                        val serverClose = socket.inputStream.read() == 1
                        handle(serverClose)
                        if (selfClose || serverClose) {
                            break
                        }
                    }
                } else {
                    RouterLogger.getCoreLogger().e("[Client][SharedMemory] There is no server for name $memoryName")
                    close()
                }
            }
        }
        this.clientCallback = callback
    }

    @Synchronized
    fun handle(isServerClose: Boolean) {
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
        if (clientAsync) {
            val queueSize = workCount!!.get()
            if (queueSize > 10) {
                RouterLogger.getCoreLogger().w(
                    "[Client][SharedMemory] $clientName async works: $queueSize")
            }
            if (queueSize < MAX_QUEUE_SIZE && queueSize * dataBuffer.capacity() <= MAX_MEMORY_COPY) {
                workCount!!.incrementAndGet()
                val realBuffer = dataBuffer.duplicate()
                handler?.post {
                    realBuffer.clear()
                    clientCallback?.onNotify(realBuffer)
                    workCount!!.decrementAndGet()
                }
            } else {
                RouterLogger.getCoreLogger().e(
                    "[Client][SharedMemory] $clientName abandon work, queue size: $queueSize")
            }
        } else {
            dataBuffer.clear()
            clientCallback?.onNotify(dataBuffer)
        }
        clear()
    }

    @Synchronized
    fun close() {
        if (!selfClose) {
            selfClose = true
            // wait if there is a task is still running.
            if (handler != null) {
                handler?.removeCallbacksAndMessages(null)
                handler?.post {
                    closeInner()
                }
            } else {
                closeInner()
            }
        }
    }

    private fun closeInner() {
        RouterLogger.getCoreLogger().w("[Client][SharedMemory] client $clientName real close")
        DRouter.build(ISharedMemory::class.java)
            .setRemote(Strategy(authority).setCallAsync(true))
            .getService()
            .close(memoryName, serial)
        if (this::dataBuffer.isInitialized) {
            SharedMemory.unmap(dataBuffer)
        }
        if (this::oriBuffer.isInitialized) {
            SharedMemory.unmap(oriBuffer)
        }
        if (this::socketServer.isInitialized) {
            socketServer.close()
        }
        handlerThread?.quitSafely()
    }

    private fun clear() {
        oriBuffer.put(dataBuffer.capacity() + serial, MemoryServer.CLIENT_STATE_FREE)
    }

    abstract class MemCallback {
        open fun onConnected(info: Bundle?) {}
        abstract fun onNotify(buffer: ByteBuffer)
        open fun onServerClosed() {}
    }
}