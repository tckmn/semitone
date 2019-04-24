#include "Tone.h"

#include <oboe/Oboe.h>
#include <math.h>

Tone::Tone(int pitch) : pitch(pitch) {
    phase = 0;
    phaseIncrement = 2*M_PI * (440*powf(2, (pitch-69)/12.0f)) /
        oboe::DefaultStreamValues::SampleRate;
}

bool Tone::operator==(const Tone &t) const {
    return pitch == t.pitch;
}

float Tone::tick() {
    phase += phaseIncrement;
    if (phase > 2*M_PI) phase -= 2*M_PI;
    return phase < M_PI ? 1 : -1;
}
