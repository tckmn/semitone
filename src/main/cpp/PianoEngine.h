/*
 * Semitone - tuner, metronome, and piano for Android
 * Copyright (C) 2019  Andy Tockman <andy@tck.mn>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef __PIANO_ENGINE_H__
#define __PIANO_ENGINE_H__

#include <thread>
#include <android/asset_manager.h>
#include <oboe/Oboe.h>

#include "Tone.h"
#include "Sound.h"

#define MAX_TONES  100
#define MAX_SOUNDS 200

#define TONE_MODE  1
#define SOUND_MODE 2

class PianoEngine : oboe::AudioStreamCallback {

public:
    explicit PianoEngine(AAssetManager &am);
    ~PianoEngine();
    void play(int pitch);
    void stop(int pitch);
    void playFile(const char *path);

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *stream, void *data, int32_t frames);
    void onErrorAfterClose(oboe::AudioStream *stream, oboe::Result err);

private:
    void init();
    void deinit();

    AAssetManager &am;

    oboe::AudioStream *stream;
    bool is16bit;
    std::unique_ptr<float[]> buf16;

    Tone *tones[MAX_TONES] = {nullptr};
    Sound *sounds[MAX_SOUNDS] = {nullptr};
    int mode = TONE_MODE;

    std::mutex restartLock, tonesLock, soundsLock;

};

#endif
