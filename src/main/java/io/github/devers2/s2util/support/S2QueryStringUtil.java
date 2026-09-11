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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.github.devers2.s2util.core.S2StringUtil;

/**
 * s2's utilities
 *
 * @author devers2
 * @version 1.0
 * @since 2025. 05. 27.
 */
public class S2QueryStringUtil {

    /**
     * 키-값 쌍을 쿼리 문자열로 변환하는 메서드.
     *
     * @param baseString 기존 URL 또는 쿼리 문자열 {@code(예: "test.do", "test.do?wrong", "wrong&x=y")}
     * @param entries    추가할 키-값 쌍 (가변인자, 예: Map.entry("a", "1"), Map.entry("b", "2"))
     * @return 결합된 URL 및 쿼리 문자열
     * @apiNote
     *
     *          <pre>{@code
     * queryStringFromEntries("", Map.entry("a", "1"), Map.entry("b", "2")) → "a=1&b=2"
     * queryStringFromEntries("test.do", Map.entry("a", "1"), Map.entry("b", "2")) → "test.do?a=1&b=2"
     * queryStringFromEntries("test.do?wrong", Map.entry("a", "1"), Map.entry("b", "2")) → "test.do?wrong=&a=1&b=2"
     * queryStringFromEntries("wrong&x=y", Map.entry("a", "1"), Map.entry("b", "2"), Map.entry("x", "z")) → "wrong=&x=z&a=1&b=2"
     * }</pre>
     *
     * @details
     *          <dl>
     *          <dd>- 쿼리 문자열 앞에 URL 이 있다면 기존 URL 은 유지하고 이후에 추가한다.</dd>
     *          <dd>- 기존 쿼리 문자열에 동일 키값이 있으면 덮어쓴다.</dd>
     *          <dd>- 키와 값은 URL 인코딩되며, 기존 쿼리 문자열은 디코딩 후 처리된다.</dd>
     *          <dd>- 입력 유효성 검사를 통해 제어 문자 등 비정상 입력을 필터링한다.</dd>
     *          </dl>
     */
    @SafeVarargs
    public static String queryStringFromEntries(String baseString, Entry<String, String>... entries) {
        var url = "";
        var existingQueryString = new StringBuilder();
        Map<String, String> queryParams = new HashMap<>();

        // 입력 유효성 검사 및 baseString 처리
        if (baseString != null && !baseString.isBlank()) {
            var sanitizedBaseString = S2StringUtil.sanitizeInput(baseString);
            var queryIndex = sanitizedBaseString.indexOf('?');
            if (queryIndex > -1) {
                url = sanitizedBaseString.substring(0, queryIndex);
                var checkString = sanitizedBaseString.substring(queryIndex + 1);
                if (!checkString.isBlank()) {
                    if (checkString.contains("=")) {
                        existingQueryString.append(checkString);
                    } else {
                        queryParams.put(S2StringUtil.sanitizeInput(checkString), "");
                    }
                }
            } else if (sanitizedBaseString.contains("=")) {
                existingQueryString.append(sanitizedBaseString);
            } else {
                url = sanitizedBaseString;
            }
        }

        // 기존 쿼리 문자열을 파싱 (StringBuilder 로 성능 최적화, 디코딩 적용)
        if (existingQueryString.length() > 0) {
            parseQueryString(existingQueryString.toString(), queryParams);
        }

        // 가변인자로 받은 Entry 추가 (null 값 제외, 중복 키 덮어씀)
        if (entries != null) {
            for (Entry<String, String> entry : entries) {
                if (entry != null && entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null) {
                    queryParams.put(S2StringUtil.sanitizeInput(entry.getKey()),
                            S2StringUtil.sanitizeInput(entry.getValue()));
                }
            }
        }

        // 쿼리 문자열 생성 (URL 인코딩 적용)
        var resultQueryString = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                if (resultQueryString.length() > 0) {
                    resultQueryString.append('&');
                }
                resultQueryString.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()))
                        .append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode query parameters", e);
        }

        // 최종 결과 조합
        return url + (url.isBlank() || resultQueryString.length() == 0 ? "" : "?") + resultQueryString;
    }

    /**
     * 쿼리 문자열을 파싱하여 Map 에 저장 (디코딩 및 성능 최적화)
     */
    private static void parseQueryString(String queryString, Map<String, String> queryParams) {
        var keyBuilder = new StringBuilder();
        var valueBuilder = new StringBuilder();
        var parsingKey = true;

        try {
            for (int i = 0; i < queryString.length(); i++) {
                char c = queryString.charAt(i);
                if (c == '&') {
                    if (keyBuilder.length() > 0) {
                        var key = S2StringUtil.sanitizeInput(keyBuilder.toString());
                        var value = valueBuilder.length() > 0
                                ? URLDecoder.decode(valueBuilder.toString(), StandardCharsets.UTF_8.name())
                                : "";
                        if (!key.isBlank()) {
                            queryParams.put(key, S2StringUtil.sanitizeInput(value));
                        }
                    }
                    keyBuilder.setLength(0);
                    valueBuilder.setLength(0);
                    parsingKey = true;
                } else if (c == '=' && parsingKey) {
                    parsingKey = false;
                } else if (parsingKey) {
                    keyBuilder.append(c);
                } else {
                    valueBuilder.append(c);
                }
            }

            // 마지막 파라미터 처리
            if (keyBuilder.length() > 0) {
                var key = S2StringUtil.sanitizeInput(keyBuilder.toString());
                var value = valueBuilder.length() > 0
                        ? URLDecoder.decode(valueBuilder.toString(), StandardCharsets.UTF_8.name())
                        : "";
                if (!key.isBlank()) {
                    queryParams.put(key, S2StringUtil.sanitizeInput(value));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decode query string", e);
        }
    }

    /**
     * Query String에서 특정 파라미터 값을 조회한다.
     *
     * @param queryString Query String 또는 URL {@code(예: "a=1&b=2", "http://example.com?a=1")}
     * @param key         조회할 파라미터 키
     * @return 파라미터 값 (키가 없거나 값이 없는 경우 빈 문자열, URL 디코딩 적용)
     */
    public static String getQueryStringParameter(String queryString, String key) {
        if (queryString == null || key == null || queryString.isBlank() || key.isBlank()) {
            return "";
        }

        // URL 에서 쿼리 문자열 추출
        String query = queryString;
        int queryIndex = queryString.indexOf('?');
        if (queryIndex >= 0) {
            query = queryString.substring(queryIndex + 1);
        }

        if (!query.contains("=")) {
            return "";
        }

        // 쿼리 문자열 파싱
        try {
            int start = 0;
            while (start < query.length()) {
                int ampIndex = query.indexOf('&', start);
                if (ampIndex == -1) {
                    ampIndex = query.length();
                }

                String param = query.substring(start, ampIndex);
                if (!param.isBlank()) {
                    int eqIndex = param.indexOf('=');
                    if (eqIndex >= 0) {
                        String paramKey = param.substring(0, eqIndex);
                        if (key.equals(paramKey)) {
                            String value = eqIndex < param.length() - 1 ? param.substring(eqIndex + 1) : "";
                            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
                        }
                    }
                }
                start = ampIndex + 1;
            }
        } catch (Exception e) {
            // 디코딩 실패 시 빈 문자열 반환
            return "";
        }

        return "";
    }

}
