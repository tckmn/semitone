package mn.tck.semitone;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.support.v7.preference.PreferenceManager;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

import java.util.Arrays;

public class TunerFragment extends Fragment {

    final static String[] notenames = {"A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"};

    final static int SAMPLE_RATE = 44100;
    final static int HIST_SIZE = 16;
    int bufsize;
    AudioRecord ar;

    View view;
    TextView notename;
    CentErrorView centerror;

    int concert_a;
    Thread tunerThread;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.tuner, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        this.view = view;

        notename = (TextView) view.findViewById(R.id.notename);
        notename.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                notename.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                notename.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        Util.maxTextSize("G#000", notename.getWidth()));
            }
        });
        centerror = (CentErrorView) view.findViewById(R.id.centerror);

        bufsize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        ar = new AudioRecord(AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufsize);
        ar.startRecording();

        DSP.init(bufsize);

        tunerThread = new Thread(new TunerThread());
        tunerThread.start();

        onSettingsChanged();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        ar.stop();
        tunerThread.interrupt();
    }

    public void onSettingsChanged() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        try {
            concert_a = Integer.parseInt(sp.getString("concert_a", "440"));
        } catch (NumberFormatException e) {
            concert_a = 440;
        }
    }

    class TunerThread implements Runnable {
        @Override public void run() {
            short[] buf = new short[bufsize];
            double[] dbuf = new double[DSP.fftlen];
            double[] hist = new double[HIST_SIZE];
            double[] sorted = new double[HIST_SIZE];
            while (!Thread.interrupted()) {
                // copy data to fft buffer - scale down to avoid huge numbers
                ar.read(buf, 0, bufsize);
                for (int i = 0; i < DSP.fftlen; ++i) dbuf[i] = buf[i] / 1024.0;

                // calculate frequency and note
                double freq = DSP.freq(dbuf, SAMPLE_RATE),
                       semitone = 12 * Math.log(freq/concert_a)/Math.log(2);

                // insert into moving average history
                for (int i = 1; i < HIST_SIZE; ++i) sorted[i-1] = hist[i-1] = hist[i];
                sorted[HIST_SIZE-1] = hist[HIST_SIZE-1] = semitone;

                // find median
                Arrays.sort(sorted);
                final double median = (sorted[HIST_SIZE/2-1]+sorted[HIST_SIZE/2])/2;

                final int rounded = (int)Math.round(median);
                final int note = Math.floorMod(rounded, 12);

                if (getUserVisibleHint()) getActivity().runOnUiThread(new Runnable() {
                    @Override public void run() {
                        notename.setText(notenames[note] +
                            (Math.floorDiv(rounded, 12) + 5 - (note <= 2 ? 1 : 0)));
                        centerror.setError(median - rounded);
                    }
                });
            }
        }
    }

}
