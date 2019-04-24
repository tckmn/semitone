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
        view = inflate(context, R.layout.numbox, this);
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
    }

    public void setValue(int val) {
        value = val;
        valueView.setText(""+val);
    }

    public interface Callback {
        public void onChange(int value);
    }

}
