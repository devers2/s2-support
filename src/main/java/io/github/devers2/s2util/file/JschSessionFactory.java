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
package io.github.devers2.s2util.file;

import java.time.Duration;
import java.util.NoSuchElementException;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * s2's utilities
 *
 * @author devers2
 * @version 1.0
 * @since 2020. 07. 08.
 */
public class JschSessionFactory {

    private static final S2Logger logger = S2LogManager.getLogger(JschSessionFactory.class);

    /*
     * 세션 풀 유지/정비 관련 상수
     * - EVICTION_RUN_INTERVAL_MILLIS: 유휴 객체 정비 주기 (밀리초)
     * - IDLE_EVICT_MILLIS: 사용하지 않은 유휴 세션의 최대 보관 시간 (밀리초)
     * - ABANDONED_TIMEOUT_SECONDS: 반납되지 않은(대여 상태로 방치된) 세션 강제 회수 시간 (초)
     * (예: 다운로드 InputStream 미정리 등으로 반납되지 않은 세션 회수)
     */
    private static final long EVICTION_RUN_INTERVAL_MILLIS = 60_000L; // 1분
    private static final long IDLE_EVICT_MILLIS = 30L * 60_000L; // 30분
    private static final int ABANDONED_TIMEOUT_SECONDS = 30 * 60; // 30분

    /* 최대 세션 수 기본값 (64/128/256) */
    private final int DEFAULT_SESSION_MAX_TOTAL = 128;
    /* 최소 유지 세션 수 기본값 (8/16/32) */
    private final int DEFAULT_SESSION_MIN_IDLE = 16;
    /* 세션 획득 대기 시간 기본값 (30초) */
    private final int DEFAULT_SESSION_MAX_WAIT_MILLIS = 30_000;

    private final String host;
    private final int port;
    private final String username;
    /* privateKey 파일 경로 */
    private final String privateKeyPath;
    /* privateKey 비밀번호 (있는 경우) */
    private final String passphrase;
    /* 비밀번호 (privateKeyPath 가 없는 경우 사용) */
    private final String password;

    private GenericObjectPool<Session> sessionPool;

    /* 세션 풀에서 사용 가능한 세션을 기다리는 최대 시간 */
    private final Integer sessionMaxWaitMillis;

    private static final int MAX_RETRY_COUNT = 3;
    private volatile boolean isPoolExhausted = false;

    /**
     * @param host           sftp host
     * @param port           sftp port
     * @param username       sftp username
     * @param privateKeyPath sftp private key path
     * @param passphrase     sftp private key passphrase
     * @param password       sftp password
     */
    public JschSessionFactory(String host, int port, String username, String privateKeyPath, String passphrase, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.privateKeyPath = privateKeyPath;
        this.passphrase = passphrase;
        this.password = password;

        // 세션 풀 설정
        sessionPool = new GenericObjectPool<>(new SessionFactory());
        sessionPool.setMaxTotal(DEFAULT_SESSION_MAX_TOTAL);
        sessionPool.setMinIdle(DEFAULT_SESSION_MIN_IDLE);
        sessionPool.setMaxIdle(sessionPool.getMaxTotal());

        // 세션 풀 검증/정비 설정
        sessionPool.setTestOnBorrow(true);
        sessionPool.setTestOnReturn(true);
        sessionPool.setTestWhileIdle(true);
        sessionPool.setDurationBetweenEvictionRuns(Duration.ofMillis(EVICTION_RUN_INTERVAL_MILLIS));
        sessionPool.setMinEvictableIdleDuration(Duration.ofMillis(IDLE_EVICT_MILLIS));
        sessionPool.setBlockWhenExhausted(true); // 풀이 가득 찼을 때 블록킹
        sessionPool.setTestOnCreate(true); // 객체 생성 시 검증

        // 반납되지 않은(대여 중 방치) 세션 회수 설정
        var abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnMaintenance(true);
        abandonedConfig.setRemoveAbandonedTimeout(Duration.ofSeconds(ABANDONED_TIMEOUT_SECONDS));
        // 빌리는 시점에도 방치(대여 상태) 세션 회수 활성화
        // - ABANDONED_TIMEOUT_SECONDS 초과한 세션만 회수 대상
        // - 장시간 홀드 중인 작업은 강제 종료되어 I/O 실패가 발생할 수 있으므로 운영 모니터링 권장
        abandonedConfig.setRemoveAbandonedOnBorrow(true);
        sessionPool.setAbandonedConfig(abandonedConfig);

        this.sessionMaxWaitMillis = DEFAULT_SESSION_MAX_WAIT_MILLIS;
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
    public JschSessionFactory(String host, int port, String username, String privateKeyPath,
            String passphrase, String password, Integer sessionMaxTotal, Integer sessionMinIdle,
            Integer sessionMaxWaitMillis) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.privateKeyPath = privateKeyPath;
        this.passphrase = passphrase;
        this.password = password;

        // 세션 풀 설정
        sessionPool = new GenericObjectPool<>(new SessionFactory());
        sessionPool.setMaxTotal(sessionMaxTotal != null ? sessionMaxTotal : DEFAULT_SESSION_MAX_TOTAL);
        sessionPool.setMinIdle(sessionMinIdle != null ? sessionMinIdle : DEFAULT_SESSION_MIN_IDLE);
        sessionPool.setMaxIdle(sessionPool.getMaxTotal());

        // 세션 풀 검증/정비 설정
        sessionPool.setTestOnBorrow(true); // 대여 시 검증
        sessionPool.setTestOnReturn(true); // 반환 시 검증
        sessionPool.setTestWhileIdle(true); // 유휴 상태일 때 검증
        sessionPool.setDurationBetweenEvictionRuns(Duration.ofMillis(EVICTION_RUN_INTERVAL_MILLIS)); // 주기적 유휴 검사
        sessionPool.setMinEvictableIdleDuration(Duration.ofMillis(IDLE_EVICT_MILLIS)); // 오래된 유휴 세션 제거
        sessionPool.setBlockWhenExhausted(true); // 풀이 가득 찼을 때 블록킹
        sessionPool.setTestOnCreate(true); // 객체 생성 시 검증

        // 반납되지 않은(대여 중 방치) 세션 회수 설정
        var abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnMaintenance(true);
        abandonedConfig.setRemoveAbandonedTimeout(Duration.ofSeconds(ABANDONED_TIMEOUT_SECONDS));
        // 빌리는 시점에도 방치(대여 상태) 세션 회수 활성화
        // - ABANDONED_TIMEOUT_SECONDS 초과한 세션만 회수 대상
        // - 장시간 홀드 중인 작업은 강제 종료되어 I/O 실패가 발생할 수 있으므로 운영 모니터링 권장
        abandonedConfig.setRemoveAbandonedOnBorrow(true);
        sessionPool.setAbandonedConfig(abandonedConfig);

        this.sessionMaxWaitMillis = sessionMaxWaitMillis != null ? sessionMaxWaitMillis : DEFAULT_SESSION_MAX_WAIT_MILLIS;
    }

    private class SessionFactory implements PooledObjectFactory<Session> {
        @Override
        public boolean validateObject(PooledObject<Session> p) {
            var session = p.getObject();
            if (!session.isConnected()) {
                return false;
            }
            try {
                // 실제 연결 상태 확인을 위한 더 강력한 검증
                session.sendKeepAliveMsg();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public void activateObject(PooledObject<Session> p) throws Exception {
            var session = p.getObject();
            if (!session.isConnected()) {
                session.connect();
            }
        }

        @Override
        public void destroyObject(PooledObject<Session> p) throws Exception {
            var session = p.getObject();
            if (session.isConnected()) {
                session.disconnect();
            }
        }

        @Override
        public PooledObject<Session> makeObject() throws Exception {
            var jsch = new JSch();
            var session = jsch.getSession(username, host, port);
            session.setConfig("ConnectTimeout", String.valueOf(sessionMaxWaitMillis)); // SFTP 연결
            // 타임아웃
            session.setConfig("ServerAliveInterval", "60000"); // 서버 연결 유지 확인 간격 (60초)
            session.setConfig("ServerAliveCountMax", "3"); // 연결 유지 실패 허용 횟수

            if (privateKeyPath != null && !privateKeyPath.isBlank()) {
                // 개인키 설정
                jsch.addIdentity(privateKeyPath, passphrase);
            } else {
                // privateKeyPath 가 없는 경우 password 사용
                session.setPassword(password);
            }

            session.setConfig("StrictHostKeyChecking", "no"); // 운영 환경에서는 적절한 설정 필요
            session.connect();
            return new DefaultPooledObject<>(session);
        }

        @Override
        public void passivateObject(PooledObject<Session> p) throws Exception {
            // 필요한 경우 세션 비활성화 로직 추가
        }
    }

    public Session getSession() throws Exception {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                if (isPoolExhausted || sessionPool.getNumActive() >= sessionPool.getMaxTotal() * 0.9) {
                    logger.warn("세션 풀 상태 위험 - 강제 초기화 진행 (현재 상태: {}})", getPoolStatus());
                    forceResetPool();
                    Thread.sleep(1000); // 풀 초기화 후 잠시 대기
                }

                var session = sessionPool.borrowObject(Duration.ofMillis(sessionMaxWaitMillis));
                if (session != null) {
                    if (!session.isConnected()) {
                        try {
                            session.connect();
                        } catch (Exception e) {
                            invalidateSession(session);
                            throw e;
                        }
                    }
                    isPoolExhausted = false;
                    return session;
                }
            } catch (Exception e) {
                lastException = e;
                logger.error("세션 획득 실패 (시도 {}) - ", (retryCount + 1) + "/" + MAX_RETRY_COUNT, e);

                if (e instanceof NoSuchElementException || e.getMessage().contains("Pool not open")) {
                    isPoolExhausted = true;
                    forceResetPool();
                }
            }
            retryCount++;

            if (retryCount < MAX_RETRY_COUNT) {
                Thread.sleep(1000);
            }
        }

        throw new Exception("최대 재시도 횟수 초과 - 마지막 오류: " + (lastException != null ? lastException.getMessage() : "알 수 없는 오류"));
    }

    private void invalidateSession(Session session) {
        if (session != null) {
            try {
                sessionPool.invalidateObject(session);
            } catch (Exception e) {
                logger.warn("세션 무효화 실패: {}", e.getMessage());
            } finally {
                try {
                    session.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private synchronized void forceResetPool() {
        try {
            logger.info("세션 풀 강제 초기화 시작 - 현재 상태: {}", getPoolStatus());

            // 1. 기존 풀 완전 종료
            try {
                sessionPool.close();
            } catch (Exception e) {
                logger.warn("기존 세션 풀 종료 중 오류: {}", e.getMessage());
            }

            // 2. 새로운 세션 풀 생성
            var factory = new SessionFactory();
            var newPool = new GenericObjectPool<>(factory);

            // 3. 새 풀 설정
            newPool.setMaxTotal(DEFAULT_SESSION_MAX_TOTAL);
            newPool.setMinIdle(DEFAULT_SESSION_MIN_IDLE);
            newPool.setMaxIdle(DEFAULT_SESSION_MAX_TOTAL);
            newPool.setTestOnBorrow(true);
            newPool.setTestOnReturn(true);
            newPool.setTestWhileIdle(true);
            newPool.setDurationBetweenEvictionRuns(Duration.ofMillis(EVICTION_RUN_INTERVAL_MILLIS));
            newPool.setMinEvictableIdleDuration(Duration.ofMillis(IDLE_EVICT_MILLIS));
            newPool.setBlockWhenExhausted(true);
            newPool.setMaxWait(Duration.ofSeconds(30));

            // 반납되지 않은(대여 중 방치) 세션 회수 설정
            var abandonedConfig = new AbandonedConfig();
            abandonedConfig.setRemoveAbandonedOnMaintenance(true);
            abandonedConfig.setRemoveAbandonedTimeout(Duration.ofSeconds(ABANDONED_TIMEOUT_SECONDS));
            // 빌리는 시점에도 방치(대여 상태) 세션 회수 활성화
            // - ABANDONED_TIMEOUT_SECONDS 초과한 세션만 회수 대상
            // - 장시간 홀드 중인 작업은 강제 종료되어 I/O 실패가 발생할 수 있으므로 운영 모니터링 권장
            abandonedConfig.setRemoveAbandonedOnBorrow(true);
            newPool.setAbandonedConfig(abandonedConfig);

            // 4. 기존 풀 참조 교체
            this.sessionPool = newPool;

            // 5. 최소 세션 생성
            try {
                for (var i = 0; i < DEFAULT_SESSION_MIN_IDLE; i++) {
                    var session = this.sessionPool.borrowObject();
                    if (session != null) {
                        this.sessionPool.returnObject(session);
                    }
                }
                isPoolExhausted = false;
            } catch (Exception e) {
                logger.error("초기 세션 생성 실패: {}", e.getMessage(), e);
            }

            logger.info("세션 풀 강제 초기화 완료 - 현재 상태: {}", getPoolStatus());
        } catch (Exception e) {
            logger.error("세션 풀 강제 초기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    public void returnSession(Session session) {
        if (session != null) {
            try {
                if (!session.isConnected()) {
                    invalidateSession(session);
                    return;
                }

                try {
                    session.sendKeepAliveMsg();
                    sessionPool.returnObject(session);
                } catch (Exception e) {
                    logger.warn("세션 반환 실패 - 세션을 무효화합니다: {}", e.getMessage());
                    invalidateSession(session);
                }
            } catch (Exception e) {
                logger.error("세션 반환/무효화 중 오류 발생: {}", e.getMessage(), e);
                try {
                    session.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void close() {
        sessionPool.close(); // 세션 풀만 종료
    }

    public String getPoolStatus() {
        return String.format("Active: %d, Idle: %d, Max: %d", sessionPool.getNumActive(), sessionPool.getNumIdle(), sessionPool.getMaxTotal());
    }

}
