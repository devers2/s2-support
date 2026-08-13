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
package io.github.devers2.s2util.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import io.github.devers2.s2util.core.S2Util;
import io.github.devers2.s2util.exception.S2RuntimeException;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;
import net.jpountz.xxhash.XXHashFactory;

/**
 * s2's Encryption utilities
 *
 * @author devers2
 * @version 1.0
 * @since 2025. 04. 09.
 */
public class S2HashUtil {

    private static final S2Logger logger = S2LogManager.getLogger(S2HashUtil.class);

    private static final int ITERATION_COUNT = 65536; // 반복 횟수
    private static final int KEY_LENGTH = 256; // 출력 길이 (비트)
    private static final int SALT_LENGTH = 16; // 솔트 길이 (바이트)
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final String HASH_ALGORITHM_SHA256 = "SHA-256";
    private static final String HASH_ALGORITHM_SHA512_256 = "SHA-512/256";
    private static final String HASH_ALGORITHM_SHA512 = "SHA-512";

    private static XXHashFactory xxHashFactory;

    /**
     * 주어진 텍스트를 단방향으로 해시 생성.
     *
     * @param text 해시할 원본 텍스트 (예: 비밀번호)
     * @return Base64로 인코딩된 Salt 와 해시값이 결합된 문자열
     * @throws NoSuchAlgorithmException 요청된 해시 알고리즘 또는 키 파생 알고리즘이 현재 환경에서 지원되지 않는 경우
     * @throws InvalidKeySpecException  제공된 키 스펙이 해당 키 파생 알고리즘에 유효하지 않은 경우
     * @details
     *          <dl>
     *          <dd>Salt 를 생성하여 보안성을 높이고, Base64로 인코딩된 해시값을 반환.</dd>
     *          <dd>비밀번호 암호화, 높은 보안 요구사항이 있을때는 BCrypt 또는 Argon2 사용을 검토 해야함</dd>
     *          </dl>
     */
    public static String hash(String text) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // 랜덤 솔트 생성
        var salt = generateSalt();

        // PBKDF2로 해시 생성
        var hash = pbkdf2(text, salt);

        // 솔트와 해시 결합
        var combined = new byte[salt.length + hash.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(hash, 0, combined, salt.length, hash.length);

        // Base64로 인코딩하여 반환
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * 저장된 해시값과 입력된 텍스트를 비교하여 일치 여부를 확인.
     *
     * @param text       확인할 원본 텍스트 (예: 입력된 비밀번호)
     * @param storedHash 저장된 Base64 인코딩된 해시값 (Salt + Hash)
     * @return 해시값이 일치하면 true, 아니면 false
     * @throws NoSuchAlgorithmException 요청된 해시 알고리즘 또는 키 파생 알고리즘이 현재 환경에서 지원되지 않는 경우
     * @throws InvalidKeySpecException  제공된 키 스펙이 해당 키 파생 알고리즘에 유효하지 않은 경우
     * @details
     *          <dl>
     *          <dd>암호화된 비밀번호와 입력된 비밀번호 비교, 높은 보안 요구사항이 있을때는 BCrypt 또는 Argon2 사용을 검토 해야함</dd>
     *          </dl>
     */
    public static boolean verify(String text, String storedHash) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (storedHash == null || storedHash.isBlank()) {
            return false;
        }

        // 저장된 해시값 디코딩 (형식이 잘못된 Base64 는 불일치로 처리)
        byte[] combined;
        try {
            combined = Base64.getDecoder().decode(storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }

        // Salt 를 분리할 수 없을 만큼 짧은 값도 불일치로 처리 (NegativeArraySizeException 방지)
        if (combined.length <= SALT_LENGTH) {
            return false;
        }

        // 솔트와 해시 분리
        var salt = new byte[SALT_LENGTH];
        var storedHashBytes = new byte[combined.length - salt.length];
        System.arraycopy(combined, 0, salt, 0, salt.length);
        System.arraycopy(combined, salt.length, storedHashBytes, 0, storedHashBytes.length);

        // 입력된 텍스트로 새 해시 생성
        var newHash = pbkdf2(text, salt);

        // 저장된 해시와 새 해시 비교
        return slowEquals(storedHashBytes, newHash);
    }

    /**
     * PBKDF2 알고리즘을 사용하여 텍스트를 해시로 변환.
     *
     * @param text 해시할 텍스트
     * @param salt 사용할 솔트
     * @return 생성된 해시 바이트 배열
     * @throws NoSuchAlgorithmException 요청된 해시 알고리즘(PBKDF2)이 현재 환경에서 지원되지 않는 경우
     * @throws InvalidKeySpecException  제공된 키 스펙(비밀번호, 솔트, 반복 횟수, 키 길이)이 PBKDF2 알고리즘에 유효하지 않은 경우
     */
    private static byte[] pbkdf2(String text, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        var spec = new PBEKeySpec(
                text.toCharArray(),
                salt,
                ITERATION_COUNT,
                KEY_LENGTH
        );
        var factory = SecretKeyFactory.getInstance(ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }

    /**
     * 암호학적으로 안전한 랜덤 솔트를 생성.
     *
     * @return 생성된 솔트 바이트 배열 (길이: 16바이트)
     */
    private static byte[] generateSalt() {
        var salt = new byte[SALT_LENGTH];
        var random = new SecureRandom();
        random.nextBytes(salt);
        return salt;
    }

    /**
     * 두 바이트 배열을 시간 차 공격(Timing Attack)에 취약하지 않게 비교.
     *
     * @param a 비교할 첫 번째 바이트 배열
     * @param b 비교할 두 번째 바이트 배열
     * @return 두 배열이 같으면 true, 다르면 false
     */
    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a.length != b.length)
            return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    /**
     * SHA-512 해시를 사용하여 파일을 512비트 해시로 변환한다.
     *
     * @param input Path
     * @return 128자리 고정 문자열(64바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>64비트 시스템에서는 SHA-256 보다 효율적이며 보안이 최우선인 경우 적합. 디지털 서명, 고도의 보안 환경에 사용</dd>
     *          </dl>
     */
    public static String generateSHA512(Path input) {
        return generateHash(HASH_ALGORITHM_SHA512, input);
    }

    /**
     * SHA-512 해시를 사용하여 InputStream 을 512비트 해시로 변환한다.
     *
     * @param input 입력 스트림
     * @return 128자리 고정 문자열(64바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>64비트 시스템에서는 SHA-256 보다 효율적이며 보안이 최우선인 경우 적합. 디지털 서명, 고도의 보안 환경에 사용</dd>
     *          <dd>주의!: 이 메서드는 InputStream 을 닫지 않으므로 호출자가 닫아야 함</dd>
     *          </dl>
     */
    public static String generateSHA512(InputStream input) {
        return generateHash(HASH_ALGORITHM_SHA512, input, false);
    }

    /**
     * SHA-512 해시를 사용하여 InputStream 을 512비트 해시로 변환한다.
     *
     * @param input             입력 스트림
     * @param shouldCloseStream sourceStream 을 닫을지 여부
     * @return 128자리 고정 문자열(64바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>64비트 시스템에서는 SHA-256 보다 효율적이며 보안이 최우선인 경우 적합. 디지털 서명, 고도의 보안 환경에 사용</dd>
     *          </dl>
     */
    public static String generateSHA512(InputStream input, boolean shouldCloseStream) {
        return generateHash(HASH_ALGORITHM_SHA512, input, shouldCloseStream);
    }

    /**
     * SHA-512 해시를 사용하여 문자열을 512비트 해시로 변환한다.
     *
     * @param input 문자열
     * @return 128자리 고정 문자열(64바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>64비트 시스템에서는 SHA-256 보다 효율적이며 보안이 최우선인 경우 적합. 디지털 서명, 고도의 보안 환경에 사용</dd>
     *          </dl>
     */
    public static String generateSHA512(String input) {
        return generateHash(HASH_ALGORITHM_SHA512, input);
    }

    /**
     * SHA-512 해시를 사용하여 바이트 배열을 512비트 해시로 변환한다.
     *
     * @param input 바이트 배열
     * @return 128자리 고정 문자열(64바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>64비트 시스템에서는 SHA-256 보다 효율적이며 보안이 최우선인 경우 적합. 디지털 서명, 고도의 보안 환경에 사용</dd>
     *          </dl>
     */
    public static String generateSHA512(byte[] input) {
        return generateHash(HASH_ALGORITHM_SHA512, input);
    }

    /**
     * SHA-512/256 해시를 사용하여 파일을 256비트 해시로 변환한다.
     *
     * @param input Path
     * @return 64자리 고정 문자열(32바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>64비트 시스템에서는 SHA-256 보다 효율적이며 SHA-256과 동일한 출력 크기(256비트)를 제공하는 대체 알고리즘. 디지털 서명, 고도의 보안/성능이 중요한 환경에서 사용</dd>
     *          </dl>
     */
    public static String generateSHA512To256(Path input) {
        return generateHash(HASH_ALGORITHM_SHA512_256, input);
    }

    /**
     * SHA-512/256 해시를 사용하여 InputStream 을 256비트 해시로 변환한다.
     *
     * @param input 입력 스트림
     * @return 64자리 고정 문자열(32바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>64비트 시스템에서는 SHA-256 보다 효율적이며 SHA-256과 동일한 출력 크기(256비트)를 제공하는 대체 알고리즘. 디지털 서명, 고도의 보안/성능이 중요한 환경에서 사용</dd>
     *          <dd>주의!: 이 메서드는 InputStream 을 닫지 않으므로 호출자가 닫아야 함</dd>
     *          </dl>
     */
    public static String generateSHA512To256(InputStream input) {
        return generateHash(HASH_ALGORITHM_SHA512_256, input, false);
    }

    /**
     * SHA-512/256 해시를 사용하여 InputStream 을 256비트 해시로 변환한다.
     *
     * @param input             입력 스트림
     * @param shouldCloseStream sourceStream 을 닫을지 여부
     * @return 64자리 고정 문자열(32바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>64비트 시스템에서는 SHA-256 보다 효율적이며 SHA-256과 동일한 출력 크기(256비트)를 제공하는 대체 알고리즘. 디지털 서명, 고도의 보안/성능이 중요한 환경에서 사용</dd>
     *          </dl>
     */
    public static String generateSHA512To256(InputStream input, boolean shouldCloseStream) {
        return generateHash(HASH_ALGORITHM_SHA512_256, input, shouldCloseStream);
    }

    /**
     * SHA-512/256 해시를 사용하여 문자열을 256비트 해시로 변환한다.
     *
     * @param input 문자열
     * @return 64자리 고정 문자열(32바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>64비트 시스템에서는 SHA-256 보다 효율적이며 SHA-256과 동일한 출력 크기(256비트)를 제공하는 대체 알고리즘. 디지털 서명, 고도의 보안/성능이 중요한 환경에서 사용</dd>
     *          </dl>
     */
    public static String generateSHA512To256(String input) {
        return generateHash(HASH_ALGORITHM_SHA512_256, input);
    }

    /**
     * SHA-512/256 해시를 사용하여 바이트 배열을 256비트 해시로 변환한다.
     *
     * @param input 바이트 배열
     * @return 64자리 고정 문자열(32바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>64비트 시스템에서는 SHA-256 보다 효율적이며 SHA-256과 동일한 출력 크기(256비트)를 제공하는 대체 알고리즘. 디지털 서명, 고도의 보안/성능이 중요한 환경에서 사용</dd>
     *          </dl>
     */
    public static String generateSHA512To256(byte[] input) {
        return generateHash(HASH_ALGORITHM_SHA512_256, input);
    }

    /**
     * SHA-256 해시를 사용하여 파일을 256비트 해시로 변환한다.
     *
     * @param input Path
     * @return 64자리 고정 문자열(32바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>32비트 시스템으로 높은 호환성을 가지고 있음. XXHash64 에 비해 속도는 느리나 디지털 서명, 블록체인 등 보안성이 중요한 경우에 사용</dd>
     *          </dl>
     */
    public static String generateSHA256(Path input) {
        return generateHash(HASH_ALGORITHM_SHA256, input);
    }

    /**
     * SHA-256 해시를 사용하여 InputStream 을 256비트 해시로 변환한다.
     *
     * @param input 입력 스트림
     * @return 64자리 고정 문자열(32바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>32비트 시스템으로 높은 호환성을 가지고 있음. XXHash64 에 비해 속도는 느리나 디지털 서명, 블록체인 등 보안성이 중요한 경우에 사용</dd>
     *          <dd>주의!: 이 메서드는 InputStream 을 닫지 않으므로 호출자가 닫아야 함</dd>
     *          </dl>
     */
    public static String generateSHA256(InputStream input) {
        return generateHash(HASH_ALGORITHM_SHA256, input, false);
    }

    /**
     * SHA-256 해시를 사용하여 InputStream 을 256비트 해시로 변환한다.
     *
     * @param input             입력 스트림
     * @param shouldCloseStream sourceStream 을 닫을지 여부
     * @return 64자리 고정 문자열(32바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>32비트 시스템으로 높은 호환성을 가지고 있음. XXHash64 에 비해 속도는 느리나 디지털 서명, 블록체인 등 보안성이 중요한 경우에 사용</dd>
     *          </dl>
     */
    public static String generateSHA256(InputStream input, boolean shouldCloseStream) {
        return generateHash(HASH_ALGORITHM_SHA256, input, shouldCloseStream);
    }

    /**
     * SHA-256 해시를 사용하여 문자열을 256비트 해시로 변환한다.
     *
     * @param input 문자열
     * @return 64자리 고정 문자열(32바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>32비트 시스템으로 높은 호환성을 가지고 있음. XXHash64 에 비해 속도는 느리나 디지털 서명, 블록체인 등 보안성이 중요한 경우에 사용</dd>
     *          </dl>
     */
    public static String generateSHA256(String input) {
        return generateHash(HASH_ALGORITHM_SHA256, input);
    }

    /**
     * SHA-256 해시를 사용하여 바이트 배열을 256비트 해시로 변환한다.
     *
     * @param input 바이트 배열
     * @return 64자리 고정 문자열(32바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>32비트 시스템으로 높은 호환성을 가지고 있음. XXHash64 에 비해 속도는 느리나 디지털 서명, 블록체인 등 보안성이 중요한 경우에 사용</dd>
     *          </dl>
     */
    public static String generateSHA256(byte[] input) {
        return generateHash(HASH_ALGORITHM_SHA256, input);
    }

    /**
     * 지정된 알고리즘으로 파일을 해시로 변환하는 공통 구현.
     * generateSHA256/generateSHA512/generateSHA512To256(Path) 가 공유한다.
     */
    private static String generateHash(String algorithm, Path input) {
        if (input == null || !Files.exists(input) || !Files.isRegularFile(input) || !Files.isReadable(input)) {
            return "";
        }

        try (InputStream inputStream = S2StreamUtil.getBufferedInputStream(Files.newInputStream(input))) {
            return generateHashFromStream(algorithm, inputStream, S2StreamUtil.getBufferSize(Files.size(input)));
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("{} Hash 오류 발생 [Path]: {}", algorithm, input, e);
            throw new S2RuntimeException(algorithm + " Hash 오류 발생 [Path]");
        }
    }

    /**
     * 지정된 알고리즘으로 InputStream 을 해시로 변환하는 공통 구현.
     * generateSHA256/generateSHA512/generateSHA512To256(InputStream, boolean) 이 공유한다.
     */
    private static String generateHash(String algorithm, InputStream input, boolean shouldCloseStream) {
        if (input == null) {
            return "";
        }

        try {
            return generateHashFromStream(algorithm, input, null);
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("{} Hash 오류 발생 [InputStream]", algorithm, e);
            throw new S2RuntimeException(algorithm + " Hash 오류 발생 [InputStream]");
        } finally {
            if (shouldCloseStream) {
                S2StreamUtil.closeStream(input);
            }
        }
    }

    /**
     * 지정된 알고리즘으로 문자열을 해시로 변환하는 공통 구현.
     * generateSHA256/generateSHA512/generateSHA512To256(String) 이 공유한다.
     */
    private static String generateHash(String algorithm, String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
            return generateHashFromStream(algorithm, inputStream, null);
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("{} Hash 오류 발생 [문자열 변환]", algorithm, e);
            throw new S2RuntimeException(algorithm + " Hash 오류 발생 [문자열 변환]");
        }
    }

    /**
     * 지정된 알고리즘으로 바이트 배열을 해시로 변환하는 공통 구현.
     * generateSHA256/generateSHA512/generateSHA512To256(byte[]) 이 공유한다.
     */
    private static String generateHash(String algorithm, byte[] input) {
        if (input == null) {
            return "";
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(input)) {
            return generateHashFromStream(algorithm, inputStream, null);
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("{} Hash 오류 발생 [바이트 배열]", algorithm, e);
            throw new S2RuntimeException(algorithm + " Hash 오류 발생 [바이트 배열]");
        }
    }

    private static String generateHashFromStream(String algorithm, InputStream input, Integer bufferSize) throws IOException, NoSuchAlgorithmException {
        var md = MessageDigest.getInstance(algorithm);
        var buffer = new byte[bufferSize != null && bufferSize > S2StreamUtil.getBufferSize() ? bufferSize : S2StreamUtil.getBufferSize()];
        int bytesRead;

        while ((bytesRead = input.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }

        var hash = md.digest();
        return bytesToHex(hash);
    }

    private static String bytesToHex(byte[] hash) {
        return java.util.HexFormat.of().formatHex(hash);
    }

    /**
     * XXHash64 해시를 사용하여 파일을 64비트 해시로 변환한다.
     *
     * @param seed  해시 시드 값
     * @param input 해시할 파일
     * @return 16자리 고정 문자열(8바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>비암호화 해시 함수로 빠르고 효율적이며 보안성이 중요하지 않을때 사용</dd>
     *          </dl>
     */
    public static String generateXXHash64(long seed, Path input) {
        if (input == null || !Files.exists(input) || !Files.isRegularFile(input) || !Files.isReadable(input)) {
            logger.debug("Invalid input file");
            return "";
        }

        try (InputStream inputStream = S2StreamUtil.getBufferedInputStream(Files.newInputStream(input))) {
            return generateXXHash64FromStream(seed, S2StreamUtil.getBufferSize(Files.size(input)), false, inputStream);
        } catch (IOException e) {
            logger.error("XXHash64 Hash 오류 발생 [Path]", e);
            throw new S2RuntimeException("XXHash64 Hash 오류 발생 [Path]");
        }
    }

    /**
     * XXHash64 해시를 사용하여 InputStream 을 64비트 해시로 변환한다.
     *
     * @param seed   해시 시드 값
     * @param inputs 해시할 InputStream 배열 (가변 인자)
     * @return 16자리 고정 문자열(8바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>비암호화 해시 함수로 빠르고 효율적이며 보안성이 중요하지 않을때 사용</dd>
     *          <dd>주의!: 이 메서드는 InputStream 을 닫지 않으므로 호출자가 닫아야 함</dd>
     *          </dl>
     */
    public static String generateXXHash64(long seed, InputStream... inputs) {
        return generateXXHash64(seed, false, inputs);
    }

    /**
     * XXHash64 해시를 사용하여 InputStream 을 64비트 해시로 변환한다.
     *
     * @param seed              해시 시드 값
     * @param shouldCloseStream sourceStream 을 닫을지 여부
     * @param inputs            해시할 InputStream 배열 (가변 인자)
     * @return 16자리 고정 문자열(8바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>비암호화 해시 함수로 빠르고 효율적이며 보안성이 중요하지 않을때 사용</dd>
     *          </dl>
     */
    public static String generateXXHash64(long seed, boolean shouldCloseStream, InputStream... inputs) {
        if (S2Util.isEmpty(inputs)) {
            return "";
        }

        try {
            return generateXXHash64FromStream(seed, null, shouldCloseStream, inputs);
        } catch (IOException e) {
            logger.error("XXHash64 Hash 오류 발생 [InputStream]", e);
            throw new S2RuntimeException("XXHash64 Hash 오류 발생 [InputStream]");
        }
    }

    /**
     * XXHash64 해시를 사용하여 문자열을 64비트 해시로 변환한다.
     *
     * @param seed  해시 시드 값
     * @param input 해시할 문자열
     * @return 16자리 고정 문자열(8바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>비암호화 해시 함수로 빠르고 효율적이며 보안성이 중요하지 않을때 사용</dd>
     *          </dl>
     */
    public static String generateXXHash64(long seed, String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        try (var inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
            return generateXXHash64FromStream(seed, null, false, inputStream);
        } catch (IOException e) {
            logger.error("XXHash64 Hash 오류 발생 [문자열 변환]", e);
            throw new S2RuntimeException("XXHash64 Hash 오류 발생 [문자열 변환]");
        }
    }

    /**
     * XXHash64 해시를 사용하여 바이트 배열을 64비트 해시로 변환한다.
     *
     * @param seed  해시 시드 값
     * @param input 해시할 바이트 배열
     * @return 16자리 고정 문자열(8바이트, 16진수)
     * @details
     *          <dl>
     *          <dd>비암호화 해시 함수로 빠르고 효율적이며 보안성이 중요하지 않을때 사용</dd>
     *          </dl>
     */
    public static String generateXXHash64(long seed, byte[] input) {
        if (input == null) {
            return "";
        }

        try (var inputStream = new ByteArrayInputStream(input)) {
            return generateXXHash64FromStream(seed, null, false, inputStream);
        } catch (IOException e) {
            logger.error("XXHash64 Hash 오류 발생 [바이트 배열]", e);
            throw new S2RuntimeException("XXHash64 Hash 오류 발생 [바이트 배열]");
        }
    }

    /**
     * XXHash64 해시를 사용하여 InputStream 을 64비트 해시로 변환하는 공통 메서드.
     *
     * @param seed              해시 시드 값
     * @param bufferSize        버퍼 크기
     * @param shouldCloseStream sourceStream 을 닫을지 여부
     * @param inputs            InputStream 배열 (가변 인자)
     * @return 16자리 고정 문자열(8바이트, 16진수)
     * @throws IOException 스트림 읽기 오류 발생 시
     * @details
     *          <dl>
     *          <dd>비암호화 해시 함수로 빠르고 효율적이며 보안성이 중요하지 않을때 사용</dd>
     *          </dl>
     */
    private static String generateXXHash64FromStream(long seed, Integer bufferSize, boolean shouldCloseStream, InputStream... inputs) throws IOException {
        if (S2Util.isEmpty(inputs)) {
            return "";
        }

        synchronized (XXHashFactoryLOCK) {
            if (xxHashFactory == null) {
                try {
                    // JNI 모드를 우선으로 XXHashFactory 초기화
                    xxHashFactory = XXHashFactory.fastestInstance();
                } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
                    logger.debug("XXHashFactory JNI mode failed, falling back to Java mode: {}", e.getMessage());
                    xxHashFactory = XXHashFactory.fastestJavaInstance();
                }
            }
        }

        try (var hash64 = xxHashFactory.newStreamingHash64(seed)) {
            var buffer = new byte[bufferSize != null && bufferSize > S2StreamUtil.getBufferSize() ? bufferSize : S2StreamUtil.getBufferSize()];

            for (var input : inputs) {
                if (input == null)
                    continue;

                try {
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        hash64.update(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    logger.error("스트림 읽기 중 오류 발생", e);
                    throw e;
                } finally {
                    if (shouldCloseStream) {
                        S2StreamUtil.closeStream(input);
                    }
                }
            }

            return String.format("%016x", hash64.getValue());
        }

    }

    private static final Object XXHashFactoryLOCK = new Object();

}
