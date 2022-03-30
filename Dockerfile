FROM openjdk:oraclelinux8

ENV ANDROID_HOME="/usr/local/android-sdk" \
    ANDROID_SDK_ROOT="/usr/local/android-sdk" \
    ANDROID_VERSION=31 \
    ANDROID_BUILD_TOOLS_VERSION="30.0.2" \
    ANDROID_SDK_TOOLS_VERSION="8092744" \

RUN microdnf install -y wget tar unzip libstdc++ findutils python3.9 python3-pip

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
