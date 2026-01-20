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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import io.github.devers2.s2util.exception.S2RuntimeException;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * s2's utilities
 *
 * @author devers2
 * @version 1.0
 * @since 2025. 03. 07.
 */
public class S2StreamUtil {

    private static final S2Logger logger = S2LogManager.getLogger(S2StreamUtil.class);

    /**
     * 기본 버퍼 크기를 반환한다.
     *
     * @return 버퍼 크기(기본값 32KB)
     */
    public static int getBufferSize() {
        return getBufferSize(null);
    }

    /**
     * 파일 또는 데이터의 예상 크기에 따라 적절한 버퍼 크기를 반환한다.
     *
     * @param targetSize 파일 또는 데이터 사이즈
     * @return 파일 또는 데이터 사이즈별 버퍼 크기
     */
    public static int getBufferSize(Long targetSize) {
        if (targetSize == null) {
            // 기본 버퍼 크기는 32KB 또는 64KB
            return 32 * 1024;
        } else if (targetSize <= 100 * 1024) { // 100KB 이하
            return 8 * 1024;
        } else if (targetSize <= 1024 * 1024) { // 1MB 이하
            return 16 * 1024;
        } else if (targetSize <= 100 * 1024 * 1024) { // 100MB 이하
            return 64 * 1024;
        } else if (targetSize <= 500 * 1024 * 1024) { // 500MB 이하
            return 128 * 1024;
        } else if (targetSize <= 1024 * 1024 * 1024) { // 1GB 이하
            return 256 * 1024;
        } else { // 1GB 초과
            return 512 * 1024; // 또는 1MB 등으로 조정 가능
        }
    }

    /**
     * 데이터 스트림을 닫는다.
     *
     * @param stream 스트림 객체
     */
    public static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                logger.error("Failed to close stream", e);
                stream = null;
            }
        }
    }

    /**
     * InputStream 을 BufferedInputStream 으로 변환한다.
     *
     * @param inputStream 변환할 InputStream
     * @return BufferedInputStream 또는 null (inputStream 이 null 인 경우)
     * @details
     *          <dl>
     *          <dd>이미 BufferedInputStream 인 경우 그대로 반환</dd>
     *          </dl>
     */
    public static BufferedInputStream getBufferedInputStream(InputStream inputStream) {
        return getBufferedInputStream(inputStream, null);
    }

    /**
     * InputStream 을 BufferedInputStream 으로 변환한다.
     *
     * @param inputStream 변환할 InputStream
     * @param bufferSize  버퍼 크기
     * @return BufferedInputStream 또는 null (inputStream 이 null 인 경우)
     * @details
     *          <dl>
     *          <dd>이미 BufferedInputStream 인 경우 그대로 반환</dd>
     *          </dl>
     */
    public static BufferedInputStream getBufferedInputStream(InputStream inputStream, Integer bufferSize) {
        if (inputStream == null) {
            logger.debug("InputStream is null.");
            return null;
        }
        return inputStream instanceof BufferedInputStream
                ? (BufferedInputStream) inputStream
                : bufferSize != null && bufferSize > getBufferSize()
                        ? new BufferedInputStream(inputStream, bufferSize)
                        : new BufferedInputStream(inputStream);
    }

    /**
     * OutputStream 을 BufferedOutputStream 으로 변환한다.
     *
     * @param outputStream 변환할 OutputStream
     * @return BufferedOutputStream 또는 null (outputStream 이 null 인 경우)
     * @details
     *          <dl>
     *          <dd>이미 BufferedOutputStream 인 경우 그대로 반환</dd>
     *          </dl>
     */
    public static BufferedOutputStream getBufferedOutputStream(OutputStream outputStream) {
        return getBufferedOutputStream(outputStream, null);
    }

    /**
     * OutputStream 을 BufferedOutputStream 으로 변환한다.
     *
     * @param outputStream 변환할 OutputStream
     * @param bufferSize   버퍼 크기
     * @return BufferedOutputStream 또는 null (outputStream 이 null 인 경우)
     * @details
     *          <dl>
     *          <dd>이미 BufferedOutputStream 인 경우 그대로 반환</dd>
     *          </dl>
     */
    public static BufferedOutputStream getBufferedOutputStream(OutputStream outputStream, Integer bufferSize) {
        if (outputStream == null) {
            logger.debug("OutputStream is null.");
            return null;
        }
        return outputStream instanceof BufferedOutputStream
                ? (BufferedOutputStream) outputStream
                : bufferSize != null && bufferSize > getBufferSize()
                        ? new BufferedOutputStream(outputStream, bufferSize)
                        : new BufferedOutputStream(outputStream);
    }

    /**
     * InputStream 을 바이트 배열로 변환한다.
     *
     * @param sourceStream      처리할 InputStream
     * @param shouldCloseStream sourceStream 을 닫을지 여부
     * @return 바이트 배열
     */
    public static byte[] streamToByteArray(InputStream sourceStream, boolean shouldCloseStream) {
        if (sourceStream == null) {
            logger.warn("소스 스트림이 null입니다.");
            return null;
        }

        var bufferedInput = getBufferedInputStream(sourceStream);

        try (var outputStream = new ByteArrayOutputStream()) {
            var buffer = new byte[getBufferSize()];
            int bytesRead;
            long totalBytesRead = 0;
            final long maxSize = 50 * 1024 * 1024; // 최대 50MB 제한

            while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                if (totalBytesRead > maxSize) {
                    logger.error("파일 크기가 최대 허용 크기(50MB)를 초과했습니다.");
                    return null;
                }
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            logger.error("바이트 배열 변환 실패: ", e);
            return null;
        } catch (OutOfMemoryError e) {
            logger.error("메모리 부족 오류 발생: ", e);
            return null;
        } finally {
            if (shouldCloseStream) {
                closeStream(bufferedInput);
            }
        }
    }

    /**
     * Reader 을 바이트 배열로 변환한다.
     *
     * @param sourceReader      처리할 Reader
     * @param shouldCloseStream sourceStream 을 닫을지 여부
     * @return 바이트 배열
     */
    public static byte[] streamToByteArray(Reader sourceReader, boolean shouldCloseStream) {
        if (sourceReader == null) {
            logger.warn("소스 리더가 null입니다.");
            return null;
        }

        var bufferedReader = new BufferedReader(sourceReader);

        try (var outputStream = new ByteArrayOutputStream()) {
            var bufferSize = getBufferSize();
            var charBuffer = CharBuffer.allocate(bufferSize);
            var byteBuffer = ByteBuffer.allocate(bufferSize * 4);
            var encoder = StandardCharsets.UTF_8.newEncoder();

            long totalBytesRead = 0;
            final long maxSize = 50 * 1024 * 1024; // 최대 50MB 제한

            while (bufferedReader.read(charBuffer) != -1) {
                charBuffer.flip();
                encoder.encode(charBuffer, byteBuffer, true);
                byteBuffer.flip();

                var bytesRead = byteBuffer.limit();
                totalBytesRead += bytesRead;

                if (totalBytesRead > maxSize) {
                    logger.error("데이터 크기가 최대 허용 크기(50MB)를 초과했습니다.");
                    return null;
                }

                outputStream.write(byteBuffer.array(), 0, bytesRead);
                charBuffer.clear();
                byteBuffer.clear();
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            logger.error("바이트 배열 변환 실패: ", e);
            return null;
        } catch (OutOfMemoryError e) {
            logger.error("메모리 부족 오류 발생: ", e);
            return null;
        } finally {
            if (shouldCloseStream) {
                closeStream(bufferedReader);
            }
        }
    }

    /**
     * 입력 스트림에서 출력 스트림으로 데이터를 청크 단위로 복사하며, 전송된 총 바이트 수를 반환
     *
     * @param inputStream  입력 스트림
     * @param outputStream 출력 스트림
     * @return 전송된 총 바이트 수
     * @throws IOException 입출력 중 오류가 발생한 경우. 예: 스트림이 닫혔거나 네트워크 연결이 끊긴 경우.
     */
    public static long copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        if (inputStream == null || outputStream == null) {
            throw new NullPointerException("inputStream 과 outputStream 은 null 일 수 없습니다.");
        }

        // Java 9+ transferTo 사용 (네이티브 최적화 및 코드 단순화)
        return inputStream.transferTo(outputStream);
    }

    /**
     * InputStream 을 문자열로 변환한다.
     *
     * @param inputStream InputStream
     * @return 문자열
     */
    public static String convertStreamToString(InputStream inputStream) {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[S2StreamUtil.getBufferSize()];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8.name()); // UTF-8 인코딩 사용
        } catch (IOException e) {
            throw new S2RuntimeException("InputStream 문자열 변환 오류 발생");
        }
    }

}
