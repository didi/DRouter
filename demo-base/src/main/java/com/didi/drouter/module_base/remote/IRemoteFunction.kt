//package com.didi.drouter.module_base.remote
//
//import android.content.Context
//import androidx.annotation.Keep
//import com.didi.drouter.module_base.ParamObject
//import com.didi.drouter.module_base.ResultObject
//import com.didi.drouter.remote.IRemoteCallback
//
///**
// * Created by gaowei on 2018/11/2
// */
//@Keep
//interface IRemoteFunction {
//    fun handle(x: Array<ParamObject?>?, y: ParamObject?, z: Int?, context: Context?, callback: IRemoteCallback.Type2<String, Int>?): ResultObject?
//    fun register(callback: IRemoteCallback?)
//    fun unregister(callback: IRemoteCallback?)
//    fun kill()
//    fun call()
//}