{
  description = "Android Kotlin/Compose dev shell";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachSystem [ "x86_64-linux" ] (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
          config.android_sdk.accept_license = true;
        };

        androidEnv = pkgs.androidenv.override {
          licenseAccepted = true;
        };

        androidComposition = androidEnv.composeAndroidPackages {
          cmdLineToolsVersion = "8.0";
          platformToolsVersion = "36.0.2";
          buildToolsVersions = [ "35.0.0" "36.0.0" ];
          platformVersions = [ "35" "36" ];
          abiVersions = [ "x86_64" ];
          includeNDK = false;
          includeEmulator = false;
        };

        androidSdk = androidComposition.androidsdk;
      in {
        devShells.default = pkgs.mkShell {
          packages = with pkgs; [
            androidSdk
            gradle
            jdk21
            git
            unzip
          ];

          ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
          ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
          JAVA_HOME = pkgs.jdk21.home;

          GRADLE_OPTS =
            "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidSdk}/libexec/android-sdk/build-tools/35.0.0/aapt2";

          shellHook = ''
            export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
            echo "Android dev shell ready"
            echo "ANDROID_HOME=$ANDROID_HOME"
          '';
        };
      });
}
