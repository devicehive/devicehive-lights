package com.devicehive.lights.tools;

/*
Android Things demo project which controls WS2812B LED strip.
Written in 2017 by Nikolay Khabarov <2xl@mail.ru>

 To the extent possible under law, the author(s) have dedicated all copyright and related and
neighboring rights to this software to the public domain worldwide. This software is distributed
without any warranty.
 You should have received a copy of the CC0 Public Domain Dedication along with this software. If
not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
*/

import android.graphics.Color;
import android.util.Log;

import com.devicehive.lights.model.ColorParam;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class WS2812B {

    private static final String TAG = "ws2812b";
    private static SpiDevice mSpi = null;

    /**
     * This constructor creates WS2812B class which controls LED strips using SPI interface.
     * This class extends LinkedList and all LED states should be loaded into it with
     * android.graphics.Color object. Since everything is loaded into list, call commit() to
     * actually upload it to strip. Then this object can be changed and committed with new values
     * again. Few instances with different colors can be created and committed one by one.
     */
    public WS2812B() {
        if (mSpi == null) {
            init();
        }
    }

    private void init() {
        // Find and choose SPI deivce
        PeripheralManagerService manager = new PeripheralManagerService();
        List<String> deviceList = manager.getSpiBusList();
        if (deviceList.isEmpty()) {
            Log.i(TAG, "No SPI bus available on this device.");
            return;
        }
        Log.i(TAG, "List of available devices: " + deviceList);
        String device_name = deviceList.get(0);
        Log.i(TAG, "Using: " + device_name);

        // Attempt to access the SPI device
        try {
            mSpi = manager.openSpiDevice(device_name);
        } catch (IOException e) {
            Log.w(TAG, "Unable to access SPI device", e);
            return;
        }

        try {
            mSpi.setMode(SpiDevice.MODE0);
            mSpi.setFrequency(3809523); // special frequency to emulate ws2812b protocol
            mSpi.setBitsPerWord(8);
            mSpi.setBitJustification(false);
        } catch (IOException e) {
            Log.w(TAG, "Failed to configure SPI device", e);
        }
    }

    /**
     * Upload current state to LEDs.
     */
    public void commit(LinkedList<Color> colors) {
        if (!initSPIifNull()) {
            return;
        }
        int pos = 0;
        byte buffer[] = new byte[12 * colors.size()];
        for (Color led : colors) {
            byte color[] = {(byte) (led.green() * 255),
                    (byte) (led.red() * 255),
                    (byte) (led.blue() * 255)};
            for (byte aColor : color) {
                for (int j = 3; j >= 0; j--) {
                    buffer[pos++] = (byte) (0x88 + ((aColor >> (2 * j)) & 0x01) * 0x06
                            + ((aColor >> (2 * j + 1)) & 0x01) * 0x60);
                }
            }
        }
        try {
            mSpi.write(null, 60);
            mSpi.write(buffer, buffer.length);
        } catch (IOException e) {
            Log.w(TAG, "Failed to write to SPI device", e);
        }
    }

    public void commit(byte[] colors, int length) {
        if (!initSPIifNull()) {
            return;
        }
        try {
            System.out.println(colors.length);
            mSpi.write(null, 1);
            mSpi.write(colors, length);
        } catch (IOException e) {
            Log.w(TAG, "Failed to write to SPI device", e);
        }
    }

    public void commit(List<ColorParam> colors) {
        if (!initSPIifNull()) {
            return;
        }
        int pos = 0;
        byte buffer[] = new byte[12 * colors.size()];
        for (ColorParam led : colors) {
            byte color[] = {(byte) (led.getGreen() * 255),
                    (byte) (led.getRed() * 255),
                    (byte) (led.getBlue() * 255)};
            for (byte aColor : color) {
                for (int j = 3; j >= 0; j--) {
                    buffer[pos++] = (byte) (0x88 + ((aColor >> (2 * j)) & 0x01) * 0x06
                            + ((aColor >> (2 * j + 1)) & 0x01) * 0x60);
                }
            }
        }
        try {
            mSpi.write(buffer, buffer.length);
        } catch (IOException e) {
            Log.w(TAG, "Failed to write to SPI device", e);
        }
    }

    public void offLed() {
        if (!initSPIifNull()) {
            return;
        }
        byte buffer[] = new byte[12 * 300];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = -128;
        }
        for (byte b :
                buffer) {
            if (b != -128) {
                Log.d("BYTE", String.valueOf(b));
            }
        }
        try {
            //sometimes the first led is green
            mSpi.write(null, 1);
            mSpi.write(buffer, buffer.length);
        } catch (IOException e) {
            Log.w(TAG, "Failed to write to SPI device", e);
        }
    }

    private boolean initSPIifNull() {
        if (mSpi == null) {
            init();
            if (mSpi == null) {
                Log.w(TAG, "SPI is not initialized");
                return false;
            }
        }
        return true;
    }

    public static LinkedList<Color> generateRandomColors(int maxSize) {
        if (maxSize <= 0) {
            return new LinkedList<>();
        }
        LinkedList<Color> list = new LinkedList<>();
        Random rnd = new Random();
        for (int i = 0; i < maxSize; i++) {
            // using HSV palette to generate bright colors.
            list.add(i, Color.valueOf(Color.HSVToColor(new float[]{
                    rnd.nextFloat() * 360.0f, 1.0f, 0.1f
            })));
        }
        return list;
    }

    /**
     * De-initialise SPI interface. The number of instances of this class might be more then one.
     * This method should be called when any of instances no longer require.
     */
    public static void closeSPI() {
        if (mSpi != null) {
            try {
                mSpi.close();
                mSpi = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close SPI device", e);
            }
        }
    }
}
