/*
 *
 *
 *   MainActivity.java
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

package com.devicehive.lights.view;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageButton;
import android.widget.Button;

import com.devicehive.lights.R;
import com.devicehive.lights.presenter.base.BasePresenterActivity;
import com.devicehive.lights.presenter.base.MvpView;
import com.devicehive.lights.presenter.base.PresenterFactory;
import com.devicehive.lights.presenter.impl.MainPresenter;

import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends BasePresenterActivity<MainPresenter, MvpView> implements MvpView {

    private MainPresenter presenter;
    private SpotsDialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppCompatImageButton recordButton = findViewById(R.id.recordAudio);
        AppCompatImageButton blinkButton = findViewById(R.id.blink);
        AppCompatImageButton randomButton = findViewById(R.id.random);
        Button offButton = findViewById(R.id.off);

        blinkButton.setOnClickListener(v -> presenter.sendBlink());
        randomButton.setOnClickListener(v -> presenter.sendRandom());
        offButton.setOnClickListener(v -> presenter.offLed());
        recordButton.setOnClickListener(v -> MainActivityPermissionsDispatcher.prepareAudioWithPermissionCheck(this));

    }

    @NonNull
    @Override
    protected PresenterFactory<MainPresenter> getPresenterFactory() {
        return MainPresenter::new;
    }

    @Override
    protected void onPresenterPrepared(@NonNull MainPresenter presenter) {
        this.presenter = presenter;
    }

    @NeedsPermission(value = {Manifest.permission.RECORD_AUDIO})
    public void prepareAudio() {
        dialog = new SpotsDialog(this, getString(R.string.dialog_message));
        dialog.show();
        presenter.captureAudio().observeOn(AndroidSchedulers.mainThread()).subscribe(sent -> {
            if (dialog != null) {
                dialog.dismiss();
            }
        }, Throwable::printStackTrace);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

}