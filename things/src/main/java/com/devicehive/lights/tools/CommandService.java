/*
 *
 *
 *   CommandService.java
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

import android.graphics.Color;
import android.text.TextUtils;

import com.devicehive.lights.model.BlinkParams;
import com.devicehive.lights.model.ColorJson;
import com.devicehive.lights.model.ColorParam;
import com.github.devicehive.client.model.CommandFilter;
import com.github.devicehive.client.model.DHResponse;
import com.github.devicehive.client.model.DeviceCommandsCallback;
import com.github.devicehive.client.model.FailureData;
import com.github.devicehive.client.service.Device;
import com.github.devicehive.client.service.DeviceCommand;
import com.github.devicehive.client.service.DeviceHive;
import com.github.devicehive.rest.model.JsonStringWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CommandService {
    //TODO to make this application work set URL, REFRESH_TOKEN and DEVICE_ID
    public static final String URL = "";
    public static final String REFRESH_TOKEN = "";
    public static final String DEVICE_ID = "";

    public static final String DEVICE_ID_FORMAT = "Device Id: %s";

    private static final String LIGHTS_AUDIO = "AUDIO";
    private static final String LIGHTS_BLINKER = "BLINK";
    private static final String LIGHTS_RANDOM = "RANDOM";
    private static final String LIGHTS_OFF = "OFF";

    private DeviceHive deviceHive;
    private WS2812B strip;
    private ScheduledFuture<?> future;
    private ScheduledExecutorService scheduledExecutorService;
    private ExecutorService service;

    private CommandService() {
        if (TextUtils.isEmpty(URL)) {
            throw new RuntimeException("URL cannot be null or empty");
        }
        if (TextUtils.isEmpty(REFRESH_TOKEN)) {
            throw new RuntimeException("URL cannot be null or empty");
        }
        if (TextUtils.isEmpty(DEVICE_ID)) {
            throw new RuntimeException("URL cannot be null or empty");
        }

        deviceHive = DeviceHive.getInstance().init(URL, REFRESH_TOKEN);
        strip = new WS2812B();
        service = Executors.newFixedThreadPool(1);
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    public static CommandService getInstance() {
        return CommandService.InstanceHolder.INSTANCE;
    }

    private static class InstanceHolder {
        static final CommandService INSTANCE = new CommandService();
    }

    public void start() {
        service.submit(new Runnable() {
            @Override
            public void run() {
                DHResponse<Device> deviceDHResponse = deviceHive.getDevice(DEVICE_ID);
                if (!deviceDHResponse.isSuccessful()) {
                    return;
                }
                Device device = deviceDHResponse.getData();
                CommandFilter commandFilter = new CommandFilter();
                commandFilter.setCommandNames(LIGHTS_BLINKER, LIGHTS_RANDOM, LIGHTS_AUDIO, LIGHTS_OFF);


                device.subscribeCommands(commandFilter, new DeviceCommandsCallback() {
                    @Override
                    public void onSuccess(List<DeviceCommand> list) {
                        DeviceCommand command = list.get(0);
                        if (Objects.equals(command.getCommandName(), LIGHTS_AUDIO)) {
                            if (future != null) {
                                future.cancel(true);
                            }

                            future = getlights(command.getParameters());

                        } else if (Objects.equals(command.getCommandName(), LIGHTS_BLINKER)) {
                            if (future != null) {
                                future.cancel(true);
                            }
                            future = getBlinker();


                        } else if (Objects.equals(command.getCommandName(), LIGHTS_RANDOM)) {
                            if (future != null) {
                                future.cancel(true);
                            }
                            future = getRandom();

                        } else if (Objects.equals(command.getCommandName(), LIGHTS_OFF)) {
                            if (future != null) {
                                future.cancel(true);
                            }
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            strip.offLed();
                        }
                    }

                    @Override
                    public void onFail(FailureData failureData) {
                        if (future != null) {
                            future.cancel(true);
                        }
                        strip.offLed();
                    }
                });
                while (true) {
                    //special for TeamLead
                }
            }
        });
    }

    public void stop() {
        service.shutdownNow();
    }

    private ScheduledFuture<?> getBlinker() {
        final BlinkParams params = new BlinkParams();
        final LinkedList<Color> colors = new LinkedList<>();
        for (int i = 0; i < 62; i++) {
            colors.add(Color.valueOf(Color.BLACK));
        }

        strip.commit(colors);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (params.getIter() <= 30) {
                    params.iterateForward();
                    colors.set(params.getLeft(), Color.valueOf(Color.WHITE));
                    colors.set(params.getRight(), Color.valueOf(Color.RED));

                } else if (params.getIter() > 30 && params.getIter() < 62) {
                    colors.set(params.getLeft(), Color.valueOf(Color.BLACK));
                    colors.set(params.getRight(), Color.valueOf(Color.BLACK));
                    params.iterateBack();
                } else if (params.getIter() == 62) {
                    params.setDefaults();
                }
                strip.commit(colors);
            }
        };
        return scheduledExecutorService.scheduleAtFixedRate(r, 50, 50, TimeUnit.MILLISECONDS);
    }

    private ScheduledFuture<?> getlights(JsonStringWrapper wrapper) {
        System.out.println(wrapper.getJsonString());
        int speed = 300;
        Gson gson = new Gson();
        Type listType = new TypeToken<List<ColorParam>>() {
        }.getType();

        final ColorJson colorParam = gson.fromJson(wrapper.getJsonString(), ColorJson.class);
        final LinkedList<ColorParam> colorList = new LinkedList<>();
        try {
            List<ColorParam> colorListJson = gson.fromJson(colorParam.getColors(), listType);
            colorList.addAll(colorListJson);
            speed = Integer.valueOf(colorParam.getSpeed());
            if (speed < 100) {
                speed = 100;
            } else if (speed > 3000) {
                speed = 3000;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                colorList.addFirst(colorList.pollLast());
                strip.commit(colorList);
            }
        };

        return scheduledExecutorService.scheduleAtFixedRate(r, 10, speed, TimeUnit.MILLISECONDS);
    }

    private ScheduledFuture<?> getRandom() {
        final LinkedList<Color> colors = WS2812B.generateRandomColors(300);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                colors.addFirst(colors.pollLast());
                strip.commit(colors);
            }
        };

        return scheduledExecutorService.scheduleAtFixedRate(r, 50, 100, TimeUnit.MILLISECONDS);
    }

}
