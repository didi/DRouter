package com.didi.drouter.mem

import android.os.Build
import android.os.Bundle
import android.os.SharedMemory
import android.system.ErrnoException
import android.system.OsConstants
import androidx.annotation.RequiresApi
import com.didi.drouter.remote.IRemoteCallback
import com.didi.drouter.utils.RouterLogger
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by gaowei on 2022/1/27
 */
@RequiresApi(api = Build.VERSION_CODES.O_MR1)
class MemoryServer private constructor() {

     companion object {
         internal val servers = ConcurrentHashMap<String, MemoryServer>()
         const val CLIENT_STATE_FREE: Byte = 0
         const val CLIENT_STATE_BUSY: Byte = 1

         fun create(memoryName: String, memorySize: Int, maxClient: Int): MemoryServer? {
             if (servers.containsKey(memoryName)) {
                 RouterLogger.getCoreLogger().e(
                         "[Server][SharedMemory] last shared memory \"$memoryName\" has not closed")
                 return null
             }
             try {
                 val allocSize = memorySize + maxClient
                 val memory = SharedMemory.create(memoryName, allocSize)
                 if (memory.size < allocSize) {
                     RouterLogger.getCoreLogger().e("[Server][SharedMemory] memory only alloc:${memory.size}")
                     return null
                 }
                 val memoryServer = MemoryServer()
                 memoryServer.name = memoryName
                 memoryServer.maxClient = maxClient
                 memoryServer.clientPlaceHolder = BooleanArray(maxClient)
                 memoryServer.memory = memory
                 // offset have to be 0
                 memoryServer.dataBuffer = memory.map(OsConstants.PROT_READ or OsConstants.PROT_WRITE,
                         0, memory.size - maxClient)
                 memoryServer.oriBuffer = memory.map(OsConstants.PROT_READ or OsConstants.PROT_WRITE,
                         0, memory.size)
                 servers[memoryName] = memoryServer
                 return memoryServer
             } catch (e: ErrnoException) {
                 RouterLogger.getCoreLogger().e("[Server][SharedMemory] \"$memoryName\" memory create error", e)
                 return null
             }
         }
    }

    private var close = false
    private var name = ""
    internal var maxClient = 0
    private lateinit var oriBuffer: ByteBuffer
    private lateinit var dataBuffer: ByteBuffer
    internal lateinit var memory: SharedMemory
    private val clients = CopyOnWriteArrayList<ClientInfo>()
    private lateinit var clientPlaceHolder: BooleanArray
    private var frameCount = 0L

    /**
     * If closed, buffer can't be used.
     * @return buffer
     */
    @Synchronized
    fun acquireBuffer(): ByteBuffer? {
        val tag = StringBuffer()
        var hasBusy = false
        for (client in clients) {
            if (oriBuffer.get(dataBuffer.capacity() + client.serial) == CLIENT_STATE_BUSY) {
                client.copy = true
                hasBusy = true
                tag.append("${client.serial},")
            }
        }
        RouterLogger.getCoreLogger().dw("[Server][SharedMemory] \"$name\" " +
                "acquire buffer ${if (!hasBusy) "success" else "fail for (${tag.dropLast(1)}) is busy"}", hasBusy)
        return if (!hasBusy) dataBuffer else null
    }

    @Synchronized
    internal fun addClient(filter: Int, callback: IRemoteCallback.Type2<Boolean, Bundle?>): ClientInfo? {
        if (!close) {
            for (i in clientPlaceHolder.indices) {
                if (!clientPlaceHolder[i]) {
                    clientPlaceHolder[i] = true
                    val client = ClientInfo(i, filter, callback)
                    clients.add(client)
                    RouterLogger.getCoreLogger().w("[Server][SharedMemory] \"$name\" add client serial: $i")
                    return client
                }
            }
        }
        RouterLogger.getCoreLogger().e("[Server][SharedMemory] \"$name\" The client \"$name\" register fail")
        return null
    }

    @Synchronized
    internal fun removeClient(serial: Int) {
        for (client in clients) {
            if (client.serial == serial) {
                RouterLogger.getCoreLogger().w("[Server][SharedMemory] \"$name\" remove client serial: $serial")
                removeClient(client)
                return
            }
        }
    }

    @Synchronized @JvmOverloads
    fun notifyClient(bundle: Bundle? = null) {
        RouterLogger.getCoreLogger().d("[Server][SharedMemory] \"$name\" notify client size ${clients.size}")
        for (client in clients) {
            if (client.callback.asBinder().isBinderAlive) {
                val reason1 = close
                val reason2 = client.filter <= 0 || frameCount % client.filter == 0L
                if (reason1 || reason2) {
                    oriBuffer.put(dataBuffer.capacity() + client.serial, CLIENT_STATE_BUSY)
                    client.callback.callback(close, bundle)
                } else {
                    RouterLogger.getCoreLogger().w(
                            "[Server][SharedMemory] \"$name\" frame $frameCount skip notify client ${client.serial}")
                }
            } else {
                removeClient(client)
            }
        }
        if (!close) {
            dataBuffer.clear()
            ++frameCount
        }
    }

    @Synchronized
    private fun removeClient(client: ClientInfo) {
        clients.remove(client)
        // clear
        clientPlaceHolder[client.serial] = false
        oriBuffer.put(dataBuffer.capacity() + client.serial, CLIENT_STATE_FREE)
        tryCloseInner()
    }

    @Synchronized
    fun close() {
        close = true
        notifyClient()
        tryCloseInner()
    }

    /**
     * wait until client has closed.
     */
    @Synchronized
    private fun tryCloseInner() {
        if (close && clients.isEmpty() && servers.containsKey(name)) {
            RouterLogger.getCoreLogger().w("[Server][SharedMemory] server \"$name\" real close")
            servers.remove(name)
            SharedMemory.unmap(dataBuffer)
            SharedMemory.unmap(oriBuffer)
            memory.close()
        }
    }

    internal class ClientInfo(
            val serial: Int,
            val filter: Int,
            val callback: IRemoteCallback.Type2<Boolean, Bundle?>) {
        var copy = false
    }
}