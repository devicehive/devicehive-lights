/*
 *
 *
 *   LoginPresenter.java
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

package com.devicehive.lights.presenter.impl;

import android.text.TextUtils;

import com.devicehive.lights.presenter.base.AbstractPresenter;
import com.devicehive.lights.presenter.base.ResultView;
import com.devicehive.lights.tools.PreferencesHelper;
import com.github.devicehive.client.model.DHResponse;
import com.github.devicehive.client.service.Device;
import com.github.devicehive.client.service.DeviceHive;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class LoginPresenter extends AbstractPresenter<ResultView> {


    public static final String ERROR_MESSAGE = "There are some issues happened. Try again later";

    public void login(String url, String token, String deviceId) {
        try {

            DeviceHive deviceHive = DeviceHive.getInstance().init(url.trim(), token.trim());

            Observable.just("")
                    .observeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())
                    .map(s -> {
                        DHResponse<Device> deviceDHResponse = deviceHive.getDevice(deviceId);
                        if (deviceDHResponse.isSuccessful()) {
                            return "";
                        } else {
                            String message = deviceDHResponse.getFailureData().getMessage();
                            return TextUtils.isEmpty(message) ? ERROR_MESSAGE : message;
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(s -> {
                        if (isViewNull()) {
                            return;
                        }
                        PreferencesHelper helper = PreferencesHelper.getInstance();
                        if (TextUtils.isEmpty(s)) {
                            helper.putServerUrl(url.trim());
                            helper.putRefreshToken(token.trim());
                            helper.putDeviceId(deviceId.trim());
                            helper.putIsLoggedSuccessfully(true);
                            view.onSuccess();
                        } else {
                            helper.putIsLoggedSuccessfully(true);
                            view.onError(s);
                        }
                    }, t -> {
                        if (!isViewNull()) {
                            view.onError(ERROR_MESSAGE);
                        }
                    });

        } catch (Error e) {
            e.printStackTrace();
            if (!isViewNull()) {
                String tokenValue = token.length() > 10 ? token.substring(0, 10) + "..." : token;
                view.onError("Incorrect input fields url: " + url + " token :" + tokenValue);
            }
        }

    }

    @Override
    protected void onDestroyed() {

    }
}
