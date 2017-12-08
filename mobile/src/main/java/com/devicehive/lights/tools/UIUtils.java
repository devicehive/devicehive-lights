/*
 *
 *
 *   UIUtils.java
 *
 *   Copyright (C) 2017 DataArt
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.devicehive.lights.tools;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class UIUtils {

    public static void hideKeyboard(Activity a) {
        hideKeyboard(a.getCurrentFocus());
    }

    public static void hideKeyboard(View v) {
        if (v == null) {
            return;
        }

        View focus = v.findFocus();
        if (focus != null) {
            Context c = v.getContext();
            InputMethodManager imm =
                    (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);

            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    private UIUtils() {
        throw new UnsupportedOperationException("Do not instantiate");
    }

    public static void setViewsEnabled(boolean enabled, View... views) {
        if (views == null || views.length == 0) {
            return;
        }

        for (View v : views) {
            v.setEnabled(enabled);
        }
    }
}