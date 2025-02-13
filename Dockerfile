FROM openjdk:17-slim-bullseye

ENV ANDROID_HOME="/usr/local/android-sdk" \
    ANDROID_SDK_ROOT="/usr/local/android-sdk" \
    ANDROID_VERSION=33 \
    ANDROID_BUILD_TOOLS_VERSION="33.0.0" \
    ANDROID_SDK_TOOLS_VERSION="9123335"

# install required tools
RUN apt-get --quiet update --yes \
    && apt-get -y install gnupg \
    && apt-get --quiet install --yes wget unzip lib32stdc++6 lib32z1 tar \
    && apt-get --quiet install --yes software-properties-common > /dev/null \
    && apt-add-repository --yes ppa:deadsnakes/ppa > /dev/null \
    && apt-get --quiet install --yes python3.9 > /dev/null \
    && apt-get --quiet install --yes python3-pip > /dev/null

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

RUN mkdir /application

WORKDIR /application
