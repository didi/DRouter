package com.didi.drouter.router;

import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by gaowei on 2018/9/5
 */
public interface RouterCallback {

    void onResult(@NonNull Result result);

    /**
     * ActivityResult for {@link android.app.Activity#startActivityForResult(Intent, int)} request.
     * You can also assign request code, {@link com.didi.drouter.api.Extend#START_ACTIVITY_REQUEST_CODE}
     *
     * @deprecated use
     * {@link Request#setActivityResultLauncher(ActivityResultLauncher)}
     */
    @Deprecated
    abstract class ActivityCallback implements RouterCallback {
        public @Override void onResult(@NonNull Result result) {}
        public abstract void onActivityResult(int resultCode, @Nullable Intent data);
    }

}
