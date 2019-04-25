cmake_minimum_required (VERSION 3.4.1)

add_library (semitone-native SHARED
    src/main/cpp/semitone-native.cpp
    src/main/cpp/PianoEngine.cpp
    src/main/cpp/Tone.cpp
    src/main/cpp/Sound.cpp)

find_library (android-lib android)

add_subdirectory (lib/oboe lib/oboe)
include_directories (lib/oboe/include)

set (FFMPEG_DIR ${PROJECT_SOURCE_DIR}/lib/ffmpeg/build/${ANDROID_ABI})
include_directories (${FFMPEG_DIR}/include)
add_library (avcodec SHARED IMPORTED)
set_target_properties (avcodec PROPERTIES IMPORTED_LOCATION ${FFMPEG_DIR}/lib/libavcodec.so)
add_library (avformat SHARED IMPORTED)
set_target_properties (avformat PROPERTIES IMPORTED_LOCATION ${FFMPEG_DIR}/lib/libavformat.so)
add_library (avutil SHARED IMPORTED)
set_target_properties (avutil PROPERTIES IMPORTED_LOCATION ${FFMPEG_DIR}/lib/libavutil.so)
add_library (swresample SHARED IMPORTED)
set_target_properties (swresample PROPERTIES IMPORTED_LOCATION ${FFMPEG_DIR}/lib/libswresample.so)

target_link_libraries (semitone-native ${android-lib} oboe avcodec avformat avutil swresample)
