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

public class PianoView extends View {

    public int rows, keys, pitch;
    int whiteWidth, whiteHeight, blackWidth, blackHeight;
    Paint whitePaint, greyPaint, blackPaint;

    final int OUTLINE = 2, YPAD = 20;
    final int SAMPLE_RATE = 44100;
    final int MAX_TRACKS = 10;

    int[][] pitches;

    AudioAttributes aa;
    AudioFormat af;
    AudioTrack tracks[];

    public PianoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        rows = 2;
        keys = 7;
        pitch = 60;

        whitePaint = new Paint();
        whitePaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        greyPaint = new Paint();
        greyPaint.setColor(ContextCompat.getColor(getContext(), R.color.grey3));
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

        tracks = new AudioTrack[10];
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth(), height = getHeight();

        whiteWidth = width / keys;
        whiteHeight = height / rows;
        blackWidth = whiteWidth * 2 / 3;
        blackHeight = whiteHeight / 2;

        pitches = new int[rows][keys];

        int p = pitch;
        for (int row = 0; row < rows; ++row) {
            for (int key = 0; key < keys; ++key) {
                pitches[row][key] = p;

                int x = whiteWidth * key, y = whiteHeight * row;
                canvas.drawRect(x, y, x + whiteWidth, y + whiteHeight - YPAD, greyPaint);
                canvas.drawRect(x + OUTLINE, y, x + whiteWidth - OUTLINE,
                        y + whiteHeight - OUTLINE*2 - YPAD, whitePaint);

                if (hasBlackLeft(p)) {
                    canvas.drawRect(x, y, x + blackWidth / 2, y + blackHeight, blackPaint);
                }

                if (hasBlackRight(p)) {
                    canvas.drawRect(x + whiteWidth - blackWidth / 2, y,
                            x + whiteWidth, y + blackHeight, blackPaint);
                    p += 2;
                } else ++p;
            }
        }
    }

    @Override public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() != MotionEvent.ACTION_DOWN) return false;

        int row = (int)(ev.getY() / whiteHeight),
            key = (int)(ev.getX() / whiteWidth),
            p = pitches[row][key];

        if (ev.getY() - row*whiteHeight < blackHeight) {
            // we're high enough to hit a black key - check if we do
            int x = (int)(ev.getX() - key*whiteWidth);
            if (x < blackWidth/2 && hasBlackLeft(p)) --p;
            else if (x > whiteWidth - blackWidth/2 && hasBlackRight(p)) ++p;
        }

        playTone(440 * Math.pow(2, (p - 69) / 12.0));
        return true;
    }

    private void playTone(double freq) {
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

        byte[] buf = genSine(freq);
        AudioTrack at = new AudioTrack.Builder()
            .setAudioAttributes(aa)
            .setAudioFormat(af)
            .setBufferSizeInBytes(buf.length)
            .build();
        at.write(buf, 0, buf.length);
        at.play();

        tracks[pos] = at;
    }

    private byte[] genSine(double freq) {
        int samples = SAMPLE_RATE;
        byte[] buf = new byte[samples*2];
        for (int i = 0; i < samples; ++i) {
            short val = (short)(32767 * Math.sin(2*Math.PI * freq * i / SAMPLE_RATE));
            buf[i*2] = (byte)(val & 0x00ff);
            buf[i*2+1] = (byte)((val & 0xff00) >> 8);
        }
        return buf;
    }

    private boolean hasBlackLeft(int p) { return p % 12 != 5 && p % 12 != 0; }
    private boolean hasBlackRight(int p) { return p % 12 != 4 && p % 12 != 11; }

}
