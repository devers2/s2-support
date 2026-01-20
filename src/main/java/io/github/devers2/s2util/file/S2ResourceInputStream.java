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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.devers2.s2util.support.S2FileUtil;
import io.github.devers2.s2util.support.S2StreamUtil;

/**
 * s2's utilities try-with-resources 구문 사용 가능한 InputStream 전달 객체
 *
 * @author devers2
 * @version 1.0
 * @since 2020. 07. 08.
 * @details
 *          <dl>
 *          <dd>InputStream 과 함께 Closeable 이나 File 객체들을 같이 전달하여 InputStream 이 close 될때 해당 객체들도 close 되거나 삭제 되어 메모리 누수를 방지한다.</dd>
 *          </dl>
 */
public class S2ResourceInputStream extends InputStream { // AutoCloseable

    private final InputStream delegate;
    private List<Closeable> closeableList;
    private List<Path> tempFileList;
    private boolean closed = false;

    public S2ResourceInputStream(InputStream inputStream) {
        this.delegate = inputStream;
    }

    public S2ResourceInputStream(InputStream inputStream, Closeable... closeables) {
        this.delegate = inputStream;
        this.setCloseable(closeables);
    }

    public S2ResourceInputStream(InputStream inputStream, Path... tempFiles) {
        this.delegate = inputStream;
        this.setTempFile(tempFiles);
    }

    public void setCloseable(Closeable... closeables) {
        if (closeables != null) {
            if (this.closeableList == null) {
                this.closeableList = new ArrayList<>();
            }
            Collections.addAll(this.closeableList, closeables);
        }
    }

    public void setTempFile(Path... tempFiles) {
        if (tempFiles != null) {
            if (this.tempFileList == null) {
                this.tempFileList = new ArrayList<>();
            }
            Collections.addAll(this.tempFileList, tempFiles);
        }
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public int read() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        return delegate.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        return delegate.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        return delegate.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        return delegate.available();
    }

    @Override
    public void close() {
        if (!closed) {
            // InputStream 이 close 될때 같이 전달된 자원들도 모두 close 시킨다.
            closed = true;
            S2StreamUtil.closeStream(delegate);
            if (closeableList != null) {
                for (var closeable : closeableList) {
                    S2StreamUtil.closeStream(closeable);
                }
            }
            if (tempFileList != null) {
                for (var tempFile : tempFileList) {
                    S2FileUtil.delete(tempFile);
                }
            }
        }
    }

    @Override
    public void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }
}
