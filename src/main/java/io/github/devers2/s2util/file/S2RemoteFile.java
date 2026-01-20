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

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

import io.github.devers2.s2util.support.S2FileUtil;

/**
 * s2's utilities
 *
 * @author devers2
 * @version 1.0
 * @since 2020. 07. 08.
 */
public class S2RemoteFile implements Serializable {

    private static final long serialVersionUID = -3153072958120884593L;

    /** 확장자 제외 파일명 */
    private String baseName = "";
    /** 파일 확장자 */
    private String extension = "";
    /** 콘텐츠 유형 */
    private String contentType = "";
    /** 파일 용량 */
    private long size = 0;
    /** 파일 최종 수정 시간 */
    private long lastModified = 0;

    public S2RemoteFile(URL url) {
        if (url != null) {
            try {
                var connection = url.openConnection();
                var contentDisposition = connection.getHeaderField("Content-Disposition");
                var fileName = "";

                // contentDisposition = "attachment; filename=baseName.extension;"
                if (contentDisposition != null && contentDisposition.contains("filename=")) {
                    var filenameStart = contentDisposition.indexOf("filename=") + 9;
                    var filenameEnd = contentDisposition.indexOf(";", filenameStart);

                    if (filenameEnd == -1) {
                        // filename= 뒤에 세미콜론이 없으면 끝까지
                        fileName = contentDisposition.substring(filenameStart).trim();
                    } else {
                        // filename= 뒤에 세미콜론이 있으면 세미콜론까지
                        fileName = contentDisposition.substring(filenameStart, filenameEnd).trim();
                    }
                }

                if (fileName != null && !fileName.isBlank()) {
                    baseName = S2FileUtil.getBaseName(fileName);
                    extension = S2FileUtil.getExtension(fileName);
                }

                contentType = connection.getContentType();
                size = connection.getContentLength();
                lastModified = connection.getLastModified();
            } catch (IOException e) {
                baseName = "";
                extension = "";
                contentType = "";
                size = 0;
                lastModified = 0;
            }
        }
    }

    /**
     * 확장자 제외 파일명 getter
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * 파일 확장자 getter
     */
    public String getExtension() {
        return extension;
    }

    /**
     * 확장자 포함 파일명 getter
     */
    public String getName() {
        return baseName + (extension != null && !extension.isBlank() ? "." + extension : "");
    }

    /**
     * 콘텐츠 유형 getter
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 파일 용량 getter
     */
    public long getSize() {
        return size;
    }

    /**
     * 파일 최종 수정 시간 getter
     */
    public long getLastModified() {
        return lastModified;
    }

}
