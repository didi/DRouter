package com.didi.drouter.plugin;

import org.gradle.api.Project;

/**
 * temporary global access "gradle.properties".
 * (If don't upgrade plugin version for [RouterSetting])
 *
 * Created by holmes on 2021/10/7
 */
public class RouterProperties {

    private RouterProperties() {
    }

    public static boolean getBoolean(Project project, String key) {
        Object obj = project.getProperties().get(key);
        if (obj == null) {
            return false;
        }
        if (obj instanceof String) {
            return Boolean.parseBoolean((String) obj);
        } else if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        return false;
    }

    /**
     * key: "drouter.useActivityRouterClass"
     *
     */
    public static boolean useActivityRouterClass = false;


}
