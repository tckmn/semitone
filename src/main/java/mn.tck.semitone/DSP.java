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

public class DSP {

    public static int fftlen, fftpow;

    static double[] cos;
    static double[] sin;

    public static void init(int bufsize) {
        fftpow = 31 - Integer.numberOfLeadingZeros(bufsize);
        fftlen = 1 << fftpow;

        cos = new double[fftlen/2];
        sin = new double[fftlen/2];
        for (int i = 0; i < fftlen/2; ++i) {
            cos[i] = Math.cos(-2*Math.PI*i/fftlen);
            sin[i] = Math.sin(-2*Math.PI*i/fftlen);
        }
    }

    // in-place Cooley-Tukey algorithm
    private static void fft(double[] re, double[] im) {
        // bit reversal
        {
            int j = 0;
            int n2 = fftlen/2;
            for (int i = 1; i < fftlen-1; ++i) {
                int n1 = n2;
                while (j >= n1) {
                    j -= n1;
                    n1 /= 2;
                }
                j += n1;

                double tmp;
                if (i < j) {
                    tmp = re[i]; re[i] = re[j]; re[j] = tmp;
                    tmp = im[i]; im[i] = im[j]; im[j] = tmp;
                }
            }
        }

        int n1 = 0, n2 = 1;
        for (int i = 0; i < fftpow; ++i) {
            n1 = n2;
            n2 *= 2;
            int a = 0;
            for (int j = 0; j < n1; ++j) {
                for (int k = j; k < fftlen; k += n2) {
                    double tre = cos[a] * re[k+n1] - sin[a] * im[k+n1],
                           tim = sin[a] * re[k+n1] + cos[a] * im[k+n1];
                    re[k+n1] = re[k] - tre;
                    im[k+n1] = im[k] - tim;
                    re[k] += tre;
                    im[k] += tim;
                }
                a += 1 << (fftpow - i - 1);
            }
        }
    }

    // in-place autocorrelation (scaled by N, but that doesn't matter for us)
    private static void autocorr(double[] are) {
        double[] aim = new double[fftlen];
        fft(are, aim);
        for (int i = 0; i < fftlen; ++i) {
            // corr(a, a) = ifft(fft(a) * conj(fft(a)))
            are[i] = are[i] * are[i] + aim[i] * aim[i];
            aim[i] = 0;
        }
        fft(aim, are); // inverse fft
    }

    // get frequency from mic data (buf must have length fftlen)
    public static double freq(double[] buf, int sr) {
        // TODO check buf length
        // TODO clone buf if necessary
        autocorr(buf);
        // look for the maximum value after the first local minimum
        // (only look halfway through cause it's symmetric)
        boolean looking = false;
        double maxval = 0;
        int j = -1; // maxidx
        for (int i = 0; i < fftlen/2; ++i) {
            if (looking) {
                double weighted = buf[i] * 1; // naive weighting
                if (weighted > maxval) {
                    maxval = weighted;
                    j = i;
                }
            } else {
                // looking = buf[i] < buf[i+1];
                looking = buf[i] < 0;
            }
        }
        // TODO handle this better
        if (j == -1) return 440;
        // quadratic interpolation
        double interp = 0.5 * (buf[j-1] - buf[j+1]) / (buf[j-1] - 2*buf[j] + buf[j+1]);
        return (double)sr / (j + interp);
    }

}
