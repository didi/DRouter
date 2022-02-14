package com.didi.drouter.store;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.SystemUtil;

/**
 * Created by gaowei on 2019-06-10
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class LoadProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        Log.d(RouterLogger.CORE_TAG, "[LoadProvider] onCreate and DRouter set application | " + getContext());
        SystemUtil.setApplication((Application) getContext());
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
