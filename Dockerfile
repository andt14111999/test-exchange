FROM --platform=linux/amd64 ruby:3.2.2-alpine as BaseImage

LABEL maintainer="trathailoi@gmail.com"
# ARG NPM_AUTH_TOKEN
# ENV NPM_AUTH_TOKEN=${NPM_AUTH_TOKEN}
ENV BUNDLER_VERSION=2.3.26
# ENV NODE_VERSION 18.12.1
# ENV NODE_PACKAGE_URL https://unofficial-builds.nodejs.org/download/release/v$NODE_VERSION/node-v$NODE_VERSION-linux-x64-musl.tar.gz

RUN gem install bundler -v $BUNDLER_VERSION

RUN ["apk", "add", "--update", "--no-cache", "build-base", "postgresql-dev", "postgresql-client", "tzdata", "git", "yarn", "gcompat", "automake", "libtool", "autoconf", "libsodium-dev", "imagemagick", "gcc", "musl-dev", "make", "curl-dev", "openssl-dev", "ca-certificates"]
# RUN update-ca-certificates
# RUN wget $NODE_PACKAGE_URL
# RUN mkdir -p /opt && mkdir -p /opt/nodejs
# RUN tar -zxvf *.tar.gz --directory /opt/nodejs --strip-components=1
# RUN rm *.tar.gz
# RUN ln -s /opt/nodejs/bin/node /usr/local/bin/node
# RUN ln -s /opt/nodejs/bin/npm /usr/local/bin/npm

WORKDIR /app

# COPY package.json yarn.lock ./
# RUN ["yarn", "install"]

COPY Gemfile Gemfile.lock ./
# COPY config/master.key config/master.key
ARG BUNDLE_GITHUB__COM
ARG RAILS_ENV="production"
ARG RAILS_MASTER_KEY="28749d9e518d8903fb48911821b3dae1"
ARG BUNDLE_GEM__FURY__IO
ARG RAILS_STAGING_MASTER_KEY="28749d9e518d8903fb48911821b3dae1"
ARG GITHUB_PACKAGE_TOKEN
ARG BUNDLE_RUBYGEMS__PKG__GITHUB__COM
ARG DATABASE_URL
# ARG TIMESCALE_URL
ARG REDIS_URL

RUN ["bundle", "config", "set", "without", "development test"]
RUN ["bundle", "install", "--jobs", "20", "--retry", "4"]

# Copy application code
ADD . /app

# Remove the database migration from here since we'll run it manually
# RUN ["bin/rails", "db:migrate"]

# Remove these lines since we'll handle the master key through environment variables
# COPY config/master.key /app/config/
# RUN chmod 600 /app/config/master.key

#######################################
# Stage: EXECUTABLE

FROM --platform=linux/amd64 ruby:3.2.2-alpine as Executable
RUN ["apk", "add", "--update", "--no-cache", "build-base", "postgresql-dev", "postgresql-client", "tzdata", "git", "yarn", "libsodium-dev", "libjxl", "aom", "imagemagick", "freetype-dev", "libpng-dev", "jpeg-dev", "libjpeg-turbo-dev", "ca-certificates"]
COPY --from=BaseImage /usr/local/bundle /usr/local/bundle
COPY --from=BaseImage /app /app

# ARG NPM_AUTH_TOKEN
# ENV NPM_AUTH_TOKEN=${NPM_AUTH_TOKEN}
ENV LC_ALL "C.UTF-8"
ENV LANG "en_uC.UTF-8"
ENV LANGUAGE "en_US.UTF-8"

ENV RAILS_LOG_TO_STDOUT true
ENV RAILS_SERVE_STATIC_FILES true
ARG RAILS_ENV="production"
ARG RAILS_MASTER_KEY
ARG BUNDLE_GEM__FURY__IO
ARG RAILS_STAGING_MASTER_KEY
ARG GITHUB_PACKAGE_TOKEN
ARG BUNDLE_RUBYGEMS__PKG__GITHUB__COM
ARG DATABASE_URL
# ARG TIMESCALE_URL
ARG REDIS_URL

# ENV NODE_VERSION 18.12.1
# ENV NODE_PACKAGE_URL https://unofficial-builds.nodejs.org/download/release/v$NODE_VERSION/node-v$NODE_VERSION-linux-x64-musl.tar.gz

# RUN update-ca-certificates
# RUN wget $NODE_PACKAGE_URL
# RUN mkdir -p /opt && mkdir -p /opt/nodejs
# RUN tar -zxvf *.tar.gz --directory /opt/nodejs --strip-components=1
# RUN rm *.tar.gz
# RUN ln -s /opt/nodejs/bin/node /usr/local/bin/node
# RUN ln -s /opt/nodejs/bin/npm /usr/local/bin/npm

WORKDIR /app
RUN ["bin/rails", "assets:clobber"]
RUN ["bundle", "exec", "rails", "assets:precompile"]

EXPOSE 3969
