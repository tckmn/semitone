package mn.tck.semitone;

import android.content.Context;
import android.content.res.TypedArray;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.util.AttributeSet;
import android.view.View;
import android.view.KeyEvent;

public class NumBox extends FrameLayout {

    View view;
    EditText valueView;
    int value, minVal, maxVal;
    Callback cb;

    public NumBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        view = inflate(context, R.layout.numbox, null);
        valueView = (EditText) view.findViewById(R.id.value);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.NumBox, 0, 0);
        try {
            value = a.getInteger(R.styleable.NumBox_value, 0);
            minVal = a.getInteger(R.styleable.NumBox_minVal, 0);
            maxVal = a.getInteger(R.styleable.NumBox_maxVal, 0);
            ((TextView)view.findViewById(R.id.label)).setText(a.getString(R.styleable.NumBox_label));
            valueView.setText(""+value);
        } finally {
            a.recycle();
        }

        ((TextView)view.findViewById(R.id.minus)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (value > minVal) {
                    valueView.setText(""+(--value));
                    if (cb != null) cb.onChange(value);
                }
            }
        });

        ((TextView)view.findViewById(R.id.plus)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (value < maxVal) {
                    valueView.setText(""+(++value));
                    if (cb != null) cb.onChange(value);
                }
            }
        });

        valueView.setOnKeyListener(new View.OnKeyListener() {
            @Override public boolean onKey(View v, int keyCode, KeyEvent ev) {
                if (ev.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
                    try {
                        value = Integer.parseInt(valueView.getText().toString());
                        if (value < minVal) value = minVal;
                        if (value > maxVal) value = maxVal;
                        if (cb != null) cb.onChange(value);
                    } catch (NumberFormatException e) {}
                    valueView.setText(""+value);
                }
                return false;
            }
        });

        addView(view);
    }

    public void setValue(int val) {
        value = val;
        valueView.setText(""+val);
    }

    public interface Callback {
        public void onChange(int value);
    }

}
