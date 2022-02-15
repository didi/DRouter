package com.didi.demo.remote;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.didi.drouter.memory.MemoryClient;
import com.didi.drouter.utils.RouterLogger;

import java.nio.ByteBuffer;

/**
 * Created by gaowei on 2022/2/4
 */
@RequiresApi(api = Build.VERSION_CODES.O_MR1)
public class RemoteService extends Service {

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        for (int i = 0; i < 1; i++) {
            MemoryClient client = new MemoryClient(
                    "com.didi.drouter.remote.demo.host", "host2", 0);
            final int finalI = i;
            client.registerObserver(new MemoryClient.MemCallback() {
                @Override
                public void onNotify(@NonNull ByteBuffer buffer) {

//                    try {
//                        Thread.sleep(13000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    RouterLogger.getAppLogger().d("SharedMemory onNotify %s", finalI);
                }

                @Override
                public void onServerClosed() {
                    RouterLogger.getAppLogger().e("SharedMemory server closed");
                }
            });
        }
    }
}
