package com.didi.demo.remote;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.demo.R;
import com.didi.drouter.memory.MemoryClient;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gaowei on 2022/1/28
 */
@RequiresApi(api = Build.VERSION_CODES.O_MR1)
@Router(path = "/activity/remote_mem")
public class RemoteMemoryActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private TextView textView;
    private TextView textViewFrequency;
    private TextView surfaceFrequency;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private final List<MemoryClient> clients = new ArrayList<>();
    private Bitmap bitmap;
    private Rect rect;
    private boolean surfaceReady;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_memory);
        textView = findViewById(R.id.text);
        textViewFrequency = findViewById(R.id.text_frequency);
        surfaceFrequency = findViewById(R.id.surface_frequency);
        surfaceView = findViewById(R.id.surface);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        registerLight();
        registerSurface();
    }

    private void registerLight() {
        MemoryClient client = new MemoryClient("com.didi.drouter.remote.demo.host", "host1",
                0, 0);
        client.registerObserver(new MemoryClient.MemCallback() {
            long t;
            int count;
            @Override
            public void onNotify(@NonNull ByteBuffer buffer) {
                if (++count % 500 == 0) {
                    long now = System.nanoTime();
                    long diff = now - t;
                    if (diff != 0) {
                        textViewFrequency.setText(String.format("帧率 %s", 1000_000_000 / diff));
                    }
                    count = 0;
                }
                t = System.nanoTime();
                // 耗时1ms左右
//                StringBuffer b = new StringBuffer();
//                for (int i = 0; i < buffer.capacity(); i++) {
//                    b.append(buffer.get(i));
//                }
//                textView.setText(b);

                //RouterLogger.getAppLogger().d("SharedMemory onNotify1");
            }
        });
        clients.add(client);
    }
    
    private void registerSurface() {
        MemoryClient client = new MemoryClient("com.didi.drouter.remote.demo.host", "host2",
                0, 0);
        client.registerObserver(new MemoryClient.MemCallback() {
            Bundle config;
            long t;
            int count;

            @Override
            public void onConnected(@Nullable Bundle info) {
                config = info;
            }

            @Override
            public void onNotify(@NonNull ByteBuffer buffer) {
                if (!surfaceReady) return;
                if (++count % 30 == 0) {
                    long now = System.currentTimeMillis();
                    long diff = now - t;
                    if (diff != 0) {
                        surfaceFrequency.setText(String.format("帧率 %s", 1000 / diff));
                    }
                    count = 0;
                }
                t = System.currentTimeMillis();

//                RouterLogger.getAppLogger().d("SharedMemory onNotify2");
                Canvas canvas = surfaceHolder.lockCanvas();
                if (config != null) {
                    if (bitmap == null) {
                        int width = config.getInt("width");
                        int height = config.getInt("height");
                        int scale = surfaceView.getWidth() / width;
                        int fullHeight = height * scale;
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                        rect = new Rect(0,
                                surfaceView.getHeight() / 2 - fullHeight / 2,
                                surfaceView.getWidth(),
                                surfaceView.getHeight() / 2 + fullHeight / 2);
                    }
                    try {
                        bitmap.copyPixelsFromBuffer(buffer);
                        // 绘制比较慢，20ms左右
                        canvas.drawBitmap(bitmap, null, rect, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        });
        clients.add(client);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
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
