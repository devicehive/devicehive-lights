/*
 *
 *
 *   AudioRecorder.java
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

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Log;

import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioRecorder {

    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int SAMPLE_RATE = 16000; // Hz
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    public static final int RECORD_LENGTH = 3;
    public static final int SAMPLES_SIZE = 32768;

    public static final int BUFFER_SIZE = RECORD_LENGTH * SAMPLE_RATE;
    private static AtomicBoolean isCancelled = new AtomicBoolean(false);

    static void stop() {
        isCancelled.set(true);
    }

    public static byte[] record() {
        isCancelled.set(false);
        byte[] buffer = new byte[BUFFER_SIZE];
        System.out.println("BF LENGTH:" + buffer.length);
        try (AudioRecordClosable audioRecord =
                     new AudioRecordClosable(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE)) {
            // Avoiding loop allocations
            boolean run = true;

            audioRecord.startRecording();
            new Thread(() -> {
                DateTime nowPlu5seconds = DateTime.now().plusSeconds(5);
                System.out.println();
                System.out.print("Recording: ");
                while (nowPlu5seconds.isAfter(DateTime.now())) {
                    System.out.print(".");
                }
                System.out.println();
                AudioRecorder.stop();
            }).start();
            while (!isCancelled.get()) {
                audioRecord.read(buffer, 0, buffer.length);
            }
        } catch (IOException ex) {
            return new byte[]{};
        }
        return buffer;
    }

    public static short[] convert16bitAudio(byte[] input) {
        short[] shorts = new short[input.length / 2];
        // to turn bytes to shorts as either big endian or little endian.
        ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
    }

    public static double[] convertToDouble(byte[] input) {
        double[] out = new double[input.length / 2];
        // to turn bytes to shorts as either big endian or little endian.
        ByteBuffer bb = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < out.length; i++) {
            out[i] = bb.getShort();
        }
        Log.d("DOUBLE", Arrays.toString(out));
        return out;
    }

}
