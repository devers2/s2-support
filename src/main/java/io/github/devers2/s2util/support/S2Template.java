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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.github.devers2.s2util.core.S2StringUtil;
import io.github.devers2.s2util.core.S2Util;

/**
 * <p>
 * S2Template은 Java 17 이상의 환경에서 동적 문자열(SQL, JPQL, 메시지 등)을
 * 안전하고 가독성 있게 생성하기 위한 유틸리티입니다.
 * </p>
 *
 * <p>
 * JS 스타일의 {@code {{=key}}} 문법을 사용하며, 값이 존재할 때만 특정 구문을 포함시키는
 * 조건부 바인딩과 쿼리 전용 포맷팅 기능을 제공합니다.
 * </p>
 *
 * <pre>{@code
 * String jpql = S2Template.of(
 *         """
 *             SELECT m FROM Member m
 *             WHERE 1=1
 *             {{=name_cond}}
 *             {{=age_in}}
 *         """
 * )
 *         .bind("name_cond", name, "AND m.name = ")
 *         .bindInQuery("age_in", ageList, "AND m.age IN ")
 *         .render();
 * }</pre>
 *
 * @author devers2
 * @since 1.0.0
 */
public class S2Template {

    private final String template;
    private final Map<String, String> bindings = new HashMap<>();

    private S2Template(String template) {
        this.template = Objects.requireNonNull(template, "Template must not be null");
    }

    /**
     * 새로운 S2Template 인스턴스를 생성합니다.
     *
     * @param template {{=key}} 형식을 포함한 템플릿 문자열 (텍스트 블록 사용 권장)
     * @return S2Template 인스턴스
     */
    public static S2Template of(String template) {
        return new S2Template(template);
    }

    /**
     * 일반적인 문자열 바인딩을 수행합니다.
     * 값이 유효(null이 아니고 비어있지 않음)할 때만 prefix와 함께 치환됩니다.
     *
     * @param key    템플릿 내의 키 ({{=key}} 형태)
     * @param value  치환될 값
     * @param prefix 값이 존재할 때 앞에 붙을 접두사
     * @return 메서드 체이닝을 위한 현재 인스턴스
     */
    public S2Template bind(String key, Object value, String prefix) {
        bindings.put(key, S2Util.isNotEmpty(value) ? prefix + value.toString() : "");
        return this;
    }

    /**
     * 접두사 없이 단순 키-값 바인딩을 수행합니다
     *
     * @param key   템플릿 내의 키 ({{=key}} 형태)
     * @param value 치환될 값
     * @return 메서드 체이닝을 위한 현재 인스턴스
     */
    public S2Template bind(String key, Object value) {
        return bind(key, value, "");
    }

    /**
     * 쿼리 전용 IN 절 바인딩을 수행합니다.
     * 컬렉션 내부의 문자열 요소는 자동으로 홑따옴표('') 처리를 하며, 홑따옴표 이스케이프를 지원합니다.
     *
     * @param key    템플릿 내의 키
     * @param values 바인딩할 컬렉션 값
     * @param prefix 값이 존재할 때 앞에 붙을 접두사 (예: "AND id IN ")
     * @return 메서드 체이닝을 위한 현재 인스턴스
     */
    public S2Template bindInQuery(String key, Collection<?> values, String prefix) {
        if (S2Util.isNotEmpty(values)) {
            String inClause = values.stream()
                    .map(this::formatQueryValue)
                    .collect(Collectors.joining(", ", "(", ")"));
            bindings.put(key, prefix + inClause);
        } else {
            bindings.put(key, "");
        }
        return this;
    }

    /**
     * 값이 유효할 때만 추가적인 바인딩 액션을 수행할 수 있도록 합니다.
     *
     * @param value  검사할 값
     * @param action 값이 유효할 때 실행할 Consumer 작업 (S2Template 인스턴스가 전달됨)
     * @return 메서드 체이닝을 위한 현재 인스턴스
     */
    public S2Template ifPresent(Object value, Consumer<S2Template> action) {
        if (S2Util.isNotEmpty(value)) {
            action.accept(this);
        }
        return this;
    }

    /**
     * 바인딩된 데이터를 바탕으로 최종 문자열을 생성합니다.
     * 미치환 패턴 제거, 빈 줄 정리, 앞뒤 공백 제거 등의 디테일 로직이 적용됩니다.
     *
     * @return 정리된 최종 결과 문자열
     */
    public String render() {
        String result = template;

        // 1. 등록된 바인딩 치환
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            result = result.replace("{{=" + entry.getKey() + "}}", entry.getValue());
        }

        // 2. 미치환된 {{=...}} 패턴 제거 (JS 스타일 호환)
        result = S2StringUtil.replaceAll(result, "\\{\\{=.*?\\}\\}", "");

        // 3. 디테일 정리: 빈 줄 제거 및 줄 끝 공백 정리
        return result.lines()
                .filter(line -> !line.isBlank())
                .map(String::stripTrailing)
                .collect(Collectors.joining("\n"))
                .strip();
    }

    /**
     * 쿼리 문법에 맞게 값을 포맷팅합니다. (홑따옴표 처리 등)
     */
    private String formatQueryValue(Object value) {
        if (value == null)
            return "NULL";
        if (value instanceof String s) {
            return "'" + s.replace("'", "''") + "'";
        }
        return value.toString();
    }

}
