/**
 * S2 Support Library
 *
 * Copyright 2020 - 2026 devers2 (이승수, Daejeon, Korea)
 * Contact: eseungsu.dev@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For more information, please see the LICENSE file in the root directory.
 */
package io.github.devers2.s2util.file.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import io.github.devers2.s2util.exception.S2RuntimeException;
import io.github.devers2.s2util.file.FileManager;
import io.github.devers2.s2util.file.JschSessionFactory;
import io.github.devers2.s2util.file.S2ResourceInputStream;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;
import io.github.devers2.s2util.support.S2FileUtil;
import io.github.devers2.s2util.support.S2StreamUtil;

/**
 * s2's utilities
 * SFTP 파일 전송 서비스 파일 업로드, 다운로드, 삭제 및 디렉토리 생성 기능을 제공
 * (단, 엔터프라이즈 환경에서 안정성, 모니터링, 대용량 처리가 중요하다면 spring-integration-sftp 사용을 고려할 필요가 있음)
 *
 * @author devers2
 * @version 1.0
 * @since 2025. 02. 01.
 */
public class S2SftpFileManagerImpl implements FileManager {

    private static final S2Logger logger = S2LogManager.getLogger(S2SftpFileManagerImpl.class);

    // 디렉토리 생성 캐시: 키 → 마지막 기록 시각(ms)
    private final ConcurrentMap<String, Long> createdDirs = new ConcurrentHashMap<>();
    // TTL 기본값 (5분). 시스템 프로퍼티로 오버라이드 가능: -Ds2.sftp.dirCacheTtlMs=60000
    private static final long DEFAULT_DIR_CACHE_TTL_MS = 5 * 60_000L;
    private final long dirCacheTtlMillis;
    // 주기적 정리 트리거 (정비 주기마다 캐시 스윕)
    private final AtomicInteger cacheUpdateCounter = new AtomicInteger();

    private static final int CHANNEL_CONNECT_TIMEOUT = 30_000; // 30초
    /*
     * 다운로드 스트림 idle(마지막 read() 이후 미사용) 자동 정리 시간(밀리초)
     * - 호출 측에서 InputStream.close()를 호출하지 않아도, 마지막 read() 이후 이 시간 동안
     *   추가 읽기가 없으면 자동으로 스트림/채널/세션을 정리합니다.
     * - "생성 후 경과 시간"이 아닌 "마지막 활동 이후 경과 시간" 기준이므로, 대용량 파일을
     *   느린 회선으로 계속 읽고 있는 정상적인 다운로드는 끊기지 않습니다.
     * - 장시간 유출(호출 측이 close()를 잊어버림)로 인한 세션 풀 고갈을 방지하기 위함입니다.
     */
    private static final long MAX_STREAM_IDLE_MILLIS = 5L * 60_000L; // 5분
    private static final long STREAM_IDLE_CHECK_INTERVAL_MILLIS = 30_000L; // 30초마다 idle 여부 점검

    private final JschSessionFactory jschSessionFactory;

    /**
     * @param host           sftp host
     * @param port           sftp port
     * @param username       sftp username
     * @param privateKeyPath sftp private key path
     * @param passphrase     sftp private key passphrase
     * @param password       sftp password
     */
    public S2SftpFileManagerImpl(String host, int port, String username, String privateKeyPath, String passphrase, String password) {
        this(host, port, username, privateKeyPath, passphrase, password, null, null, null);
    }

    /**
     * @param host                 sftp host
     * @param port                 sftp port
     * @param username             sftp username
     * @param privateKeyPath       sftp private key path
     * @param passphrase           sftp private key passphrase
     * @param password             sftp password
     * @param sessionMaxTotal      세션 풀의 최대 세션 수
     * @param sessionMinIdle       세션 풀에 유휴 상태로 유지할 최소 세션 수
     * @param sessionMaxWaitMillis 세션 풀에서 사용 가능한 세션을 기다리는 최대 시간
     */
    public S2SftpFileManagerImpl(String host, int port, String username, String privateKeyPath, String passphrase, String password, Integer sessionMaxTotal, Integer sessionMinIdle, Integer sessionMaxWaitMillis) {
        // 디렉토리 캐시 TTL 우선순위: 시스템 프로퍼티 > 기본값
        long ttl = DEFAULT_DIR_CACHE_TTL_MS;
        try {
            var prop = System.getProperty("s2.sftp.dirCacheTtlMs");
            if (prop != null) {
                ttl = Long.parseLong(prop);
            }
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("s2.sftp.dirCacheTtlMs system property parse failed, using default {}", DEFAULT_DIR_CACHE_TTL_MS);
            }
        }
        this.dirCacheTtlMillis = Math.max(0L, ttl);

        // 파라미터 유효성 검사 및 경고
        if (sessionMaxTotal != null && sessionMaxTotal <= 0) {
            if (logger.isWarnEnabled()) {
                logger.warn("sessionMaxTotal is <= 0, consider using a positive value. Provided: {}", sessionMaxTotal);
            }
        }
        if (sessionMinIdle != null && sessionMinIdle < 0) {
            if (logger.isWarnEnabled()) {
                logger.warn("sessionMinIdle is negative. Provided: {}", sessionMinIdle);
            }
        }
        if (sessionMaxWaitMillis != null && sessionMaxWaitMillis < 0) {
            if (logger.isWarnEnabled()) {
                logger.warn("sessionMaxWaitMillis is negative. Provided: {}", sessionMaxWaitMillis);
            }
        }

        jschSessionFactory = new JschSessionFactory(host, port, username, privateKeyPath, passphrase, password, sessionMaxTotal, sessionMinIdle, sessionMaxWaitMillis);
    }

    /**
     * 원격 여부
     *
     * @return 원격 여부
     */
    public boolean isRemote() {
        return true;
    }

    /**
     * 지정된 경로에 파일을 업로드
     *
     * @param fileData 업로드할 파일 데이터 (InputStream)
     * @param savePath 원격 서버의 대상 파일 저장 경로
     * @param saveName 원격 서버의 대상 파일 저장 명
     * @return 저장된 파일의 크기(바이트 단위), 실패 시 -1
     */
    public long writeFile(InputStream fileData, String savePath, String saveName) {
        var fileSize = -1L;
        var remoteFileFullPath = S2FileUtil.joinPaths(savePath, saveName);
        Session session = null;
        ChannelSftp channel = null;

        // 커스텀 진행률 모니터 인스턴스 생성 (클래스는 static nested로 변경하여 클래스 로딩 비용 절감)
        var monitor = new SizeTrackingMonitor();

        try (InputStream bufferedInput = S2StreamUtil.getBufferedInputStream(fileData)) {
            // 세션/채널은 실제 전송 시간 동안에만 열고, 전송 완료 즉시 finally 블록에서 반환/종료한다.
            // 따라서 일반적인 업로드 흐름에서는 세션이 장시간 홀드되지 않는다.
            session = jschSessionFactory.getSession();
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(CHANNEL_CONNECT_TIMEOUT);

            // 디렉토리 존재 여부 확인 및 생성 (TTL 캐시로 네트워크 호출 최소화)
            if (!isDirCached(savePath)) {
                try {
                    createDirectoryIfNotExists(channel, savePath);
                    putDirCache(savePath);
                } catch (Exception e) {
                    if (logger.isErrorEnabled()) {
                        logger.error("원격 디렉토리 생성 실패: {}", savePath, e);
                    } else {
                        logger.error("원격 디렉토리 생성 실패: {}", savePath);
                    }
                    throw new S2RuntimeException("원격 디렉토리 생성 실패: " + savePath);
                }
            }

            // 기존 파일 존재 여부 확인
            if (exists(channel, remoteFileFullPath)) {
                throw new S2RuntimeException("이미 존재하는 원격 파일입니다: " + remoteFileFullPath);
            }

            // 스트림 직접 전송 및 크기 추적
            channel.put(bufferedInput, remoteFileFullPath, monitor);
            var transferSize = monitor.getTotalBytesTransferred();

            var attrs = channel.stat(remoteFileFullPath);
            fileSize = attrs.getSize(); // 전송 완료된 실제 파일의 크기

            if (transferSize != fileSize) {
                if (logger.isWarnEnabled()) {
                    logger.warn("업로드 전송량과 원격 파일 크기가 다릅니다. 전송: {}, 실제: {}", transferSize, fileSize);
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Failed to upload file: {}", saveName, e);
            } else {
                logger.error("Failed to upload file: {}", saveName);
            }
            throw new S2RuntimeException("Failed to upload file");
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null) {
                jschSessionFactory.returnSession(session);
            }
        }

        return fileSize;
    }

    /**
     * 지정된 경로의 파일을 다운로드
     *
     * @param savePath 원격 서버의 대상 파일 저장 경로
     * @param saveName 원격 서버의 대상 파일 저장 명
     * @return 파일 내용을 담은 InputStream
     * @apiNote 반환된 InputStream 은 SFTP 세션 풀 리소스를 물고 있으므로 사용 후 반드시 close() 해야 한다
     *          (try-with-resources 권장). 호출 측이 닫지 않더라도 마지막 read() 이후
     *          {@code MAX_STREAM_IDLE_MILLIS} 동안 추가 읽기가 없으면 자동으로 정리된다.
     */
    public InputStream readFile(String savePath, String saveName) {
        var remoteFileFullPath = S2FileUtil.joinPaths(savePath, saveName);
        Session session = null;
        ChannelSftp channel = null;

        try {
            // 세션/채널은 스트림을 반환하기 직전까지 열리며, 호출 측이 스트림을 닫을 때 함께 정리된다.
            // 호출 측이 스트림을 닫지 않는 누수 케이스를 대비해 AutoClosingResourceInputStream이
            // 마지막 read() 이후 MAX_STREAM_IDLE_MILLIS 동안 idle 상태면 자동으로 정리한다.
            session = jschSessionFactory.getSession();
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(CHANNEL_CONNECT_TIMEOUT);

            if (!exists(channel, remoteFileFullPath)) {
                throw new S2RuntimeException("원격 파일을 찾을 수 없습니다: " + remoteFileFullPath);
            }

            var rawInputStream = channel.get(remoteFileFullPath);
            var bufferedInputStream = S2StreamUtil.getBufferedInputStream(rawInputStream);

            ChannelSftp finalChannel = channel;
            Session finalSession = session;

            return new AutoClosingResourceInputStream(bufferedInputStream, rawInputStream, finalChannel, finalSession, MAX_STREAM_IDLE_MILLIS, STREAM_IDLE_CHECK_INTERVAL_MILLIS, jschSessionFactory);
        } catch (Exception e) {
            // 예외 발생 시 리소스 정리
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null) {
                jschSessionFactory.returnSession(session);
            }
            logger.error("원격 파일 다운로드 스트림 열기 실패: {}", saveName, e);
            throw new S2RuntimeException("원격 파일 다운로드 스트림 열기에 실패하였습니다.");
        }
    }

    /**
     * 지정된 경로의 파일을 삭제합니다.
     *
     * @param savePath 원격 서버의 대상 파일 저장 경로
     * @param saveName 원격 서버의 대상 파일 저장 명
     */
    public void deleteFile(String savePath, String saveName) {
        var remoteFileFullPath = S2FileUtil.joinPaths(savePath, saveName);
        Session session = null;
        ChannelSftp channel = null;

        try {
            session = jschSessionFactory.getSession();
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(CHANNEL_CONNECT_TIMEOUT);

            if (exists(channel, remoteFileFullPath)) {
                channel.rm(remoteFileFullPath);
            }
        } catch (Exception e) {
            logger.error("원격 파일 삭제 실패: {}", saveName, e);
            throw new S2RuntimeException("원격 파일 삭제 실패: " + e.getMessage());
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect(); // 채널 종료
            }
            jschSessionFactory.returnSession(session);
        }
    }

    private boolean exists(ChannelSftp channel, String path) throws Exception {
        try {
            channel.lstat(path);
            return true;
        } catch (SftpException e) {
            // 파일/디렉토리가 실제로 없는 경우에만 false. 그 외(네트워크/권한 등) 오류는 그대로 전파한다.
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            }
            throw e;
        }
    }

    /**
     * 지정된 경로의 디렉토리가 존재하지 않을 경우 생성합니다. 중간 경로의 디렉토리들도 필요한 경우 함께 생성됩니다.
     *
     * @param channel SFTP 채널 객체
     * @param path    생성할 디렉토리 경로
     */
    private void createDirectoryIfNotExists(ChannelSftp channel, String path) throws Exception {
        var folders = path.split("/");
        var fullPath = new StringBuilder();

        for (var folder : folders) {
            if (folder.isBlank())
                continue;

            fullPath.append("/").append(folder);
            // lstat 으로 존재 여부만 확인한다. cd()는 채널의 현재 작업 디렉토리를 바꾸는 부작용이 있어
            // 이후 상대경로 기반의 exists()/put() 호출이 엉뚱한 위치를 가리키게 될 수 있으므로 사용하지 않는다.
            if (!exists(channel, fullPath.toString())) {
                channel.mkdir(fullPath.toString());
            }
        }
    }

    // SizeTrackingMonitor를 static nested 클래스로 이동(로딩 비용 절감)
    private static class SizeTrackingMonitor implements SftpProgressMonitor {
        private long totalBytesTransferred = 0L;

        @Override
        public void init(int op, String src, String dest, long max) {
        }

        @Override
        public boolean count(long bytesTransferred) {
            totalBytesTransferred += bytesTransferred;
            return true;
        }

        @Override
        public void end() {
        }

        long getTotalBytesTransferred() {
            return totalBytesTransferred;
        }
    }

    /*
     * 다운로드 스트림이 호출 측에서 닫히지 않아도 일정 시간 idle 상태가 지속되면 자동으로 닫히도록 하는 래퍼
     * - 마지막 read() 이후 경과 시간(idle time) 기준이므로, 계속 읽고 있는 정상적인 대용량/저속
     *   다운로드는 끊기지 않고 진짜로 방치된 스트림만 정리 대상이 된다.
     * - 세션/채널 누수 방지 목적
     * - Java 8 호환을 위해 java.util.Timer 사용
     */
    private static class AutoClosingResourceInputStream extends S2ResourceInputStream {
        private final ChannelSftp channel;
        private final Session session;
        private final JschSessionFactory sessionFactory;
        private final Timer timer;
        private final TimerTask task;
        private final long idleTimeoutMillis;
        private volatile long lastActivityAtMillis;

        AutoClosingResourceInputStream(InputStream buffered, InputStream raw, ChannelSftp channel, Session session, long idleTimeoutMillis, long idleCheckIntervalMillis, JschSessionFactory sessionFactory) {
            super(buffered, raw);
            this.channel = channel;
            this.session = session;
            this.sessionFactory = sessionFactory;
            this.idleTimeoutMillis = idleTimeoutMillis;
            this.lastActivityAtMillis = System.currentTimeMillis();
            this.timer = new Timer("s2-sftp-autoclose", true);
            this.task = new TimerTask() {
                @Override
                public void run() {
                    if (System.currentTimeMillis() - lastActivityAtMillis >= idleTimeoutMillis) {
                        try {
                            AutoClosingResourceInputStream.this.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            };
            try {
                var interval = Math.max(1L, idleCheckIntervalMillis);
                this.timer.scheduleAtFixedRate(this.task, interval, interval);
            } catch (Exception ignored) {
            }
        }

        private void markActivity() {
            this.lastActivityAtMillis = System.currentTimeMillis();
        }

        @Override
        public int read() throws IOException {
            var result = super.read();
            markActivity();
            return result;
        }

        @Override
        public int read(byte[] b) throws IOException {
            var result = super.read(b);
            markActivity();
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            var result = super.read(b, off, len);
            markActivity();
            return result;
        }

        @Override
        public long skip(long n) throws IOException {
            var result = super.skip(n);
            markActivity();
            return result;
        }

        @Override
        public void close() {
            if (!this.isClosed()) {
                try {
                    super.close();
                } finally {
                    try {
                        if (task != null)
                            task.cancel();
                    } catch (Exception ignored) {
                    }
                    try {
                        if (timer != null)
                            timer.cancel();
                    } catch (Exception ignored) {
                    }
                    try {
                        if (channel != null && channel.isConnected()) {
                            channel.disconnect();
                        }
                    } catch (Exception ignored) {
                    }
                    try {
                        if (sessionFactory != null && session != null) {
                            sessionFactory.returnSession(session);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    // dir cache 유틸리티
    private boolean isDirCached(String path) {
        if (path == null || path.isBlank())
            return false;
        var ts = createdDirs.get(path);
        if (ts == null)
            return false;
        if (dirCacheTtlMillis == 0L)
            return true; // 무제한
        if (System.currentTimeMillis() - ts > dirCacheTtlMillis) {
            createdDirs.remove(path, ts);
            return false;
        }
        return true;
    }

    private void putDirCache(String path) {
        if (path == null || path.isBlank())
            return;
        createdDirs.put(path, System.currentTimeMillis());
        // 주기적 정리: 256번째 업데이트마다 만료 항목을 스윕
        var count = cacheUpdateCounter.incrementAndGet();
        if ((count & 0xFF) == 0) {
            var now = System.currentTimeMillis();
            var ttl = dirCacheTtlMillis;
            if (ttl > 0) {
                createdDirs.entrySet().removeIf(e -> now - e.getValue() > ttl);
            }
        }
    }

}
