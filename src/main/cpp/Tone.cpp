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
