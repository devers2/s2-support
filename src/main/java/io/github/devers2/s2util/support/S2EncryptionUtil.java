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

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * s2's Encryption utilities
 *
 * @author devers2
 * @version 1.0
 * @since 2025. 04. 09.
 */
public class S2EncryptionUtil {

    private static final S2Logger logger = S2LogManager.getLogger(S2EncryptionUtil.class);

    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;
    private static final int SALT_LENGTH = 16;
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * 주어진 평문을 비밀번호를 사용하여 AES-256으로 암호화 한다.
     *
     * @param plainText 암호화할 원본 텍스트
     * @param password  암호화에 사용할 비밀번호
     * @return Base64로 인코딩된 암호화된 문자열 (Salt + IV + 암호문, 암호화 실패 시 예외 발생)
     * @throws NoSuchAlgorithmException           요청된 암호화 알고리즘이 현재 환경에서 지원되지 않는 경우
     * @throws InvalidKeySpecException            제공된 키 스펙이 해당 키 생성 알고리즘에 유효하지 않은 경우
     * @throws NoSuchPaddingException             요청된 패딩 방식이 현재 환경에서 지원되지 않는 경우
     * @throws InvalidAlgorithmParameterException 암호화 알고리즘 파라미터(여기서는 IV)가 유효하지 않은 경우
     * @throws InvalidKeyException                암호화에 사용될 키가 유효하지 않은 경우 (예: 잘못된 길이, 형식 등)
     * @throws IllegalBlockSizeException          블록 암호화 시 평문의 길이가 블록 크기의 배수가 아니고, 적절한 패딩이 적용되지 않았을 때
     * @throws BadPaddingException                복호화 과정에서 패딩이 올바르지 않거나 손상되었을 때 (암호화 과정에서는 일반적으로 발생하지 않지만, 예외 선언에 포함)
     * @details
     *          <dl>
     *          <dd>Salt 와 IV를 생성하여 보안성을 강화하며, 결과는 Base64로 인코딩된 문자열로 반환.</dd>
     *          <dd>암호화에 실패하면 예외를 전파하여 호출한 측에서 처리하도록 한다.</dd>
     *          </dl>
     */
    public static String encrypt(String plainText, String password) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        var salt = generateSalt();
        var iv = generateIv();
        var key = generateKey(password, salt);

        var cipher = Cipher.getInstance(ALGORITHM);
        var ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

        var encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        var combined = new byte[salt.length + iv.length + encrypted.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(encrypted, 0, combined, salt.length + iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Base64로 인코딩된 암호화된 문자열을 비밀번호를 사용하여 복호화 한다.
     *
     * @param encryptedText 복호화할 Base64로 인코딩된 암호화 문자열
     * @param password      복호화에 사용할 비밀번호
     * @return 복호화된 원본 텍스트 (복호화 실패 시 원문 반환)
     * @details
     *          <dl>
     *          <dd>암호화 시 사용된 Salt 와 IV를 추출하여 원본 텍스트를 복원.</dd>
     *          <dd>복호화에 실패했을때는 예외를 전파하지 않고 원본을 반환한다.</dd>
     *          </dl>
     */
    public static String decrypt(String encryptedText, String password) {
        try {
            var combined = Base64.getDecoder().decode(encryptedText);

            var salt = new byte[SALT_LENGTH];
            var iv = new byte[16];
            var encrypted = new byte[combined.length - salt.length - iv.length];

            System.arraycopy(combined, 0, salt, 0, salt.length);
            System.arraycopy(combined, salt.length, iv, 0, iv.length);
            System.arraycopy(combined, salt.length + iv.length, encrypted, 0, encrypted.length);

            var key = generateKey(password, salt);

            var cipher = Cipher.getInstance(ALGORITHM);
            var ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

            var decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            /**
             * NoSuchAlgorithmException 요청된 암호화/키 생성 알고리즘이 현재 환경에서 지원되지 않는 경우
             * InvalidKeySpecException 제공된 키 스펙이 해당 키 생성 알고리즘에 유효하지 않은 경우 (예: 비밀번호 오류로 인한 키 생성 실패)
             * NoSuchPaddingException 요청된 패딩 방식이 현재 환경에서 지원되지 않는 경우
             * InvalidAlgorithmParameterException 복호화 알고리즘 파라미터(여기서는 IV)가 유효하지 않거나 암호화 시 사용된 IV와 다른 경우
             * InvalidKeyException 복호화에 사용될 키가 유효하지 않은 경우 (예: 잘못된 비밀번호로 생성된 키)
             * IllegalBlockSizeException 복호화하려는 데이터의 길이가 블록 크기의 배수가 아니고, 패딩이 올바르지 않을 때
             * BadPaddingException 복호화 과정에서 패딩이 올바르지 않거나 손상되었을 때 (예: 잘못된 비밀번호 사용)
             */
            logger.error("Decryption failed: ", e);
            return encryptedText;
        }
    }

    /**
     * PBKDF2 알고리즘을 사용하여 비밀번호와 Salt 로부터 AES 암호화 키를 생성합니다.
     *
     * @param password 키 생성에 사용할 비밀번호
     * @param salt     키 생성에 사용할 Salt
     * @return 생성된 AES SecretKey 객체
     * @throws NoSuchAlgorithmException 요청된 키 파생 알고리즘이 현재 환경에서 지원되지 않는 경우
     * @throws InvalidKeySpecException  제공된 키 스펙이 해당 키 파생 알고리즘에 유효하지 않은 경우
     */
    private static SecretKey generateKey(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        var spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATION_COUNT,
                KEY_LENGTH
        );
        var factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        var key = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }

    /**
     * 암호학적으로 안전한 랜덤 Salt 를 생성합니다.
     *
     * @return 생성된 Salt 바이트 배열 (길이: 16바이트)
     */
    private static byte[] generateSalt() {
        var salt = new byte[SALT_LENGTH];
        var random = new SecureRandom();
        random.nextBytes(salt);
        return salt;
    }

    /**
     * AES CBC 모드에서 사용할 랜덤 초기화 벡터(IV)를 생성합니다.
     *
     * @return 생성된 IV 바이트 배열 (길이: 16바이트)
     */
    private static byte[] generateIv() {
        var iv = new byte[16];
        var random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

}
