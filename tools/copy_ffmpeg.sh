#!/bin/bash

set -e

rm -rf shared
mkdir shared
for ABI in armeabi-v7a arm64-v8a x86 x86_64
do
    mkdir "shared/$ABI"
    cp lib/ffmpeg/build/"$ABI"/lib/lib{avcodec,avformat,avutil,swresample}.so "shared/$ABI"
done
