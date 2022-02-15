package com.didi.drouter.memory

import android.os.*
import androidx.annotation.RequiresApi
import com.didi.drouter.annotation.Remote
import com.didi.drouter.annotation.Service

/**
 * Created by gaowei on 2022/1/28
 */
@RequiresApi(api = Build.VERSION_CODES.O_MR1)
interface ISharedMemory {

    fun acquireMemory(name: String, delay: Int?, filter: Int?): ServerMemory?
    fun openSocket(name: String, serial: Int?, socketName: String)
    fun close(name: String, serial: Int?)

    @Service(function = [ISharedMemory::class])
    class SharedMemoryImpl : ISharedMemory {
        @Remote
        override fun acquireMemory(name: String, delay: Int?, filter: Int?): ServerMemory? {
            MemoryServer.servers[name]?.let { server ->
                val client = server.addClient(delay!!, filter!!)
                if (client != null) {
                    return ServerMemory(server.memory, server.maxClient, server.info, client.serial)
                }
            }
            return null
        }
        @Remote
        override fun openSocket(name: String, serial: Int?, socketName: String) {
            MemoryServer.servers[name]?.openSocket(serial!!, socketName)
        }
        @Remote
        override fun close(name: String, serial: Int?) {
            MemoryServer.servers[name]?.removeClient(serial!!)
        }
    }

    class ServerMemory : Parcelable {
        internal val memory: SharedMemory
        internal val maxClient: Int
        internal var info: Bundle? = null
        internal val serial: Int
        constructor(memory: SharedMemory, maxClient: Int, info:Bundle?, serial: Int) {
            this.memory = memory
            this.maxClient = maxClient
            this.info = info
            this.serial = serial
        }
        constructor(data: Parcel) {
            memory = data.readParcelable(javaClass.classLoader)!!
            maxClient = data.readInt()
            info = data.readBundle(javaClass.classLoader)
            serial = data.readInt()
        }
        override fun describeContents(): Int {
            return 0
        }
        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeParcelable(memory, 0)
            dest.writeInt(maxClient)
            dest.writeBundle(info)
            dest.writeInt(serial)
        }
        companion object CREATOR : Parcelable.Creator<ServerMemory> {
            override fun createFromParcel(parcel: Parcel): ServerMemory {
                return ServerMemory(parcel)
            }
            override fun newArray(size: Int): Array<ServerMemory?> {
                return arrayOfNulls(size)
            }
        }
    }
}