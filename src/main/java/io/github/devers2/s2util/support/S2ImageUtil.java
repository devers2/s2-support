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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import javax.imageio.ImageIO;

import io.github.devers2.s2util.core.S2StringUtil;
import io.github.devers2.s2util.core.S2Util;
import io.github.devers2.s2util.file.S2File;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * s2's utilities
 *
 * @author devers2
 * @version 1.0
 * @since 2025. 03. 07.
 */
public class S2ImageUtil {

    private static final S2Logger logger = S2LogManager.getLogger(S2ImageUtil.class);

    /**
     * sourceFile 로부터 변경 전 경로 정보를 계산한다.
     * convertImage/imageResize/convertImageExtension 이 공통으로 사용한다.
     */
    private record BeforeImageInfo(String fullPath, Path parent, String fileNameString, String extension) {
    }

    private static BeforeImageInfo resolveBeforeImageInfo(Path sourceFile) {
        var fullPath = S2StringUtil.replaceChars(sourceFile.toString(), "/", '\\');
        var parent = sourceFile.getParent();
        var fileNameString = sourceFile.getFileName().toString();
        var extension = S2FileUtil.getExtension(sourceFile).toLowerCase();
        return new BeforeImageInfo(fullPath, parent, fileNameString, extension);
    }

    /**
     * newFilePath/newFileName 이 없으면 원본 경로/파일명을 사용해 변경 후 전체 경로를 계산한다.
     * convertImage/imageResize 가 동일하게 사용하는 로직이다.
     */
    private static String buildAfterFileFullPath(Path beforeFilePath, String beforeFileNameString, String newFilePath, String newFileName) {
        return S2StringUtil.replaceChars(
                (newFilePath != null && !newFilePath.isBlank() ? newFilePath : beforeFilePath.toString()) + "/" + (newFileName != null && !newFileName.isBlank() ? newFileName : beforeFileNameString),
                "/",
                '\\'
        );
    }

    /**
     * 이미지를 변경한다.(속도 개선을 위해 이미지 리사이즈와 포맷 변경을 동시에 처리)
     *
     * @param sourceFile           기존 파일
     * @param maxWidth             최대 너비(null: 기존 파일 너비 유지)
     * @param maxHeight            최대 높이(null: 기존 파일 높이 유지)
     * @param isFixedRate          가로세로 비율고정 여부(true: 비율 유지, flase: 비율 무시)
     * @param maxSize              최대 파일 용량
     * @param maxSizeOverExtension 최대 파일 용량 초과 시 변환할 확장자
     * @param newFileExtension     변경할 확장자(null: 기존 파일 확장자 유지, not null: 확장자 변경(maxSizeOverExtension 보다
     *                             우선))
     * @param newFilePath          변경할 파일 경로(null: 기존 파일 경로)
     * @param newFileName          변경할 확장자 포함 파일명(null: 기존 확장자 포함 파일명)
     * @param isSourceFileDelete   기존 파일 삭제 여부
     * @return 결과 정보
     */
    public static S2File convertImage(Path sourceFile, Integer maxWidth, Integer maxHeight, boolean isFixedRate, Long maxSize, String maxSizeOverExtension, String newFileExtension, String newFilePath, String newFileName, boolean isSourceFileDelete) {
        Path resultPath = null;
        String resultExtension = "";

        if (sourceFile != null && Files.exists(sourceFile) && Files.isRegularFile(sourceFile)) {
            var before = resolveBeforeImageInfo(sourceFile);
            var beforeFileFullPath = before.fullPath();
            var beforeFileExtension = before.extension();

            var afterFileFullPath = buildAfterFileFullPath(before.parent(), before.fileNameString(), newFilePath, newFileName);
            resultPath = Paths.get(afterFileFullPath);

            try (var newFileInputStream = Files.newInputStream(sourceFile);
                    var resizedOutputStream = new ByteArrayOutputStream()) {

                var newImage = imageResize(newFileInputStream, maxWidth, maxHeight, isFixedRate);

                ImageIO.write(newImage, beforeFileExtension, resizedOutputStream);

                var resizedFileSize = resizedOutputStream.size();
                var afterFileExtension = "";

                if (newFileExtension != null && !newFileExtension.isBlank()) {
                    afterFileExtension = newFileExtension;
                } else if (maxSize != null && maxSize < resizedFileSize && maxSizeOverExtension != null && !maxSizeOverExtension.isBlank()) {
                    afterFileExtension = maxSizeOverExtension;
                }

                var newFileDirectory = resultPath.getParent();
                if (!Files.isDirectory(newFileDirectory)) {
                    Files.createDirectories(newFileDirectory);
                }

                if (afterFileExtension == null || afterFileExtension.isBlank() || beforeFileExtension.equals(afterFileExtension)) {
                    if (!beforeFileFullPath.equalsIgnoreCase(afterFileFullPath) && isSourceFileDelete) {
                        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                            S2FileUtil.delete(sourceFile);
                        }));
                    }
                    Files.write(resultPath, resizedOutputStream.toByteArray());
                } else {
                    int afterFileExtPoint = afterFileFullPath.lastIndexOf(".");
                    if (afterFileExtPoint != -1) {
                        afterFileFullPath = afterFileFullPath.substring(0, afterFileExtPoint) + "." + afterFileExtension;
                        resultPath = Paths.get(afterFileFullPath);
                    }

                    try (var resizedInputStream = new ByteArrayInputStream(resizedOutputStream.toByteArray())) {
                        var beforeImage = ImageIO.read(resizedInputStream);
                        if (beforeImage == null) {
                            throw new IOException("이미지를 읽을 수 없습니다 (지원하지 않는 형식이거나 손상된 파일)");
                        }
                        var afterImage = new BufferedImage(beforeImage.getWidth(), beforeImage.getHeight(), BufferedImage.TYPE_INT_RGB);

                        afterImage.createGraphics().drawImage(beforeImage, 0, 0, Color.white, null);
                        ImageIO.write(afterImage, afterFileExtension, resultPath.toFile());

                        if (!beforeFileFullPath.equalsIgnoreCase(afterFileFullPath) && isSourceFileDelete) {
                            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                                S2FileUtil.delete(sourceFile);
                            }));
                        }
                    }
                }

                resultExtension = S2Util.cast(afterFileExtension, beforeFileExtension);
            } catch (IOException e) {
                resultPath = null;
            }
        }

        return new S2File(resultPath, resultExtension, sourceFile != null ? sourceFile.getFileName().toString() : null);
    }

    /**
     * 이미지 사이즈를 변경한다.
     *
     * @param sourceFile         기존 파일
     * @param maxWidth           최대 너비(null: 기존 파일 너비 유지)
     * @param maxHeight          최대 높이(null: 기존 파일 높이 유지)
     * @param isFixedRate        가로세로 비율고정 여부(true: 비율 유지, flase: 비율 무시)
     * @param newFilePath        변경할 파일 경로(null: 기존 파일 경로)
     * @param newFileName        변경할 확장자 포함 파일명(null: 기존 확장자 포함 파일명)
     * @param isSourceFileDelete 기존 파일 삭제 여부
     * @return 결과 정보
     */
    public static S2File imageResize(Path sourceFile, Integer maxWidth, Integer maxHeight, boolean isFixedRate, String newFilePath, String newFileName, boolean isSourceFileDelete) {
        Path resultFile = null;
        String resultExtension = "";
        String resultOriginalName = "";

        if (sourceFile != null && Files.exists(sourceFile) && Files.isRegularFile(sourceFile)) {
            var before = resolveBeforeImageInfo(sourceFile);
            var beforeFileFullPath = before.fullPath();
            var beforeFileNameString = before.fileNameString();
            var beforeFileExtension = before.extension();

            var afterFileFullPath = buildAfterFileFullPath(before.parent(), before.fileNameString(), newFilePath, newFileName);
            resultFile = Paths.get(afterFileFullPath);

            try (var newFileInputStream = Files.newInputStream(sourceFile)) {
                var newImage = imageResize(newFileInputStream, maxWidth, maxHeight, isFixedRate);

                var resultDirectory = resultFile.getParent();
                if (!Files.isDirectory(resultDirectory)) {
                    Files.createDirectories(resultDirectory);
                }

                ImageIO.write(newImage, beforeFileExtension, resultFile.toFile());

                if (!beforeFileFullPath.equalsIgnoreCase(afterFileFullPath) && isSourceFileDelete) {
                    S2FileUtil.delete(sourceFile);
                }
            } catch (IOException e) {
                resultFile = null;
            }

            resultExtension = S2Util.cast(S2FileUtil.getExtension(resultFile), beforeFileExtension);
            resultOriginalName = beforeFileNameString;
        }

        return new S2File(resultFile, resultExtension, resultOriginalName);
    }

    /**
     * 이미지 사이즈를 변경한다.
     *
     * @param imageInputStream 이미지 InputStream
     * @param maxWidth         최대 너비(null: 기존 파일 너비 유지)
     * @param maxHeight        최대 높이(null: 기존 파일 높이 유지)
     * @param isFixedRate      가로세로 비율고정 여부(true: 비율 유지, flase: 비율 무시)
     * @return 변경된 이미지
     */
    public static BufferedImage imageResize(InputStream imageInputStream, Integer maxWidth, Integer maxHeight, boolean isFixedRate) {
        BufferedImage outputImage;

        try {
            var inputImage = ImageIO.read(imageInputStream);
            if (inputImage == null) {
                throw new IOException("이미지를 읽을 수 없습니다 (지원하지 않는 형식이거나 손상된 파일)");
            }

            int beforeWidth = inputImage.getWidth();
            int beforeHeight = inputImage.getHeight();

            Integer afterWidth = null;
            Integer afterHeight = null;

            if (isFixedRate) {
                if (maxWidth != null && beforeWidth > maxWidth) {
                    afterWidth = maxWidth;
                    afterHeight = (afterWidth * beforeHeight) / beforeWidth; // 비율유지

                    if (maxHeight != null && afterHeight > maxHeight) {
                        afterWidth = (afterWidth * maxHeight) / afterHeight; // 비율유지
                        afterHeight = maxHeight;
                    }
                } else if (maxHeight != null && beforeHeight > maxHeight) {
                    afterWidth = (beforeWidth * maxHeight) / beforeHeight; // 비율유지
                    afterHeight = maxHeight;
                }
            } else {
                if (maxWidth != null && beforeWidth > maxWidth) {
                    afterWidth = maxWidth;
                }
                if (maxHeight != null && beforeHeight > maxHeight) {
                    afterHeight = maxHeight;
                }
            }

            if (afterWidth != null && afterHeight != null) {
                Graphics2D graphics2d = null;
                try {
                    outputImage = new BufferedImage(afterWidth, afterHeight, inputImage.getType());
                    graphics2d = outputImage.createGraphics();
                    graphics2d.drawImage(inputImage, 0, 0, afterWidth, afterHeight, null);
                } finally {
                    if (graphics2d != null) {
                        graphics2d.dispose();
                    }
                }
            } else {
                outputImage = inputImage;
            }

        } catch (IOException e) {
            outputImage = null;
        }

        return outputImage;
    }

    /**
     * 이미지 파일 포맷을 변경한다.
     *
     * @param sourceFile         기존 파일
     * @param newFileExtension   변경할 확장자(null: 기존 파일 확장자 유지)
     * @param newFilePath        변경할 파일 경로(null: 기존 파일 경로)
     * @param newFileName        변경할 확장자 포함 파일명(null: 기존 파일명)
     * @param isSourceFileDelete 기존 파일 삭제 여부
     * @return 결과 정보
     */
    public static S2File convertImageExtension(Path sourceFile, String newFileExtension, String newFilePath, String newFileName, boolean isSourceFileDelete) {
        Path resultFile = null;
        String resultExtension = "";

        if (sourceFile != null && Files.exists(sourceFile) && Files.isRegularFile(sourceFile)) {
            var before = resolveBeforeImageInfo(sourceFile);
            var beforeFileFullPath = before.fullPath();
            var beforeFileExtension = before.extension();

            var afterFileExtension = newFileExtension != null && !newFileExtension.isBlank() ? newFileExtension.toLowerCase() : "";
            var afterFileName = "";

            if (newFileName != null && !newFileName.isBlank()) {
                afterFileName = newFileName;
            } else {
                afterFileName = (beforeFileExtension != null && !beforeFileExtension.isBlank() ? S2FileUtil.getBaseName(sourceFile) : sourceFile.getFileName().toString()) + (afterFileExtension != null && !afterFileExtension.isBlank() ? "." + afterFileExtension : "");
            }

            var afterFileFullPath = S2StringUtil.replaceChars((newFilePath != null && !newFilePath.isBlank() ? newFilePath : before.parent().toString()) + "/" + afterFileName, "/", '\\');

            resultFile = Paths.get(afterFileFullPath);

            if (afterFileExtension == null || afterFileExtension.isBlank() || afterFileExtension.equals(beforeFileExtension)) {
                if (beforeFileFullPath.equalsIgnoreCase(afterFileFullPath)) {
                    resultFile = sourceFile;
                } else {
                    try (var newFileInputStream = Files.newInputStream(sourceFile)) {
                        S2FileUtil.streamToFile(newFileInputStream, resultFile);
                        if (isSourceFileDelete) {
                            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                                S2FileUtil.delete(sourceFile);
                            }));
                        }
                    } catch (IOException e) {
                        resultFile = null;
                    }
                }
            } else {
                try {
                    Files.createDirectories(resultFile.getParent());

                    var beforeImage = ImageIO.read(sourceFile.toFile());
                    if (beforeImage == null) {
                        throw new IOException("이미지를 읽을 수 없습니다 (지원하지 않는 형식이거나 손상된 파일): " + sourceFile);
                    }
                    var afterImage = new BufferedImage(beforeImage.getWidth(), beforeImage.getHeight(), BufferedImage.TYPE_INT_RGB);

                    afterImage.createGraphics().drawImage(beforeImage, 0, 0, Color.white, null);
                    ImageIO.write(afterImage, afterFileExtension, resultFile.toFile());

                    if (isSourceFileDelete) {
                        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                            S2FileUtil.delete(sourceFile);
                        }));
                    }
                } catch (IOException e) {
                    resultFile = null;
                }
            }

            resultExtension = S2Util.cast(afterFileExtension, beforeFileExtension);
        }

        return new S2File(
                resultFile, resultExtension,
                sourceFile != null ? sourceFile.getFileName().toString() : null
        );
    }

    /**
     * 이미지(InputStream)를 Base64로 인코딩한다.
     *
     * @param inputStream InputStream
     * @return Base64 문자열, 에러 발생 시 빈 문자열 반환
     * @details
     *          <dl>
     *          <dd>img 태그에 사용하는 경우 src=`data:image/jpeg;base64,${base64Image 문자열}` 처럼 활용한다.</dd>
     *          </dl>
     */
    public static String encodeImageToBase64(InputStream inputStream) {
        return encodeImageToBase64(inputStream, false);
    }

    /**
     * 이미지(InputStream)를 Base64로 인코딩한다.
     *
     * @param inputStream       InputStream
     * @param shouldCloseStream inputStream 을 닫을지 여부
     * @return Base64 문자열, 에러 발생 시 빈 문자열 반환
     * @details
     *          <dl>
     *          <dd>img 태그에 사용하는 경우 src=`data:image/jpeg;base64,${base64Image 문자열}` 처럼 활용한다.</dd>
     *          </dl>
     */
    public static String encodeImageToBase64(InputStream inputStream, boolean shouldCloseStream) {
        if (inputStream == null) {
            logger.warn("입력 스트림이 null입니다.");
            return "";
        }

        try {
            var bytes = S2StreamUtil.streamToByteArray(inputStream, shouldCloseStream);
            if (bytes == null || bytes.length == 0) {
                logger.warn("스트림으로부터 데이터를 읽을 수 없습니다.");
                return "";
            }
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            logger.error("Base64 인코딩 중 오류 발생: ", e);
            return "";
        }
    }

}
