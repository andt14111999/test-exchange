# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Thêm volume cache Maven repositories
VOLUME /root/.m2

# Step 1: Caching Maven dependencies
RUN echo "===== STEP 1: Caching Maven dependencies ====="
COPY pom.xml .
# Tối ưu lệnh go-offline để chỉ download dependencies chính
RUN mvn -B -T 4 dependency:resolve-plugins dependency:resolve -Dmaven.repo.local=/root/.m2/repository

# Step 2: Building application
RUN echo "===== STEP 2: Building application ====="
COPY src ./src
# Sử dụng -U để update dependencies, bỏ --no-cache vì Maven không hỗ trợ option này
RUN mvn clean package -DskipTests -B -U -T 4

# Run stage
FROM openjdk:17-slim
RUN echo "===== STEP 1: Install RocksDB dependencies ====="

# Install required packages and RocksDB CLI
RUN apt-get update && apt-get install -y \
  libsnappy1v5 \
  liblz4-1 \
  libzstd1 \
  git \
  build-essential \
  libgflags-dev \
  libsnappy-dev \
  zlib1g-dev \
  libbz2-dev \
  liblz4-dev \
  libzstd-dev \
  cmake \
  wget \
  && rm -rf /var/lib/apt/lists/*

# Install prebuilt RocksDB CLI tools (ldb) in release mode
RUN echo "===== STEP 2: Install RocksDB CLI tools ====="
RUN mkdir -p /tmp/rocksdb && \
  cd /tmp/rocksdb && \
  wget -q https://github.com/facebook/rocksdb/archive/refs/tags/v9.7.3.tar.gz && \
  tar xf v9.7.3.tar.gz && \
  cd rocksdb-9.7.3 && \
  DEBUG_LEVEL=0 make -j$(nproc) ldb && \
  cp ./ldb /usr/local/bin/ && \
  ln -s /usr/local/bin/ldb /usr/local/bin/rocksdb_ldb && \
  chmod +x /usr/local/bin/ldb /usr/local/bin/rocksdb_ldb && \
  cd / && \
  rm -rf /tmp/rocksdb


# Step 3: Setting up runtime environment
RUN echo "===== STEP 3: Setting up runtime environment ====="
WORKDIR /app

# Step 4: Copying application artifacts
RUN echo "===== STEP 4: Copying application artifacts ====="
COPY --from=builder /build/target/exchange-engine-*-jar-with-dependencies.jar /app/app.jar

# Step 5: Creating data directories
RUN echo "===== STEP 5: Creating data directories ====="
RUN mkdir -p /app/data/rocksdb/

# Expose the port your application runs on
# EXPOSE 8080

# Step 6: Configuring startup
RUN echo "===== STEP 6: Configuring startup ====="

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
