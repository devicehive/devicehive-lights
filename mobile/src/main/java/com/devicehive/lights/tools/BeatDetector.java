/*
 *
 *
 *   BeatDetector.java
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

public class BeatDetector {

    final static private int TRIGGER_THRESHOLD = 30; // 0...oo
    final static private double LPF_COEFFICIENT = 0.85; // 0...1.0
    final static private double ENERGY_THRESHOLD = 1.3; // 1.0...oo
    final static private int SAMPLE_RATE = 16000;

    static public int findBPM(short[] samples) {
        double averageEnergy = 0;
        int energy[] = new int[samples.length];
        for (int i = 0; i < samples.length; i++) {
            final int v = samples[i];
            final int dv = v * v;
            energy[i] = dv;
            averageEnergy += ((double)dv) / ((double)samples.length);
        }
        int beats = 0;
        int trigger = 0;
        double lpf = 0.0;
        for (int i = 0; i < energy.length; i++) {
            lpf = lpf * LPF_COEFFICIENT + energy[i] * (1.0 - LPF_COEFFICIENT);
            if (trigger >=0 && trigger < TRIGGER_THRESHOLD) {
                if (lpf > ENERGY_THRESHOLD * averageEnergy) {
                    trigger++;
                    if(trigger == TRIGGER_THRESHOLD) {
                        beats++;
                    }
                } else {
                    trigger--;
                }
            } else if (lpf < averageEnergy) {
                trigger = 0;
            }
        }
        return (int)Math.round(((double)beats) / (((double)samples.length) / ((double)SAMPLE_RATE)) * 60.0);
    }
}
