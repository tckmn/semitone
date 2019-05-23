package mn.tck.semitone;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

import androidx.core.content.ContextCompat;

public class RecordEngine {

    final static int SAMPLE_RATE = 44100;

    static int bufsize;
    static AudioRecord ar;
    static Thread recordThread;

    static Callback cb;

    static boolean paused = true, created = false;

    public static void create(Activity a) {
        if (created) return;

        created = ContextCompat.checkSelfPermission(a, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        if (!created) return;

        bufsize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        ar = new AudioRecord(AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufsize);

        DSP.init(bufsize);

        resume();
    }

    public static void destroy() {
        if (!created) return;
        created = false;
        pause();
        ar.release();
    }

    public static void pause() {
        if (paused || !created) return;
        paused = true;
        ar.stop();
        recordThread.interrupt();
        recordThread = null;
    }

    public static void resume() {
        if (!paused || !created) return;
        paused = false;
        ar.startRecording();
        recordThread = new Thread(new RecordThread());
        recordThread.start();
    }

    static class RecordThread implements Runnable {
        @Override public void run() {
            short[] buf = new short[bufsize];
            while (!Thread.interrupted()) {
                ar.read(buf, 0, bufsize);
                if (cb != null) cb.onRecordUpdate(buf);
            }
        }
    }

    public interface Callback {
        public void onRecordUpdate(short[] buf);
    }

}
