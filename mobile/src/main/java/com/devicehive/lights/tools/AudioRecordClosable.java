/*
 *
 *
 *   AudioRecordClosable.java
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

import android.media.AudioRecord;

import java.io.Closeable;
import java.io.IOException;

public class AudioRecordClosable extends AudioRecord implements Closeable {
    public AudioRecordClosable(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) throws IllegalArgumentException {
        super(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
    }

    @Override
    public void close() throws IOException {
        try {
            if (this.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                this.stop();
            }
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
        }
        if (this.getState() == AudioRecord.STATE_INITIALIZED) {
            this.release();
        }
    }
}
