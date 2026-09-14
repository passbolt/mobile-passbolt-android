FROM --platform=linux/amd64 eclipse-temurin:21.0.12_8-jdk-noble@sha256:2fc8306f51cd8c1582b972213f3115300074084ec4f9060448454f04ecbd941b

ENV ANDROID_HOME="/usr/local/android-sdk" \
    ANDROID_SDK_ROOT="/usr/local/android-sdk" \
    ANDROID_VERSION=36 \
    ANDROID_BUILD_TOOLS_VERSION="36.0.0" \
    ANDROID_SDK_TOOLS_VERSION="13114758" \
    DEBIAN_FRONTEND=noninteractive

# wget + unzip: needed for android sdk download
# git + config: needed for gradle lockfiles verification
# jdk 17: gradle compile toolchain (gradle daemon toolchain runs on jdk 21)
# python3: required by gcloud cli
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        wget \
        unzip \
        git \
        openjdk-17-jdk-headless \
        python3 \
    && git config --system --add safe.directory '*' \
    && rm -rf /var/lib/apt/lists/*

# install gcloud cli used by the firebase test lab job
ENV GCLOUD_VERSION="570.0.0" \
    CLOUDSDK_PYTHON="/usr/bin/python3" \
    PATH="/usr/local/google-cloud-sdk/bin:${PATH}"
RUN wget --quiet --output-document=/tmp/gcloud.tar.gz \
        https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-cli-${GCLOUD_VERSION}-linux-x86_64.tar.gz \
    && tar -xzf /tmp/gcloud.tar.gz -C /usr/local \
    && rm /tmp/gcloud.tar.gz \
    && gcloud --version

ENV GH_VERSION="2.97.0" \
    PATH="/usr/local/gh/bin:${PATH}"
RUN wget --quiet --output-document=/tmp/gh.tar.gz \
	https://github.com/cli/cli/releases/download/v${GH_VERSION}/gh_${GH_VERSION}_linux_amd64.tar.gz \
    && mkdir /usr/local/gh \
    && tar -xzf /tmp/gh.tar.gz -C /usr/local/gh --strip-components=1 \
    && rm /tmp/gh.tar.gz \
    && gh --version

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
RUN useradd --create-home --home-dir /application --shell /bin/bash ci-build \
    && chown -R ci-build:ci-build $ANDROID_HOME \
    && passwd -l root

WORKDIR /application

USER ci-build
