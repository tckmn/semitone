#!/usr/bin/env bash

set -e
cd lib/ffmpeg

[ -n "$ANDROID_NDK" ] || { echo >&2 no ndk; exit 1; }
if [ -z "$HOST_ARCH" ]
then
    HOST_ARCH="$(find "$ANDROID_NDK/toolchains/llvm/prebuilt" -mindepth 1 -maxdepth 1 -printf '%f\0' | head -zn1 | head -c-1)"
    echo >&2 "using host arch $HOST_ARCH - set \$HOST_ARCH to change"
fi

TOOLCHAIN="$ANDROID_NDK/toolchains/llvm/prebuilt/$HOST_ARCH/bin"
[ -d "$TOOLCHAIN" ] || { echo >&2 no toolchain; exit 1; }

while read ABI ARCH CC_CMD STRIP_CMD FLAGS
do

    [ -f "$TOOLCHAIN/$CC_CMD" ] || { echo >&2 "no cc for $ABI"; exit 1; }
    [ -f "$TOOLCHAIN/$STRIP_CMD" ] || { echo >&2 "no strip for $ABI"; exit 1; }

    ./configure \
        --prefix="build/$ABI" \
        --arch="$ARCH" \
        --cc="$TOOLCHAIN/$CC_CMD" \
        --strip="$TOOLCHAIN/$STRIP_CMD" \
        $FLAGS \
        --target-os=android \
        --enable-cross-compile --enable-small --enable-shared \
        --disable-programs --disable-doc --disable-static --disable-everything \
        --enable-decoder=mp3 --enable-demuxer=mp3

    make clean
    make -j3
    make install

done <<x
armeabi-v7a armv7-a armv7a-linux-androideabi16-clang arm-linux-androideabi-strip
arm64-v8a   aarch64 aarch64-linux-android21-clang    aarch64-linux-android-strip
x86         x86     i686-linux-android16-clang       i686-linux-android-strip    --disable-asm
x86_64      x86_64  x86_64-linux-android21-clang     x86_64-linux-android-strip  --disable-asm
x
