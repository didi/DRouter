package com.didi.demo.remote;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.demo.R;
import com.didi.drouter.mem.MemoryClient;
import com.didi.drouter.utils.RouterLogger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gaowei on 2022/1/28
 */
@RequiresApi(api = Build.VERSION_CODES.O_MR1)
@Router(path = "/activity/remote_mem")
public class RemoteMemActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private final List<MemoryClient> clients = new ArrayList<>();
    private Bitmap bitmap;
    private Rect rect;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_mem);
        mSurfaceView = findViewById(R.id.surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        final Paint paint = new Paint();
        MemoryClient client = new MemoryClient("com.didi.drouter.remote.demo.host", "host");
        client.registerObserver(new MemoryClient.MemCallback() {
            @Override
            public void onNotify(@NonNull ByteBuffer buffer, @Nullable Bundle config) {
                RouterLogger.getAppLogger().d("SharedMemory onNotify");
                Canvas canvas = mSurfaceHolder.lockCanvas();
                if (canvas != null && config != null) {
                    if (bitmap == null) {
                        int width = config.getInt("width");
                        int height = config.getInt("height");
                        int scale = mSurfaceView.getWidth() / width;
                        int fullHeight = height * scale;
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                        rect = new Rect(0,
                                mSurfaceView.getHeight() / 2 - fullHeight / 2,
                                mSurfaceView.getWidth(),
                                mSurfaceView.getHeight() / 2 + fullHeight / 2);
                    }
                    bitmap.copyPixelsFromBuffer(buffer);
                    canvas.drawBitmap(bitmap, null, rect, paint);
                    mSurfaceHolder.unlockCanvasAndPost(canvas);

                }
            }
        }, true);
        clients.add(client);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (MemoryClient client : clients) {
            client.close();
        }
    }

}
