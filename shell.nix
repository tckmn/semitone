{ pkgs ? import <nixpkgs> {} }:
with pkgs;
let
  buildToolsVersion = "34.0.0";
  cmakeVersion = "3.22.1";
  androidComposition = androidenv.composeAndroidPackages {
    includeEmulator = false;
    includeSources = false;
    includeSystemImages = false;
    useGoogleAPIs = false;
    useGoogleTVAddOns = false;
    includeNDK = true;
    ndkVersions = ["22.0.7026061"];
    platformVersions = ["34"];
    buildToolsVersions = [buildToolsVersion];
    cmakeVersions = [cmakeVersion];
  };
in mkShell rec {
  ANDROID_SDK_ROOT = "${androidComposition.androidsdk}/libexec/android-sdk";
  ANDROID_NDK_ROOT = "${ANDROID_SDK_ROOT}/ndk-bundle";
  GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${ANDROID_SDK_ROOT}/build-tools/${buildToolsVersion}/aapt2";
  shellHook = ''
    export PATH="$(echo "$ANDROID_SDK_ROOT/cmake/${cmakeVersion}".*/bin):$PATH"
  '';
}
