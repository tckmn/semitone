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

#include <android/asset_manager_jni.h>
#include <oboe/Oboe.h>
#include "mn_tck_semitone_PianoEngine.h"
#include "PianoEngine.h"

JNIEXPORT jlong JNICALL Java_mn_tck_semitone_PianoEngine_createPianoEngine
  (JNIEnv *env, jclass, jobject am) {
    return reinterpret_cast<jlong>(new(std::nothrow) PianoEngine(*AAssetManager_fromJava(env, am)));
}

JNIEXPORT void JNICALL Java_mn_tck_semitone_PianoEngine_destroyPianoEngine
  (JNIEnv*, jclass, jlong handle) {
    delete reinterpret_cast<PianoEngine*>(handle);
}

JNIEXPORT void JNICALL Java_mn_tck_semitone_PianoEngine_doPause
  (JNIEnv*, jclass, jlong handle) {
    PianoEngine *engine = reinterpret_cast<PianoEngine*>(handle);
    if (engine != nullptr) engine->pause();
}

JNIEXPORT void JNICALL Java_mn_tck_semitone_PianoEngine_doResume
  (JNIEnv*, jclass, jlong handle) {
    PianoEngine *engine = reinterpret_cast<PianoEngine*>(handle);
    if (engine != nullptr) engine->resume();
}

JNIEXPORT void JNICALL Java_mn_tck_semitone_PianoEngine_setSampleRate
  (JNIEnv*, jclass, jint val) {
    oboe::DefaultStreamValues::SampleRate = (int32_t) val;
}

JNIEXPORT void JNICALL Java_mn_tck_semitone_PianoEngine_setFramesPerBurst
  (JNIEnv*, jclass, jint val) {
    oboe::DefaultStreamValues::FramesPerBurst = (int32_t) val;
}

JNIEXPORT void JNICALL Java_mn_tck_semitone_PianoEngine_doPlay
  (JNIEnv*, jclass, jlong handle, jint pitch, jint concert_a) {
    PianoEngine *engine = reinterpret_cast<PianoEngine*>(handle);
    if (engine != nullptr) engine->play(pitch, concert_a);
}

JNIEXPORT void JNICALL Java_mn_tck_semitone_PianoEngine_doStop
  (JNIEnv*, jclass, jlong handle, jint pitch) {
    PianoEngine *engine = reinterpret_cast<PianoEngine*>(handle);
    if (engine != nullptr) engine->stop(pitch);
}

JNIEXPORT void JNICALL Java_mn_tck_semitone_PianoEngine_doPlayFile
  (JNIEnv *env, jclass, jlong handle, jstring path, jint concert_a) {
    PianoEngine *engine = reinterpret_cast<PianoEngine*>(handle);
    if (engine != nullptr) engine->playFile(env->GetStringUTFChars(path, 0), concert_a);
}
