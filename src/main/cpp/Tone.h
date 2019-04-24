#ifndef __TONE_H__
#define __TONE_H__

#include <list>

class Tone {
public:
    Tone(int pitch);
    float tick();
    bool operator==(const Tone&) const;

private:
    int pitch;
    float phase, phaseIncrement;
};

#endif
