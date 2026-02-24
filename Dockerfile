FROM --platform=linux/amd64 amazoncorretto:17-alpine

ENV ANDROID_HOME="/usr/local/android-sdk" \
    ANDROID_SDK_ROOT="/usr/local/android-sdk" \
    ANDROID_VERSION=36 \
    ANDROID_BUILD_TOOLS_VERSION="36.0.0" \
    ANDROID_SDK_TOOLS_VERSION="13114758"

# the base image does not have fonts (needed for easy launcher plugin)
RUN apk add --no-cache freetype fontconfig ttf-dejavu

# install required c libraries for aapt2
RUN apk add --no-cache gcompat libstdc++

# setup android home path for moving the downloaded sdk into it
RUN install -d $ANDROID_HOME

# download and extract android sdk tools
RUN wget --quiet --output-document=$ANDROID_HOME/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_TOOLS_VERSION}_latest.zip \
    && unzip $ANDROID_HOME/cmdline-tools.zip -d $ANDROID_HOME \
    && rm $ANDROID_HOME/cmdline-tools.zip \
    && export PATH=$PATH:${ANDROID_HOME}/cmdline-tools/bin \
    && yes | sdkmanager --sdk_root=${ANDROID_HOME} --licenses || true \
    && sdkmanager --sdk_root=${ANDROID_HOME} --update \
    && sdkmanager --sdk_root=${ANDROID_HOME} "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
    && sdkmanager --sdk_root=${ANDROID_HOME} "platforms;android-${ANDROID_VERSION}"

# switch to non-root and lock root
RUN adduser -D -h /application ci-build \
    && chown -R ci-build:ci-build $ANDROID_HOME \
    && passwd -l root

WORKDIR /application

USER ci-build
