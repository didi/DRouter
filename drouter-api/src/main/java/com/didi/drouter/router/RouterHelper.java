package com.didi.drouter.router;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.didi.drouter.annotation.Router;

import static com.didi.drouter.router.ResultAgent.FIELD_START_ACTIVITY_REQUEST_NUMBER;

/**
 * Created by gaowei on 2020/10/15
 */
public class RouterHelper {

    /**
     * It will take effect when {@link Router#hold()} = true.
     * Don't forget release your task. {@link RouterHelper#release(Activity)}
     */
    public static @Nullable Request getRequest(@NonNull Activity activity) {
        Intent activityIntent = activity.getIntent();
        String number = activityIntent != null ?
                activityIntent.getStringExtra(FIELD_START_ACTIVITY_REQUEST_NUMBER) : null;
        return getRequest(number);
    }

    /**
     * It will take effect when {@link Router#hold()} = true.
     * Don't forget release your task. {@link RouterHelper#release(Activity)}
     */
    public static @Nullable Result getResult(@NonNull Activity activity) {
        Request request = getRequest(activity);
        if (request != null) {
            return getResult(request.getNumber());
        }
        return null;
    }

    /**
     * @param number {@link Request#getNumber()}
     * @return The request for this number.
     */
    public static Request getRequest(String number) {
        return ResultAgent.getRequest(number);
    }

    /**
     * @param number {@link Request#getNumber()}
     * @return The Result for this request.
     */
    public static Result getResult(String number) {
        return ResultAgent.getResult(number);
    }

    /**
     * It will take effect when {@link Router#hold()} = true.
     * Release your task.
     */
    public static void release(Activity activity) {
        ResultAgent.release(getRequest(activity), ResultAgent.STATE_COMPLETE);
    }

    /**
     * It will take effect when {@link Router#hold()} = true.
     * Release your task.
     */
    public static void release(Request request) {
        ResultAgent.release(request, ResultAgent.STATE_COMPLETE);
    }

    /**
     * Attention!
     * If there are multiple request branch, this can get primary request only.
     */
    public static @NonNull Request getPrimaryRequest(@NonNull Result result) {
        return result.agent.primaryRequest;
    }
}
