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

package mn.tck.semitone;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.HashMap;

public class PianoView extends View {

    public int rows, keys, pitch;
    protected int whiteWidth, whiteHeight, blackWidth, blackHeight;
    protected Paint whitePaint, grey1Paint, grey3Paint, grey4Paint, blackPaint;

    protected final int OUTLINE = 2, YPAD = 20;
    final int SAMPLE_RATE = 44100;
    final int MAX_TRACKS = 10;

    protected int[][] pitches;
    protected boolean[] pressed;
    HashMap<Integer, Integer> pointers;

    protected int concert_a;
    protected boolean sustain, labelnotes, labelc;

    public PianoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        rows = sp.getInt("piano_rows", 2);
        keys = sp.getInt("piano_keys", 7);
        pitch = sp.getInt("piano_pitch", 28);

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
        blackPaint.setTextAlign(Paint.Align.CENTER);

        pressed = new boolean[300];
        pointers = new HashMap<Integer, Integer>();
    }

    public void updateParams(boolean inval) {
        pitches = new int[rows][keys];

        int p = 0;
        for (int i = 0; i < pitch; ++i) p += hasBlackRight(p) ? 2 : 1;
        for (int row = 0; row < rows; ++row) {
            for (int key = 0; key < keys; ++key) {
                pitches[row][key] = p;
                p += hasBlackRight(p) ? 2 : 1;
            }
        }


        if (inval) {
            SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            editor.putInt("piano_rows", rows);
            editor.putInt("piano_keys", keys);
            editor.putInt("piano_pitch", pitch);
            editor.apply();

            invalidate();
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth(), height = getHeight();

        whiteWidth = width / keys;
        whiteHeight = height / rows;
        blackWidth = whiteWidth * 2 / 3;
        blackHeight = whiteHeight / 2;
        blackPaint.setTextSize(Util.maxTextSize("G0", whiteWidth * 2/3));

        for (int row = 0; row < rows; ++row) {
            for (int key = 0; key < keys; ++key) {
                int x = whiteWidth * key, y = whiteHeight * row;
                int p = pitches[row][key];

                canvas.drawRect(x, y, x + whiteWidth, y + whiteHeight - YPAD, grey3Paint);
                canvas.drawRect(x + OUTLINE, y, x + whiteWidth - OUTLINE,
                        y + whiteHeight - OUTLINE*2 - YPAD,
                        pressed[p] ? grey4Paint : whitePaint);

                if (labelnotes && (!labelc || p % 12 == 0)) canvas.drawText(
                        Util.notenames[(p+3)%12] + (p/12 - 1),
                        x + whiteWidth/2, y + whiteHeight*4/5, blackPaint);

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
        // we want to be able to swipe on the piano without swiping to a
        // different tab
        getParent().requestDisallowInterceptTouchEvent(true);

        int np = ev.getPointerCount();
        int pid, p;

        switch (ev.getActionMasked()) {

        case MotionEvent.ACTION_DOWN:
            pid = ev.getPointerId(0); p = getPitch(ev, 0);
            pointers.put(pid, p);
            play(p);
            invalidate();
            return true;

        case MotionEvent.ACTION_POINTER_DOWN:
            pid = ev.getPointerId(ev.getActionIndex()); p = getPitch(ev, ev.getActionIndex());
            pointers.put(pid, p);
            play(p);
            invalidate();
            return true;

        case MotionEvent.ACTION_MOVE:
            boolean anyChange = false;
            for (int i = 0; i < np; ++i) {
                pid = ev.getPointerId(i); p = getPitch(ev, i);
                if (pointers.get(pid) != p) {
                    stop(pointers.get(pid));
                    pointers.put(pid, p);
                    play(p);
                    anyChange = true;
                }
            }
            if (anyChange) invalidate();
            return true;

        case MotionEvent.ACTION_UP:
            stop(pointers.remove(ev.getPointerId(0)));
            invalidate();
            return true;

        case MotionEvent.ACTION_POINTER_UP:
            stop(pointers.remove(ev.getPointerId(ev.getActionIndex())));
            invalidate();
            return true;

        }

        return false;
    }

    private int getPitch(MotionEvent ev, int pidx) {
        int row = Math.min((int)(ev.getY(pidx) / whiteHeight), rows-1),
            key = Math.min((int)(ev.getX(pidx) / whiteWidth), keys-1),
            p = pitches[row][key];

        if (ev.getY(pidx) - row*whiteHeight < blackHeight) {
            // we're high enough to hit a black key - check if we do
            int x = (int)(ev.getX(pidx) - key*whiteWidth);
            if (x < blackWidth/2 && hasBlackLeft(p)) --p;
            else if (x > whiteWidth - blackWidth/2 && hasBlackRight(p)) ++p;
        }

        return p < 0 || p > 128 ? 0 : p;
    }

    private void play(int pitch) {
        pressed[pitch] = true;
        if (sustain) PianoEngine.play(pitch, concert_a);
        else PianoEngine.playFile("piano/"+pitch+".mp3", concert_a);
    }

    private void stop(int pitch) {
        pressed[pitch] = false;
        if (sustain) PianoEngine.stop(pitch);
    }

    protected boolean hasBlackLeft(int p) { return p % 12 != 5 && p % 12 != 0; }
    protected boolean hasBlackRight(int p) { return p % 12 != 4 && p % 12 != 11; }

}
