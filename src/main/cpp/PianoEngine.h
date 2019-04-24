#ifndef __PIANO_ENGINE_H__
#define __PIANO_ENGINE_H__

#include <thread>
#include <oboe/Oboe.h>

class PianoEngine : oboe::AudioStreamCallback {

public:
    PianoEngine();
    ~PianoEngine();
    void play(double freq);

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *stream, void *data, int32_t frames);
    void onErrorAfterClose(oboe::AudioStream *stream, oboe::Result err);

private:
    void init();
    void deinit();

    oboe::AudioStream *stream;
    bool is16bit;
    std::unique_ptr<float[]> buf16;

    float phase = 0, phaseIncrement = 0;

    std::mutex restartLock;

};

#endif
