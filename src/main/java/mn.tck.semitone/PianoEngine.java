package mn.tck.semitone;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

public class PianoEngine {

    static long handle = 0;

    static {
        System.loadLibrary("semitone-native");
    }

    static boolean create(Context context) {
        if (handle != 0) return true;
        handle = createPianoEngine();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            setSampleRate(Integer.parseInt(am.getProperty(
                            AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)));
            setFramesPerBurst(Integer.parseInt(am.getProperty(
                            AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)));
        }
        return false;
    }

    static void destroy() {
        if (handle == 0) return;
        destroyPianoEngine(handle);
        handle = 0;
    }

    static void play(int pitch) { doPlay(handle, pitch); }
    static void stop(int pitch) { doStop(handle, pitch); }

    private static native long createPianoEngine();
    private static native void destroyPianoEngine(long handle);
    private static native void setSampleRate(int val);
    private static native void setFramesPerBurst(int val);
    private static native void doPlay(long handle, int pitch);
    private static native void doStop(long handle, int pitch);

}
