package com.didi.drouter.memory

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.os.Build
import android.os.Bundle
import android.os.SharedMemory
import android.system.ErrnoException
import android.system.OsConstants
import androidx.annotation.RequiresApi
import com.didi.drouter.utils.RouterLogger
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by gaowei on 2022/1/27
 */
@RequiresApi(api = Build.VERSION_CODES.O_MR1)
class MemoryServer private constructor() {

     companion object {
         internal val servers = ConcurrentHashMap<String, MemoryServer>()
         const val CLIENT_STATE_FREE: Byte = 0
         const val CLIENT_STATE_BUSY: Byte = 1

         fun create(memoryName: String, memorySize: Int, maxClient: Int = 16, info: Bundle? = null): MemoryServer? {
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
                 memoryServer.info = info
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
    internal var info: Bundle? = null
    private lateinit var oriBuffer: ByteBuffer
    private lateinit var dataBuffer: ByteBuffer
    internal lateinit var memory: SharedMemory
    private val clients = ConcurrentHashMap<Int, ClientInfo>()
    private lateinit var clientPlaceHolder: BooleanArray

    /**
     * If closed, buffer can't be used.
     * @return buffer
     */
    @Synchronized
    fun acquireBuffer(): ByteBuffer? {
        val tag = StringBuffer()
        var skip = false
        var hasBusy = false
        for (client in clients.values) {
            client.socket?.let {
                if (oriBuffer.get(dataBuffer.capacity() + client.serial) == CLIENT_STATE_BUSY) {
                    hasBusy = true
                    tag.append("${client.serial},")
                    if (client.delayCount++ < client.delay) {
                        skip = true
                    }
                } else {
                    client.delayCount = 0
                }
            }
        }
        if (hasBusy) {
            RouterLogger.getCoreLogger().w("[Server][SharedMemory] $name acquire buffer (${tag.dropLast(1)}) is busy")
        }
        if (skip) {
            RouterLogger.getCoreLogger().e("[Server][SharedMemory] $name acquire buffer fail")
        }
        return if (!skip) dataBuffer else null
    }

    @Synchronized
    internal fun addClient(delay: Int, filter: Int): ClientInfo? {
        if (!close) {
            for (i in clientPlaceHolder.indices) {
                if (!clientPlaceHolder[i]) {
                    clientPlaceHolder[i] = true
                    val client = ClientInfo(i, delay, filter)
                    clients[i] = client
                    RouterLogger.getCoreLogger().w("[Server][SharedMemory] $name add client serial: $i")
                    return client
                }
            }
        }
        RouterLogger.getCoreLogger().e("[Server][SharedMemory] $name The client register fail")
        return null
    }

    @Synchronized
    internal fun openSocket(serial: Int, socketName: String) {
        val socket = LocalSocket()
        val address = LocalSocketAddress(socketName)
        socket.connect(address)
        clients[serial]?.let { client ->
            client.socket = socket
        }
    }

    @Synchronized
    internal fun removeClient(serial: Int) {
        clients.remove(serial)?.let { client ->
            // clear
            clientPlaceHolder[client.serial] = false
            oriBuffer.put(dataBuffer.capacity() + client.serial, CLIENT_STATE_FREE)
            client.socket?.close()
            tryCloseInner()
            RouterLogger.getCoreLogger().w("[Server][SharedMemory] $name remove client serial: $serial")
        }
    }

    @Synchronized
    fun notifyClient() {
        for (client in clients.values) {
            client.socket?.let { socket ->
                var notFilter = client.filter <= 0
                if (!notFilter) {
                    notFilter = (client.filterCount++) % (client.filter + 1) == 0
                    if (client.filterCount == client.filter + 1) {
                        client.filterCount = 0
                    }
                }
                val free = oriBuffer.get(dataBuffer.capacity() + client.serial) == CLIENT_STATE_FREE
                if (close || (notFilter && free)) {
                    oriBuffer.put(dataBuffer.capacity() + client.serial, CLIENT_STATE_BUSY)
                    try {
                        socket.outputStream.write(if (close) 1 else 0)
                    } catch (e: IOException) {
                        RouterLogger.getCoreLogger().e(
                            "[Server][SharedMemory] $name socket notify error for client ${client.serial}")
                        removeClient(client.serial)
                    }
                }
//                else {
//                    RouterLogger.getCoreLogger().w(
//                        "[Server][SharedMemory] $name frame ${client.filterCount} skip notify client ${client.serial}")
//                }
            }
        }
        if (!close) {
            dataBuffer.clear()
        }
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
            RouterLogger.getCoreLogger().w("[Server][SharedMemory] server $name real close")
            servers.remove(name)
            SharedMemory.unmap(dataBuffer)
            SharedMemory.unmap(oriBuffer)
            memory.close()
        }
    }

    internal class ClientInfo(
            val serial: Int,
            val delay: Int,
            val filter: Int) {
        var socket: LocalSocket? = null
        var copy = false
        var delayCount = 0
        var filterCount = 0
    }
}