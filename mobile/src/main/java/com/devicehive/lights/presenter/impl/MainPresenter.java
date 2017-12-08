/*
 *
 *
 *   MainPresenter.java
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

import android.graphics.Color;
import android.util.Log;

import com.devicehive.lights.model.ColorParam;
import com.devicehive.lights.presenter.base.AbstractPresenter;
import com.devicehive.lights.presenter.base.MvpView;
import com.devicehive.lights.tools.AudioRecorder;
import com.devicehive.lights.tools.BeatDetector;
import com.devicehive.lights.tools.PreferencesHelper;
import com.github.devicehive.client.model.DHResponse;
import com.github.devicehive.client.model.Parameter;
import com.github.devicehive.client.service.Device;
import com.github.devicehive.client.service.DeviceCommand;
import com.github.devicehive.client.service.DeviceHive;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class MainPresenter extends AbstractPresenter<MvpView> {
    private static final String COLORS = "colors";
    private static final String SPEED = "speed";

    private static final String LIGHTS_CHRISTMAS = "CHRISTMAS_TIME";
    private static final String LIGHTS_BLINKER = "BLINK";
    private static final String LIGHTS_RANDOM = "RANDOM";
    private static final String LIGHTS_OFF = "OFF";
    PreferencesHelper helper = PreferencesHelper.getInstance();

    private DeviceHive deviceHive = DeviceHive.getInstance().init(helper.getServerUrl(), helper.getRefreshToken());

    public MainPresenter() {
        deviceHive.enableDebug(true);
    }

    public void sendBlink() {
        Observable.just("s").subscribeOn(Schedulers.io()).subscribe(s -> {
            Device device = getDevice();
            device.sendCommand(LIGHTS_BLINKER, null);
        }, Throwable::printStackTrace);
    }

    public void sendRandom() {
        Observable.just("s").subscribeOn(Schedulers.io()).subscribe(s -> {
            Device device = getDevice();
            device.sendCommand(LIGHTS_RANDOM, null);
        }, Throwable::printStackTrace);
    }

    public void offLed() {
        Observable.just("s").subscribeOn(Schedulers.io()).subscribe(s -> {
            Device device = getDevice();
            device.sendCommand(LIGHTS_OFF, null);
        }, Throwable::printStackTrace);
    }

    public Observable<Boolean> captureAudio() {
        return Observable.just(this)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .map(s -> {
                    Device device = getDevice();
                    if (device == null) {
                        return false;
                    }

                    byte[] bytes = AudioRecorder.record();

                    if (bytes.length == 0) {
                        System.out.println("EMPTY");
                        return false;
                    }
                    //Count speed from BPM
                    int speed = getSpeedOfAudio(bytes);
                    Parameter speedParam = new Parameter(SPEED, String.valueOf(speed));

                    //Transform input bytes to Fourier series
                    Complex[] fourierSeries = fftTransformation(bytes);

                    //Generate colors from Fourier series
                    Complex[] complexesColors = Arrays.copyOfRange(fourierSeries, 3, 303);
                    List<ColorParam> colors = generateColors(complexesColors);
                    String colorParamsJson = createColorParamsJson(colors);
                    Parameter colorParam = new Parameter(COLORS, colorParamsJson);

                    List<Parameter> parameters = new ArrayList<>();
                    parameters.add(speedParam);
                    parameters.add(colorParam);

                    DHResponse<DeviceCommand> command = device.sendCommand(LIGHTS_CHRISTMAS, parameters);

                    return command.isSuccessful();
                });
    }

    private Complex[] fftTransformation(byte[] bytes) {
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        return fft.transform(Arrays.copyOf(AudioRecorder.convertToDouble(bytes), AudioRecorder.SAMPLES_SIZE),
                TransformType.FORWARD);
    }

    private List<ColorParam> generateColors(Complex[] complexes) {
        List<ColorParam> list = new ArrayList<>();
        double maxValue = getMaxValue(complexes);
        for (Complex c : complexes) {
            int colorValue = Color.HSVToColor(new float[]{
                    Double.valueOf(Math.abs(c.getArgument() / maxValue)).floatValue() * 360.0f, 1.0f, 0.1f
            });
            float r = ((colorValue >> 16) & 0xff) / 255.0f;
            float g = ((colorValue >> 8) & 0xff) / 255.0f;
            float b = ((colorValue) & 0xff) / 255.0f;

            ColorParam colorParam = new ColorParam();
            colorParam.setRed(r);
            colorParam.setGreen(g);
            colorParam.setBlue(b);

            if (colorParam.getRed() > 0 || colorParam.getBlue() > 0 || colorParam.getGreen() > 0) {
                if ((colorParam.getRed() + colorParam.getBlue() + colorParam.getGreen()) < 300) {
                    list.add(colorParam);
                }
            }

        }
        return list;
    }


    private double getMaxValue(Complex[] array) {
        List<Double> doubles = new ArrayList<>();
        for (Complex d : array) {
            doubles.add(Math.abs(d.getArgument()));
        }
        return Collections.max(doubles);
    }

    private Device getDevice() {
        DHResponse<Device> deviceDHResponse = deviceHive.getDevice(helper.getDeviceId());
        if (deviceDHResponse.isSuccessful()) {
            return deviceDHResponse.getData();
        } else {
            Log.e("ERROR", "FAILED TO GET DEVICE WITH " + deviceDHResponse.getFailureData());
            return null;
        }
    }


    private String createColorParamsJson(List<ColorParam> params) {
        Type listOfColors = new TypeToken<List<ColorParam>>() {
        }.getType();
        Gson gson = new Gson();
        return gson.toJson(params, listOfColors);
    }

    private int getSpeedOfAudio(byte[] bytes) {
        int bpm = BeatDetector.findBPM(AudioRecorder.convert16bitAudio(bytes));
        return (int) ((1f / (bpm / 60f)) * 1000f);
    }

    @Override
    protected void onDestroyed() {

    }
}
