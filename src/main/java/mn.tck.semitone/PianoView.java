package mn.tck.semitone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.support.v4.content.ContextCompat;

import java.util.HashMap;

public class PianoView extends View {

    public int rows, keys, pitch;
    int whiteWidth, whiteHeight, blackWidth, blackHeight;
    Paint whitePaint, grey1Paint, grey3Paint, grey4Paint, blackPaint;

    final int OUTLINE = 2, YPAD = 20;
    final int SAMPLE_RATE = 44100;
    final int MAX_TRACKS = 10;

    int[][] pitches;
    boolean[] pressed;
    HashMap<Integer, Integer> pointers;

    AudioAttributes aa;
    AudioFormat af;
    AudioTrack tracks[];

    int concert_a;

    public PianoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        rows = 2;
        keys = 7;
        pitch = 48;
        updateParams(false);

        whitePaint = new Paint();
        whitePaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        grey1Paint = new Paint();
        grey1Paint.setColor(ContextCompat.getColor(getContext(), R.color.grey1));
        grey3Paint = new Paint();
        grey3Paint.setColor(ContextCompat.getColor(getContext(), R.color.grey3));
        grey4Paint = new Paint();
        grey4Paint.setColor(ContextCompat.getColor(getContext(), R.color.grey4));
        blackPaint = new Paint();
        blackPaint.setColor(ContextCompat.getColor(getContext(), R.color.black));

        aa = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

        af = new AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build();

        pressed = new boolean[300];
        pointers = new HashMap<Integer, Integer>();
        tracks = new AudioTrack[10];
    }

    public void updateParams(boolean inval) {
        pitches = new int[rows][keys];

        int p = pitch;
        for (int row = 0; row < rows; ++row) {
            for (int key = 0; key < keys; ++key) {
                pitches[row][key] = p;
                p += hasBlackRight(p) ? 2 : 1;
            }
        }

        if (inval) invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth(), height = getHeight();

        whiteWidth = width / keys;
        whiteHeight = height / rows;
        blackWidth = whiteWidth * 2 / 3;
        blackHeight = whiteHeight / 2;

        for (int row = 0; row < rows; ++row) {
            for (int key = 0; key < keys; ++key) {
                int x = whiteWidth * key, y = whiteHeight * row;
                int p = pitches[row][key];

                canvas.drawRect(x, y, x + whiteWidth, y + whiteHeight - YPAD, grey3Paint);
                canvas.drawRect(x + OUTLINE, y, x + whiteWidth - OUTLINE,
                        y + whiteHeight - OUTLINE*2 - YPAD,
                        pressed[p] ? grey4Paint : whitePaint);

                if (hasBlackLeft(p)) canvas.drawRect(
                        x, y,
                        x + blackWidth / 2, y + blackHeight,
                        pressed[p-1] ? grey1Paint : blackPaint);
                if (hasBlackRight(p)) canvas.drawRect(
                        x + whiteWidth - blackWidth / 2, y,
                        x + whiteWidth, y + blackHeight,
                        pressed[p+1] ? grey1Paint : blackPaint);

            }
        }
    }

    @Override public boolean onTouchEvent(MotionEvent ev) {
        int np = ev.getPointerCount();
        int pid, p;

        switch (ev.getActionMasked()) {

        case MotionEvent.ACTION_DOWN:
            pid = ev.getPointerId(0); p = getPitch(ev, 0);
            pointers.put(pid, p);
            playTone(p);
            invalidate();
            return true;

        case MotionEvent.ACTION_POINTER_DOWN:
            pid = ev.getPointerId(ev.getActionIndex()); p = getPitch(ev, ev.getActionIndex());
            pointers.put(pid, p);
            playTone(p);
            invalidate();
            return true;

        case MotionEvent.ACTION_MOVE:
            boolean anyChange = false;
            for (int i = 0; i < np; ++i) {
                pid = ev.getPointerId(i); p = getPitch(ev, i);
                if (pointers.get(pid) != p) {
                    pressed[pointers.get(pid)] = false;
                    pointers.replace(pid, p);
                    anyChange = true;
                    playTone(p);
                }
            }
            if (anyChange) invalidate();
            return true;

        case MotionEvent.ACTION_UP:
            pressed[pointers.remove(ev.getPointerId(0))] = false;
            invalidate();
            return true;

        case MotionEvent.ACTION_POINTER_UP:
            pressed[pointers.remove(ev.getPointerId(ev.getActionIndex()))] = false;
            invalidate();
            return true;

        }

        return false;
    }

    private int getPitch(MotionEvent ev, int pidx) {
        int row = (int)(ev.getY(pidx) / whiteHeight),
            key = (int)(ev.getX(pidx) / whiteWidth),
            p = pitches[row][key];

        if (ev.getY(pidx) - row*whiteHeight < blackHeight) {
            // we're high enough to hit a black key - check if we do
            int x = (int)(ev.getX(pidx) - key*whiteWidth);
            if (x < blackWidth/2 && hasBlackLeft(p)) --p;
            else if (x > whiteWidth - blackWidth/2 && hasBlackRight(p)) ++p;
        }

        return p < 0 || p > 128 ? 0 : p;
    }

    private void playTone(int p) {
        pressed[p] = true;
        double freq = concert_a * Math.pow(2, (p - 69) / 12.0);

        // this will be used as a fallback if all slots are taken,
        // so choose randomly
        int pos = (int)(MAX_TRACKS*Math.random());
        for (int i = 0; i < MAX_TRACKS; ++i) {
            if (tracks[i] == null) pos = i;
            else if (tracks[i].getPlaybackHeadPosition() == tracks[i].getBufferSizeInFrames()) {
                tracks[i].release();
                tracks[i] = null;
                pos = i;
            }
        }

        if (tracks[pos] != null) {
            tracks[pos].stop();
            tracks[pos].release();
        }

        byte[] buf = genSound(SoundType.SHARP, freq,
                0.01, 0.05, 0.4, 0);
        AudioTrack at = new AudioTrack.Builder()
            .setAudioAttributes(aa)
            .setAudioFormat(af)
            .setBufferSizeInBytes(buf.length)
            .build();
        at.write(buf, 0, buf.length);
        at.play();

        tracks[pos] = at;
    }

    enum SoundType { SINE, SOFT, SHARP }
    private byte[] genSound(SoundType st, double freq,
            double attack, double sustain, double release, double noise) {
        int samples = (int)((attack+sustain+release) * SAMPLE_RATE);
        byte[] buf = new byte[samples*2];
        for (int i = 0; i < samples; ++i) {
            double unprocessed = 0;
            switch (st) {
            case SINE: unprocessed = Math.sin(2*Math.PI * freq * i / SAMPLE_RATE); break;
            case SOFT: unprocessed =
                0.500*Math.sin(2*Math.PI * 1*freq * i / SAMPLE_RATE) +
                0.250*Math.sin(2*Math.PI * 2*freq * i / SAMPLE_RATE) +
                0.125*Math.sin(2*Math.PI * 3*freq * i / SAMPLE_RATE) +
                0.050*Math.sin(2*Math.PI * 4*freq * i / SAMPLE_RATE) +
                0.025*Math.sin(2*Math.PI * 5*freq * i / SAMPLE_RATE); break;
            case SHARP: unprocessed =
                0.5*Math.sin(2*Math.PI * 1*freq * i / SAMPLE_RATE) +
                0.4*Math.sin(2*Math.PI * 2*freq * i / SAMPLE_RATE) +
                0.3*Math.sin(2*Math.PI * 3*freq * i / SAMPLE_RATE) +
                0.2*Math.sin(2*Math.PI * 4*freq * i / SAMPLE_RATE) +
                0.1*Math.sin(2*Math.PI * 5*freq * i / SAMPLE_RATE); break;
            }

            // TODO not just linear
            double multiplier = 1;
            if (i <= attack*SAMPLE_RATE) multiplier = i / (attack*SAMPLE_RATE);
            else if (i >= (attack+sustain)*SAMPLE_RATE) multiplier = (samples-i) / (release*SAMPLE_RATE);
            unprocessed *= multiplier;

            // adjust for frequency
            if (freq > 400) unprocessed *= Math.pow(400/freq, 1.3);

            unprocessed += (Math.random()-0.5)*noise;

            final short amplitude = 30000;
            short val = (short)Math.min(amplitude, Math.max(-amplitude,
                        amplitude*unprocessed));
            buf[i*2] = (byte)(val & 0x00ff);
            buf[i*2+1] = (byte)((val & 0xff00) >> 8);
        }
        return buf;
    }

    private boolean hasBlackLeft(int p) { return p % 12 != 5 && p % 12 != 0; }
    private boolean hasBlackRight(int p) { return p % 12 != 4 && p % 12 != 11; }

}
