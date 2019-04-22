package mn.tck.semitone;

import android.content.Context;
import android.content.res.TypedArray;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.util.AttributeSet;
import android.view.View;

public class NumBox extends FrameLayout {

    View view;
    int value;

    public NumBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        view = inflate(context, R.layout.numbox, null);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.NumBox, 0, 0);
        try {
            value = a.getInteger(R.styleable.NumBox_value, 0);
            ((TextView)view.findViewById(R.id.label)).setText(a.getString(R.styleable.NumBox_label));
            ((TextView)view.findViewById(R.id.value)).setText(""+value);
        } finally {
            a.recycle();
        }

        addView(view);
    }

}
