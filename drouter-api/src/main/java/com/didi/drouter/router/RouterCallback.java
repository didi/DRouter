package com.didi.drouter.router;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by gaowei on 2018/9/5
 */
public interface RouterCallback {

    void onResult(@NonNull Result result);

    /**
     * RouterCallback for {@link android.app.Activity#startActivityForResult(Intent, int)} request.
     * {@link com.didi.drouter.api.Extend#START_ACTIVITY_REQUEST_CODE}
     */
    abstract class ActivityCallback implements RouterCallback {
        public @Override void onResult(@NonNull Result result) {}
        public abstract void onActivityResult(int resultCode, @Nullable Intent data);
    }

}
