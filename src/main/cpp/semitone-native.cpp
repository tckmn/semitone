#include <oboe/Oboe.h>
#include "mn_tck_semitone_PianoEngine.h"
#include "PianoEngine.h"

JNIEXPORT jlong JNICALL Java_mn_tck_semitone_PianoEngine_createPianoEngine
  (JNIEnv*, jclass) {
    return reinterpret_cast<jlong>(new(std::nothrow) PianoEngine());
}

JNIEXPORT void JNICALL Java_mn_tck_semitone_PianoEngine_destroyPianoEngine
  (JNIEnv*, jclass, jlong handle) {
    delete reinterpret_cast<PianoEngine*>(handle);
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
  (JNIEnv*, jclass, jlong handle, jint pitch) {
    PianoEngine *engine = reinterpret_cast<PianoEngine*>(handle);
    if (engine != nullptr) engine->play(pitch);
}

JNIEXPORT void JNICALL Java_mn_tck_semitone_PianoEngine_doStop
  (JNIEnv*, jclass, jlong handle, jint pitch) {
    PianoEngine *engine = reinterpret_cast<PianoEngine*>(handle);
    if (engine != nullptr) engine->stop(pitch);
}
