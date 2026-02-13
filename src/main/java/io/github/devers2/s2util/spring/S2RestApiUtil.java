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
package io.github.devers2.s2util.spring;

import java.io.InputStream;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import io.github.devers2.s2util.core.S2StringUtil;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * s2's utilities
 * REST API 호출 및 관련 유틸리티 기능을 제공하는 클래스.
 * HTTP 요청을 처리하고, {@link InputStreamResource}를 생성하는 메서드를 포함한다.
 *
 * @author devers2
 * @version 1.0
 * @since 2025. 01. 09.
 */
public class S2RestApiUtil {

    private static final S2Logger logger = S2LogManager.getLogger(S2RestApiUtil.class);

    private static final int DEFAULT_TIMEOUT = 30000;
    private static final RestTemplate defaultRestTemplate;

    static {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(DEFAULT_TIMEOUT);
        factory.setReadTimeout(DEFAULT_TIMEOUT);
        defaultRestTemplate = new RestTemplate(factory);
        defaultRestTemplate.getMessageConverters().addFirst(new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    /**
     * 지정된 URL로 REST API를 호출하여 응답을 문자열로 반환한다.
     * 기본 타임아웃(30,000ms)을 사용하며, HTTP 메서드와 매개변수를 받아 요청을 처리한다.
     *
     * @param url    호출할 REST API의 URL. (필수)
     * @param method 사용할 HTTP 메서드 (예: {@link HttpMethod#POST}, {@link HttpMethod#GET}). (필수)
     * @param params 요청에 포함할 매개변수로, 키-값 쌍의 배열. (선택)
     * @return API 호출 결과로 반환된 문자열 응답. 응답이 없거나 오류 발생 시 null을 반환.
     *
     *         <pre>{@code
     * String result = S2RestApiUtil.callApi("request.api", HttpMethod.POST,
     *     Map.entry("param1", value1),
     *     Map.entry("param2", value2),
     *     Map.entry("files", S2RestApiUtil.createInputStreamResource(fileInputStream, "file1.text", 1213)),
     *     Map.entry("files", S2RestApiUtil.createInputStreamResource(fileInputStream, "file2.pdf", 3121))
     * );
     * }</pre>
     */
    @SafeVarargs
    public static String callApi(String url, HttpMethod method, Map.Entry<String, Object>... params) {
        return S2RestApiUtil.callApi(url, method, null, params);
    }

    /**
     * 지정된 URL로 REST API를 호출하여 응답을 문자열로 반환한다.
     * 사용자 지정 타임아웃을 설정할 수 있으며, HTTP 메서드와 매개변수를 받아 요청을 처리한다.
     *
     * @param url     호출할 REST API의 URL. (필수)
     * @param method  사용할 HTTP 메서드 (예: {@link HttpMethod#POST}, {@link HttpMethod#GET}). (필수)
     * @param timeout 연결 및 읽기 타임아웃(밀리초 단위). null인 경우 기본값 30,000ms 사용. (선택)
     * @param params  요청에 포함할 매개변수로, 키-값 쌍의 배열. (선택)
     * @return API 호출 결과로 반환된 문자열 응답. 응답이 없거나 오류 발생 시 null을 반환.
     * @apiNote
     *
     *          <pre>{@code
     * String result = S2RestApiUtil.callApi("request.api", HttpMethod.POST, 10000,
     *     Map.entry("param1", value1),
     *     Map.entry("param2", value2),
     *     Map.entry("files", S2RestApiUtil.createInputStreamResource(fileInputStream, "file1.text", 1213)),
     *     Map.entry("files", S2RestApiUtil.createInputStreamResource(fileInputStream, "file2.pdf", 3121))
     * );
     * }</pre>
     */
    @SuppressWarnings("null")
    @SafeVarargs
    public static String callApi(String url, HttpMethod method, Integer timeout, Map.Entry<String, Object>... params) {
        int vTimeout = timeout != null && timeout > 0 ? timeout : DEFAULT_TIMEOUT;

        RestTemplate restTemplate;
        if (vTimeout == DEFAULT_TIMEOUT) {
            restTemplate = defaultRestTemplate;
        } else {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(vTimeout);
            requestFactory.setReadTimeout(vTimeout);
            restTemplate = new RestTemplate(requestFactory);
            restTemplate.getMessageConverters().addFirst(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));

        String result = null;
        ResponseEntity<String> responseEntity = null;

        if (method == HttpMethod.POST) {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            if (params != null) {
                for (Map.Entry<String, Object> param : params) {
                    if (param != null) {
                        body.add(param.getKey(), param.getValue());
                    }
                }
            }

            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
        } else if (method == HttpMethod.GET) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
            if (params != null) {
                for (Map.Entry<String, Object> param : params) {
                    if (param != null) {
                        builder.queryParam(param.getKey(), param.getValue());
                    }
                }
            }

            // URI 객체를 직접 사용하여 이중 인코딩 방지
            java.net.URI uri = builder.build().encode().toUri();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            responseEntity = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
        }

        if (responseEntity != null && responseEntity.getBody() != null) {
            result = S2StringUtil.decodeUnicode(responseEntity.getBody());

            logger.debug("Call URL: {}, Method: {}", url, method);
            logger.debug("Raw Response: {}", result);
        }

        return result;
    }

    /**
     * 주어진 {@link InputStream}, 파일명, 콘텐츠 길이를 이용하여 {@link InputStreamResource}를 생성.
     * 생성된 {@code InputStreamResource}는 {@link #getFilename()}과 {@link #contentLength()} 메서드를 오버라이드하여 파일명과 콘텐츠 길이를 명시적으로 반환.
     *
     * @param inputStream   전송할 데이터의 {@link InputStream}. (필수)
     * @param filename      다운로드될 확장자를 포함한 파일 이름. (필수)
     * @param contentLength 전송할 데이터의 총 길이 (바이트 단위). (필수, 0 이상)
     *                      ※ contentLength 가 없는 경우 InputStream 을 두번읽으면서 오류나 날수 있어 반드시 넣어야 한다.
     * @return 파일명과 콘텐츠 길이가 설정된 새로운 {@link InputStreamResource} 객체.
     * @throws IllegalArgumentException {@code inputStream} 또는 {@code filename}이 null이거나 {@code contentLength}가 음수인 경우.
     * @details
     *          <dl>
     *          <dd>InputStreamResource 를 사용할 때 Content-Length를 미리 계산해 설정하면 Spring 이 스트림을 미리 읽지 않는다.</dd>
     *          <dd>※ 즉 Content-Length 명시하지 않으면 InputStreamResource 를 2번 읽으면서 java.lang.IllegalStateException 예외가 발생한다.</dd>
     *          </dl>
     */
    public static InputStreamResource createInputStreamResource(InputStream inputStream, String filename, long contentLength) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream must not be null");
        }
        if (filename == null) {
            throw new IllegalArgumentException("Filename must not be null");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("Content length must not be negative");
        }
        return new InputStreamResource(inputStream) {

            @Override
            public String getFilename() {
                return filename;
            }

            @Override
            public long contentLength() {
                return contentLength;
            }
        };
    }

}
