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

void PianoEngine::pause() {
    stream->requestPause();
    /* stream->waitForStateChange(oboe::StreamState::Pausing, nullptr, 1000000000); */
    tonesLock.lock();
    for (int i = 0; i < MAX_TONES; ++i) {
        Tone *tmp = tones[i];
        if (tmp != nullptr) tmp->stopped = true;
    }
    tonesLock.unlock();
    soundsLock.lock();
    for (int i = 0; i < MAX_SOUNDS; ++i) {
        Sound *tmp = sounds[i];
        if (tmp != nullptr) tmp->stopped = true;
    }
    soundsLock.unlock();
}

void PianoEngine::resume() {
    stream->requestStart();
}

void PianoEngine::play(int pitch, int concert_a) {
    mode = TONE_MODE;
    tonesLock.lock();
    for (int i = 0; i < MAX_TONES; ++i) {
        if (tones[i] == nullptr) {
            tones[i] = new Tone(pitch, concert_a);
            break;
        }
    }
    tonesLock.unlock();
}

void PianoEngine::stop(int pitch) {
    tonesLock.lock();
    for (int i = 0; i < MAX_TONES; ++i) {
        Tone *t = tones[i];
        if (t != nullptr && t->pitch == pitch) t->stopped = true;
    }
    tonesLock.unlock();
}

void PianoEngine::playFile(const char *path, int concert_a) {
    mode = SOUND_MODE;
    soundsLock.lock();
    for (int i = 0; i < MAX_SOUNDS; ++i) {
        if (sounds[i] == nullptr) {
            Sound *s = new Sound(am, path, concert_a, 1);
            if (s->nSamples == 0) delete s;
            else sounds[i] = s;
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
            Tone *tmp = tones[i];
            if (tmp != nullptr) {
                if (tmp->stopped) {
                    tones[i] = nullptr;
                    delete tmp;
                } else ++nTones;
            }
        }

        for (int i = 0; i < frames; ++i) {
            float thing = 0;
            if (nTones) {
                for (int j = 0; j < MAX_TONES; ++j) {
                    Tone *tmp = tones[j];
                    if (tmp != nullptr) thing += tmp->tick();
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
        for (int i = 0; i < MAX_SOUNDS; ++i) {
            Sound *tmp = sounds[i];
            if (tmp != nullptr && tmp->stopped) {
                sounds[i] = nullptr;
                delete tmp;
            }
        }

        for (int i = 0; i < frames; ++i) {
            float thing = 0;
            for (int j = 0; j < MAX_SOUNDS; ++j) {
                Sound *tmp = sounds[j];
                if (tmp != nullptr) {
                    thing += (tmp->data.get())[tmp->offset];
                    if (++tmp->offset == tmp->nSamples) {
                        sounds[j] = nullptr;
                        delete tmp;
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
