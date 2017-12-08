/*
 *
 *
 *   LoginActivity.java
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

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;

import com.devicehive.lights.R;
import com.devicehive.lights.presenter.base.BasePresenterActivity;
import com.devicehive.lights.presenter.base.PresenterFactory;
import com.devicehive.lights.presenter.base.ResultView;
import com.devicehive.lights.presenter.impl.LoginPresenter;
import com.devicehive.lights.tools.PreferencesHelper;

import dmax.dialog.SpotsDialog;

public class LoginActivity extends BasePresenterActivity<LoginPresenter, ResultView> implements ResultView {
    public static final String URL = "url";
    public static final String TOKEN = "token";
    public static final String DEVICE_ID = "deviceId";
    TextInputEditText refreshToken;
    TextInputEditText serverAddress;
    TextInputEditText deviceId;
    TextInputLayout refreshTokenLayout;
    TextInputLayout serverAddressLayout;
    TextInputLayout deviceIdLayout;
    ImageButton login;
    private LoginPresenter presenter;
    private SpotsDialog dialog;
    PreferencesHelper helper = PreferencesHelper.getInstance();

    TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            enableLogin();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        refreshTokenLayout = findViewById(R.id.token_layout);
        serverAddressLayout = findViewById(R.id.url_layout);
        deviceIdLayout = findViewById(R.id.deviceId_layout);
        refreshToken = findViewById(R.id.token);
        serverAddress = findViewById(R.id.url);
        deviceId = findViewById(R.id.deviceId);

        refreshToken.addTextChangedListener(watcher);
        serverAddress.addTextChangedListener(watcher);
        deviceId.addTextChangedListener(watcher);

        login = findViewById(R.id.login);

        login.setOnClickListener(v -> {
            dialog = new SpotsDialog(this, getString(R.string.logging));
            dialog.show();
            presenter.login(serverAddress.getText().toString(),
                    refreshToken.getText().toString(),
                    deviceId.getText().toString());
        });

        refreshToken.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (!isFieldsEmpty())
                    login.callOnClick();
                return true;
            }
            return false;
        });
    }

    @NonNull
    @Override
    protected PresenterFactory<LoginPresenter> getPresenterFactory() {
        return LoginPresenter::new;
    }

    @Override
    protected void onPresenterPrepared(@NonNull LoginPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    protected void onPause() {
        super.onPause();
        helper.putServerUrl(serverAddress.getText().toString().trim());
        helper.putRefreshToken(refreshToken.getText().toString().trim());
        helper.putDeviceId(deviceId.getText().toString().trim());

    }

    @Override
    protected void onResume() {
        super.onResume();
        serverAddress.setText(helper.getServerUrl());
        deviceId.setText(helper.getDeviceId());
        refreshToken.setText(helper.getRefreshToken());
        enableLogin();
    }

    @Override
    public void onBackPressed() {
        if (!helper.isLoggedSuccessfully()) {
            helper.clearPreferences();
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (!helper.isLoggedSuccessfully()) {
            helper.clearPreferences();
        }
        super.onDestroy();
    }

    private void enableLogin() {
        login.setVisibility(isFieldsEmpty() ? View.INVISIBLE : View.VISIBLE);
        login.setEnabled(!isFieldsEmpty());
    }

    private boolean isFieldsEmpty() {
        return TextUtils.isEmpty(deviceId.getText().toString()) ||
                TextUtils.isEmpty(refreshToken.getText().toString()) ||
                TextUtils.isEmpty(serverAddress.getText().toString());

    }

    @Override
    public void onError(String message) {
        if (dialog != null) {
            dialog.dismiss();
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.error_title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                }).show();
    }

    @Override
    public void onSuccess() {
        if (dialog != null) {
            dialog.dismiss();
        }
        startActivity(new Intent(this, MainActivity.class));
        this.finish();
    }
}
