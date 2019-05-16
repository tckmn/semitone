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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

public class CentErrorView extends android.support.v7.widget.AppCompatTextView {

    private Paint centerPaint, linePaint;
    private String cents;
    double error;

    public CentErrorView(Context context, AttributeSet attrs) {
        super(context, attrs);

        centerPaint = new Paint();
        centerPaint.setColor(Color.WHITE);
        centerPaint.setStrokeWidth(1);

        linePaint = new Paint();
        linePaint.setColor(Color.RED);
        linePaint.setStrokeWidth(2);

        cents = context.getResources().getString(R.string.cents);

        error = 0;
    }

    public void setError(double error) {
        this.error = error;
        setText(String.format("%+.2f %s", error*100, cents));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth(), height = getHeight(), middle = width / 2;

        // draw middle indicator
        canvas.drawLine(middle, 0, middle, height/4, centerPaint);
        canvas.drawLine(middle, height*3/4, middle, height, centerPaint);

        // draw error position
        int xpos = middle + (int)(error*width);
        canvas.drawLine(xpos, 0, xpos, height, linePaint);
    }

}
