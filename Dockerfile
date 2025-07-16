FROM gradle:8.0-jdk17

USER root

# Install required packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        wget \
        curl \
        unzip \
        ccache \
        apt-utils \
        lib32stdc++6 \
        lib32z1 \
        libgl1-mesa-dev \
        libglu1-mesa && \
    rm -rf /var/lib/apt/lists/*

# Set Android SDK paths
ENV ANDROID_SDK_ROOT=/sdk
ENV ANDROID_HOME=/sdk
ENV ANDROID_SDK_HOME=/sdk
ENV PATH=$PATH:/sdk/cmdline-tools/latest/bin:/sdk/platform-tools:/sdk/emulator

# Download commandline tools
RUN mkdir -p /sdk/cmdline-tools && \
    curl -o sdk.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && \
    unzip sdk.zip -d /sdk/cmdline-tools && \
    mv /sdk/cmdline-tools/cmdline-tools /sdk/cmdline-tools/latest && \
    rm sdk.zip

# Accept licenses
RUN yes | sdkmanager --licenses

# Install Android SDK packages
RUN sdkmanager \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "emulator"

# Copy everything
WORKDIR /app

COPY . .

# Verify gradle
RUN gradle --version

# Build APK
RUN ./gradlew assembleDebug

# Copy built APK to /output
RUN mkdir -p /output && \
    cp app/build/outputs/apk/debug/app-debug.apk /output/app-debug.apk
