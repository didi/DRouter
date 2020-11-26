package com.didi.drouter.page;

import android.os.Bundle;
import android.support.annotation.NonNull;

/**
 * Created by gaowei on 2020/9/16
 */
public interface IPageBean {

    String getPageUri();

    String getPageName();

    Bundle getPageInfo();

    class DefPageBean implements IPageBean {

        private final String uri;
        private final String name;
        private final Bundle info;

        public DefPageBean(@NonNull String uri) {
            this(uri, "");
        }

        public DefPageBean(@NonNull String uri, String name) {
            this(uri, name, null);
        }

        public DefPageBean(@NonNull String uri, Bundle info) {
            this(uri, "", info);
        }

        public DefPageBean(@NonNull String uri, String name, Bundle info) {
            this.uri = uri;
            this.name = name;
            this.info = info;
        }

        @NonNull
        @Override
        public String getPageUri() {
            return uri;
        }

        @Override
        public String getPageName() {
            return name;
        }

        @Override
        public Bundle getPageInfo() {
            return info;
        }
    }

    class EmptyPageBean extends DefPageBean {
        public EmptyPageBean() {
            super("");
        }
    }
}
