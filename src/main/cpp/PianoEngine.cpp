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

#include "PianoEngine.h"

#include <math.h>

#include <android/log.h>
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "semitone", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,   "semitone", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,    "semitone", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,    "semitone", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,   "semitone", __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,   "semitone", __VA_ARGS__)

PianoEngine::PianoEngine(AAssetManager &am) : am(am) { init(); }
PianoEngine::~PianoEngine() { deinit(); }

void PianoEngine::init() {
    oboe::AudioStreamBuilder asb;
    asb.setChannelCount(1);
    asb.setSharingMode(oboe::SharingMode::Exclusive);
    asb.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    asb.setCallback(this);

    oboe::Result res = asb.openStream(&stream);
    if (res != oboe::Result::OK || stream == nullptr) return;

    stream->setBufferSizeInFrames(stream->getFramesPerBurst());
    is16bit = stream->getFormat() == oboe::AudioFormat::I16;
    if (is16bit) buf16 = std::make_unique<float[]>(
            stream->getBufferCapacityInFrames() * stream->getChannelCount());
    stream->requestStart();
}

void PianoEngine::deinit() {
    if (stream == nullptr) return;
    stream->requestStop();
    stream->close();
}

void PianoEngine::play(int pitch) {
    mode = TONE_MODE;
    tonesLock.lock();
    for (int i = 0; i < MAX_TONES; ++i) {
        if (tones[i] == nullptr) {
            tones[i] = new Tone(pitch);
            break;
        }
    }
    tonesLock.unlock();
}

void PianoEngine::stop(int pitch) {
    for (int i = 0; i < MAX_TONES; ++i) {
        Tone *t = tones[i];
        if (t != nullptr && t->pitch == pitch) t->stopped = true;
    }
}

void PianoEngine::playFile(const char *path) {
    mode = SOUND_MODE;
    soundsLock.lock();
    for (int i = 0; i < MAX_SOUNDS; ++i) {
        if (sounds[i] == nullptr) {
            sounds[i] = new Sound(am, path, 1);
            break;
        }
    }
    soundsLock.unlock();
}

oboe::DataCallbackResult PianoEngine::onAudioReady(oboe::AudioStream *stream, void *data, int32_t frames) {
    float *outBuf = is16bit ? buf16.get() : static_cast<float*>(data);
    int channels = stream->getChannelCount();

    if (mode == TONE_MODE) {
        // count tones and delete stopped ones
        int nTones = 0;
        for (int i = 0; i < MAX_TONES; ++i) {
            if (tones[i] != nullptr) {
                if (tones[i]->stopped) {
                    // set to nullptr before deleting for thread-safety
                    // (we don't want to delete while stop() has a handle)
                    Tone *tmp = tones[i];
                    tones[i] = nullptr;
                    delete tmp;
                } else ++nTones;
            }
        }

        for (int i = 0; i < frames; ++i) {
            float thing = 0;
            if (nTones) {
                for (int j = 0; j < MAX_TONES; ++j) {
                    if (tones[j] != nullptr) thing += tones[j]->tick();
                }
                thing /= nTones;
                // if we simply divide by the number of tones, the difference
                // between one tone and two played simultaneously is too dramatic,
                // so scale single tones far down first and gradually bring them
                // back up
                thing *= 1-expf(-(nTones-1)*0.5f)/2;
            }
            for (int ch = 0; ch < channels; ++ch) outBuf[i*channels+ch] = thing;
        }
    } else if (mode == SOUND_MODE) {
        for (int i = 0; i < frames; ++i) {
            float thing = 0;
            for (int j = 0; j < MAX_SOUNDS; ++j) {
                if (sounds[j] != nullptr) {
                    thing += (sounds[j]->data.get())[sounds[j]->offset];
                    if (++sounds[j]->offset == sounds[j]->nSamples) {
                        delete sounds[j];
                        sounds[j] = nullptr;
                    }
                }
            }
            for (int ch = 0; ch < channels; ++ch) outBuf[i*channels+ch] = thing;
        }
    }

    if (is16bit) oboe::convertFloatToPcm16(outBuf, static_cast<int16_t*>(data), frames*channels);
    return oboe::DataCallbackResult::Continue;
}

void PianoEngine::onErrorAfterClose(oboe::AudioStream *stream, oboe::Result err) {
    if (err == oboe::Result::ErrorDisconnected && restartLock.try_lock()) {
        deinit();
        init();
        restartLock.unlock();
    }
}
