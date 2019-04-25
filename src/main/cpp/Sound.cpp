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

#include "Sound.h"

#include <oboe/Oboe.h>

extern "C" {
#include <libavformat/avformat.h>
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
}

#define MP3_BLOCKSIZE 1152

#include <android/log.h>
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "semitone", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,   "semitone", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,    "semitone", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,    "semitone", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,   "semitone", __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,   "semitone", __VA_ARGS__)

int read(void *ptr, uint8_t *buf, int bufsize) {
    return AAsset_read((AAsset*)ptr, buf, (size_t)bufsize);
}

int64_t seek(void *ptr, int64_t offset, int whence) {
    // See https://www.ffmpeg.org/doxygen/3.0/avio_8h.html#a427ff2a881637b47ee7d7f9e368be63f
    if (whence == AVSEEK_SIZE) return AAsset_getLength((AAsset*)ptr);
    if (AAsset_seek((AAsset*)ptr, offset, whence) == -1) {
        return -1;
    } else {
        return 0;
    }
}

// TODO check for errors here
Sound::Sound(AAssetManager &am, const char *path, int channels) {
    AAsset *a = AAssetManager_open(&am, path, AASSET_MODE_UNKNOWN);

    // we're guessing it won't be compressed more than 12x
    const long sizeGuess = 12 * AAsset_getLength(a) * sizeof(float);
    uint8_t *decoded = new uint8_t[sizeGuess],
        *buf = reinterpret_cast<uint8_t*>(av_malloc(MP3_BLOCKSIZE));

    // obtain AVIOContext (with deleter)
    std::unique_ptr<AVIOContext, void(*)(AVIOContext*)> ioc {
        nullptr, [](AVIOContext *c) { av_free(c->buffer); avio_context_free(&c); }
    };
    AVIOContext *iocTmp = nullptr;
    iocTmp = avio_alloc_context(buf, MP3_BLOCKSIZE, 0, a, read, nullptr, seek);
    ioc.reset(iocTmp);

    // obtain AVFormatContext (with deleter)
    std::unique_ptr<AVFormatContext, decltype(&avformat_free_context)> fc {
        nullptr, &avformat_free_context
    };
    AVFormatContext *fcTmp = nullptr;
    fcTmp = avformat_alloc_context();
    fcTmp->pb = ioc.get();
    fc.reset(fcTmp);

    // initialize AVFormatContext
    AVFormatContext *fcptr = fc.get();
    avformat_open_input(&fcptr, "", nullptr, nullptr);
    avformat_find_stream_info(fcptr, nullptr);

    // find stream and codec
    AVStream *stream = fc->streams[av_find_best_stream(fc.get(), AVMEDIA_TYPE_AUDIO, -1, -1, nullptr, 0)];
    AVCodec *codec = avcodec_find_decoder(stream->codecpar->codec_id);

    // obtain AVCodecContext (with deleter)
    std::unique_ptr<AVCodecContext, void(*)(AVCodecContext*)> cc {
        nullptr, [](AVCodecContext *c) { avcodec_free_context(&c); }
    };
    cc.reset(avcodec_alloc_context3(codec));

    // initialize AVCodecContext
    avcodec_parameters_to_context(cc.get(), stream->codecpar);
    avcodec_open2(cc.get(), codec, nullptr);

    // initialize software resampler
    SwrContext *swr = swr_alloc();
    av_opt_set_int(swr, "in_channel_count",  stream->codecpar->channels,       0);
    av_opt_set_int(swr, "in_channel_layout", stream->codecpar->channel_layout, 0);
    av_opt_set_int(swr, "in_sample_rate",    stream->codecpar->sample_rate,    0);
    av_opt_set_int(swr, "in_sample_fmt",     stream->codecpar->format,         0);
    av_opt_set_int(swr,        "out_channel_count",  channels,                              0);
    av_opt_set_int(swr,        "out_channel_layout", (1 << channels) - 1,                   0);
    av_opt_set_int(swr,        "out_sample_rate",    oboe::DefaultStreamValues::SampleRate, 0);
    av_opt_set_sample_fmt(swr, "out_sample_fmt",     AV_SAMPLE_FMT_FLT,                     0);
    av_opt_set_int(swr, "force_resampling", 1, 0);
    swr_init(swr);

    // do the actual decoding
    size_t nBytes = 0;
    AVPacket packet;
    av_init_packet(&packet);
    AVFrame *frame = av_frame_alloc();
    while (av_read_frame(fc.get(), &packet) == 0) {
        if (packet.stream_index != stream->index) continue;
        while (packet.size > 0) {
            avcodec_send_packet(cc.get(), &packet);
            avcodec_receive_frame(cc.get(), frame);

            // resample
            int32_t samples = (int32_t) av_rescale_rnd(
                    swr_get_delay(swr, frame->sample_rate) + frame->nb_samples,
                    oboe::DefaultStreamValues::SampleRate,
                    frame->sample_rate,
                    AV_ROUND_UP);
            uint8_t *swrbuf;
            av_samples_alloc(&swrbuf, nullptr, channels, samples, AV_SAMPLE_FMT_FLT, 0);
            int frame_count = swr_convert(swr, &swrbuf, samples, (const uint8_t **) frame->data, frame->nb_samples);
            size_t bytesize = frame_count * sizeof(float) * channels;
            memcpy(decoded + nBytes, swrbuf, bytesize);
            nBytes += bytesize;
            av_freep(&swrbuf);

            packet.size = 0;
            packet.data = nullptr;
        }
    }
    av_frame_free(&frame);

    nSamples = nBytes / sizeof(float);
    data = std::make_unique<float[]>(nSamples);
    memcpy(data.get(), decoded, nBytes);
    delete[] decoded;
    AAsset_close(a);

    offset = 0;
}
