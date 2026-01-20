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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import io.github.devers2.s2util.exception.S2RuntimeException;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;
import io.github.devers2.s2util.support.S2FileUtil;

/**
 * s2's utilities
 * 파일 관리 유틸리티 클래스 파일 업로드, 다운로드, 파일 정보 확인 및 파일 시스템 관련 기능 제공
 *
 * @author devers2
 * @version 1.0
 * @since 2025. 02. 01.
 */
public interface FileManager {

    S2Logger logger = S2LogManager.getLogger(FileManager.class);

    /**
     * 원격 여부
     *
     * @return 원격 여부
     */
    boolean isRemote();

    /**
     * 파일을 지정된 저장 경로에 작성합니다.
     *
     * @param fileData 저장할 파일 데이터 (InputStream)
     * @param savePath 저장 경로
     * @param saveName 저장할 파일명
     * @return 저장된 파일의 크기(바이트 단위), 실패 시 -1
     */
    long writeFile(InputStream fileData, String savePath, String saveName);

    /**
     * 작성된 파일로부터 데이터를 읽어온다.
     *
     * @param savePath 저장 경로
     * @param saveName 저장 파일명
     * @return 파일 내용을 담은 InputStream
     */
    InputStream readFile(String savePath, String saveName);

    /**
     * 지정된 경로의 파일을 삭제합니다.
     *
     * @param savePath 저장 경로
     * @param saveName 저장 파일명
     */
    void deleteFile(String savePath, String saveName);

    /**
     * 원격 파일 다운로드 (원격 파일을 로컬에 저장)
     *
     * @param fileUrl  파일 URL
     * @param savePath 저장 경로
     * @param saveName 저장 명
     */
    public static S2RemoteFile downloadRemoteFile(String fileUrl, String savePath, String saveName) {
        return downloadRemoteFile(fileUrl, savePath, saveName, null);
    }

    /**
     * 원격 파일 다운로드 (파일 관리 유틸리티를 사용하여 원격 파일을 원격/로컬에 저장)
     *
     * @param fileUrl     파일 URL
     * @param savePath    저장 경로
     * @param saveName    저장 명
     * @param fileManager 파일 관리 유틸리티
     */
    public static S2RemoteFile downloadRemoteFile(String fileUrl, String savePath, String saveName, FileManager fileManager) {
        S2RemoteFile remoteFileInfo = null;

        if (fileUrl != null && !fileUrl.isBlank() && savePath != null && !savePath.isBlank() && saveName != null && !saveName.isBlank()) {
            try {
                var url = new URI(fileUrl).toURL();

                try (var inputStream = url.openStream()) {
                    var writeFileSize = fileManager != null ? fileManager.writeFile(inputStream, savePath, saveName) : S2FileUtil.streamToFile(inputStream, Paths.get(savePath, saveName));
                    if (writeFileSize != -1) {
                        // 원격 파일 정보
                        remoteFileInfo = new S2RemoteFile(url);
                    }
                } catch (FileNotFoundException e) {
                    logger.error("원격 파일을 찾을 수 없습니다.", e);
                    throw new S2RuntimeException("원격 파일을 찾을 수 없습니다.");
                } catch (SocketException e) {
                    logger.error("네트워크 연결이 끊겼습니다.", e);
                    throw new S2RuntimeException("네트워크 연결이 끊겼습니다.");
                } catch (IOException e) {
                    logger.error("원격 파일을 저장할 수 없습니다.", e);
                    throw new S2RuntimeException("원격 파일을 저장할 수 없습니다.");
                }
            } catch (MalformedURLException e) {
                logger.error("원격 파일이 존재하지 않습니다.", e);
                throw new S2RuntimeException("원격 파일이 존재하지 않습니다.");
            } catch (URISyntaxException e) {
                logger.error("유효하지 않은 URL 입니다.", e);
                throw new S2RuntimeException("유효하지 않은 URL 입니다.");
            }
        }

        return remoteFileInfo;
    }

}
