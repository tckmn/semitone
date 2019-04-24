#ifndef __PIANO_ENGINE_H__
#define __PIANO_ENGINE_H__

#include <thread>
#include <oboe/Oboe.h>

#include "Tone.h"

class PianoEngine : oboe::AudioStreamCallback {

public:
    PianoEngine();
    ~PianoEngine();
    void play(int pitch);
    void stop(int pitch);

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *stream, void *data, int32_t frames);
    void onErrorAfterClose(oboe::AudioStream *stream, oboe::Result err);

private:
    void init();
    void deinit();

    oboe::AudioStream *stream;
    bool is16bit;
    std::unique_ptr<float[]> buf16;

    std::list<Tone> tones;

    std::mutex restartLock;
    std::mutex tonesLock;

};

#endif
