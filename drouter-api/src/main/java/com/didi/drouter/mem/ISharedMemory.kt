package com.didi.drouter.mem

import android.os.*
import androidx.annotation.RequiresApi
import com.didi.drouter.annotation.Remote
import com.didi.drouter.annotation.Service
import com.didi.drouter.remote.IRemoteCallback
import com.didi.drouter.utils.RouterLogger

/**
 * Created by gaowei on 2022/1/28
 */
@RequiresApi(api = Build.VERSION_CODES.O_MR1)
interface ISharedMemory {

    fun acquireMemory(name: String, filter: Int?, notify: IRemoteCallback.Type2<Boolean, Bundle?>): ServerMemory?
    fun release(name: String, serial: Int?)

    @Service(function = [ISharedMemory::class])
    class SharedMemoryImpl : ISharedMemory {
        @Remote
        override fun acquireMemory(name: String, filter: Int?, notify: IRemoteCallback.Type2<Boolean, Bundle?>): ServerMemory? {
            val server = MemoryServer.servers[name]
            if (server != null) {
                val client = server.addClient(filter!!, notify)
                if (client != null) {
                    return ServerMemory(server.memory, server.maxClient, client.serial)
                }
            } else {
                RouterLogger.getCoreLogger().e("[Server][SharedMemory] There is no server for name \"$name\"")
            }
            return null
        }
        @Remote
        override fun release(name: String, serial: Int?) {
            val server = MemoryServer.servers[name]
            server?.removeClient(serial!!)
        }
    }

    class ServerMemory : Parcelable {
        internal val memory: SharedMemory
        internal val maxClient: Int
        internal val serial: Int
        constructor(memory: SharedMemory, maxClient: Int, serial: Int) {
            this.memory = memory
            this.maxClient = maxClient
            this.serial = serial
        }
        constructor(data: Parcel) {
            memory = data.readParcelable(javaClass.classLoader)!!
            maxClient = data.readInt()
            serial = data.readInt()
        }
        override fun describeContents(): Int {
            return 0
        }
        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeParcelable(memory, 0)
            dest.writeInt(maxClient)
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