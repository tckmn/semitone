#include "PianoEngine.h"

#include <math.h>

#include <android/log.h>
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "semitone", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,   "semitone", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,    "semitone", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,    "semitone", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,   "semitone", __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,   "semitone", __VA_ARGS__)

PianoEngine::PianoEngine() { init(); }
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
    tonesLock.lock();
    tones.push_front(Tone(pitch));
    tonesLock.unlock();
}

void PianoEngine::stop(int pitch) {
    tonesLock.lock();
    tones.remove(Tone(pitch));
    tonesLock.unlock();
}

oboe::DataCallbackResult PianoEngine::onAudioReady(oboe::AudioStream *stream, void *data, int32_t frames) {
    float *outBuf = is16bit ? buf16.get() : static_cast<float*>(data);
    tonesLock.lock();
    int channels = stream->getChannelCount(), nTones = tones.size();
    for (int i = 0; i < frames; ++i) {
        float thing = 0;
        if (nTones) {
            for (Tone &t : tones) thing += t.tick();
            thing /= nTones;
            // if we simply divide by the number of tones, the difference
            // between one tone and two played simultaneously is too dramatic,
            // so scale single tones far down first and gradually bring them
            // back up
            thing *= 1-expf(-(nTones-1)*0.5f)/2;
        }
        for (int ch = 0; ch < channels; ++ch) outBuf[i*channels+ch] = thing;
    }
    tonesLock.unlock();
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
