package mn.tck.semitone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.support.v4.content.ContextCompat;

public class PianoView extends View {

    public int rows, keys, pitch;
    Paint whitePaint, greyPaint, blackPaint;

    final int OUTLINE = 2, YPAD = 20;

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
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth(), height = getHeight();

        int whiteWidth = width / keys,
            whiteHeight = height / rows,
            blackWidth = whiteWidth * 2 / 3,
            blackHeight = whiteHeight / 2;

        int p = pitch;
        for (int row = 0; row < rows; ++row) {
            for (int key = 0; key < keys; ++key) {
                int x = whiteWidth * key, y = whiteHeight * row;
                canvas.drawRect(x, y, x + whiteWidth, y + whiteHeight - YPAD, greyPaint);
                canvas.drawRect(x + OUTLINE, y, x + whiteWidth - OUTLINE, y + whiteHeight - OUTLINE*2 - YPAD, whitePaint);

                if (p % 12 != 5 && p % 12 != 0) {
                    // not F or C - black key to the left
                    canvas.drawRect(x, y, x + blackWidth / 2, y + blackHeight, blackPaint);
                }

                if (p % 12 != 4 && p % 12 != 11) {
                    // not E or B - black key to the right
                    canvas.drawRect(x + whiteWidth - blackWidth / 2, y, x + whiteWidth, y + blackHeight, blackPaint);
                    p += 2;
                } else ++p;
            }
        }
    }

}
