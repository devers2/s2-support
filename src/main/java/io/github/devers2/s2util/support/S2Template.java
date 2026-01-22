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
     * [Value 기반 바인딩] 값이 유효할 때만 'prefix + value' 형태로 치환합니다.
     * <p>
     * 실제 데이터 값을 쿼리나 메시지에 직접 포함하고 싶을 때 사용합니다.
     * </p>
     *
     * @param key    템플릿 내의 치환 대상 키 (예: "name" -> {{=name}})
     * @param value  치환될 실제 데이터 값
     * @param prefix 값이 존재할 때 값 앞에 붙일 접두사 (예: "AND name = ")
     * @return 메서드 체이닝을 위한 현재 인스턴스
     */
    public S2Template bindValue(String key, Object value, String prefix) {
        bindings.put(key, isValid(value) ? prefix + value.toString() : "");
        return this;
    }

    /**
     * [Value 기반 바인딩] 접두사 없이 값 자체만 치환합니다.
     *
     * @param key   템플릿 내의 치환 대상 키
     * @param value 치환될 실제 데이터 값
     * @return 메서드 체이닝을 위한 현재 인스턴스
     */
    public S2Template bindValue(String key, Object value) {
        return bindValue(key, value, "");
    }

    /**
     * [Clause 기반 바인딩] 값이 유효할 때만 지정된 쿼리 구절(String) 자체를 치환합니다.
     * <p>
     * 주로 JPQL/SQL의 파라미터 바인딩(:name) 문구를 조건부로 삽입할 때 사용합니다.
     * {@code value}는 존재 여부를 판단하는 트리거 역할만 하며, 실제 치환은 {@code clause} 문자열로 이루어집니다.
     * </p>
     *
     * @param key    템플릿 내의 치환 대상 키
     * @param value  유효성을 검사할 기준 값 (null, 빈 문자열 여부 등 판단)
     * @param clause 값이 유효할 때 주입할 실제 문자열 구절 (예: "AND m.id = :id")
     * @return 메서드 체이닝을 위한 현재 인스턴스
     */
    public S2Template bindClause(String key, Object value, String clause) {
        bindings.put(key, isValid(value) ? clause : "");
        return this;
    }

    /**
     * [Query IN절 바인딩] 컬렉션 요소를 SQL 'IN' 절 문법에 맞게 포맷팅하여 치환합니다.
     * <p>
     * 문자열 요소는 자동으로 홑따옴표('') 처리 및 내부 이스케이프가 적용됩니다.
     * </p>
     *
     * @param key    템플릿 내의 치환 대상 키
     * @param values 바인딩할 컬렉션 데이터 (v1, v2, v3)
     * @param prefix 값이 존재할 때 앞에 붙일 접두사 (예: "AND m.id IN ")
     * @return 메서드 체이닝을 위한 현재 인스턴스
     */
    public S2Template bindInQuery(String key, Collection<?> values, String prefix) {
        if (isValid(values)) {
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
     * [함수형 조건 제어] 값이 유효할 때만 특정 작업(Action)을 수행합니다.
     * <p>
     * 바인딩 외에 로그 출력이나 추가적인 상태 변경이 필요할 때 유용합니다.
     * </p>
     *
     * @param value  유효성을 검사할 값
     * @param action 값이 유효할 때 실행할 Consumer (현재 S2Template 인스턴스가 전달됨)
     * @return 메서드 체이닝을 위한 현재 인스턴스
     */
    public S2Template ifPresent(Object value, Consumer<S2Template> action) {
        if (isValid(value)) {
            action.accept(this);
        }
        return this;
    }

    /**
     * 바인딩된 데이터를 바탕으로 최종 문자열을 생성합니다.
     * <p>
     * 미치환된 패턴 제거, 빈 줄 제거, 줄 끝 공백 정리 및 전체 앞뒤 공백 정리 로직이 적용됩니다.
     * </p>
     *
     * @return 정리된 형태의 최종 결과 문자열
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

    /**
     * 값의 유효성을 체크합니다.
     */
    private boolean isValid(Object value) {
        if (value == null)
            return false;
        if (value instanceof Boolean b)
            return b;
        return S2Util.isNotEmpty(value);
    }

}
