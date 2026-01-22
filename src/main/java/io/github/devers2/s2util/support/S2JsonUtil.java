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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * s2's utilities
 * JSON 유틸리티 (Jackson 의존성 제거 - MethodHandle 기반 고성능 자체 구현)
 *
 * @author devers2
 * @version 2.0
 * @since 2025. 05. 27.
 */
public class S2JsonUtil {

    private static final S2Logger logger = S2LogManager.getLogger(S2JsonUtil.class);

    /**
     * JSON 파싱 옵션을 정의하는 Feature Enum (비트 플래그 기반)
     */
    public enum Feature {
        /** 이스케이프되지 않은 제어 문자(예: \n, \t, \r 등, ASCII 0-31)를 허용 */
        ALLOW_UNESCAPED_CONTROL_CHARS(1),
        /** 백슬래시(\) 이스케이프를 허용(예: \" 외에 \x 같은 비표준 이스케이프도 허용) */
        ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER(1 << 1),
        /** 비표준 숫자 값(예: NaN, Infinity, -Infinity)을 허용 */
        ALLOW_NON_NUMERIC_NUMBERS(1 << 2),
        /** JSON 배열에서 값이 누락된 경우(예: [1,,3])를 허용 */
        ALLOW_MISSING_VALUES(1 << 3),
        /** 객체 또는 배열의 끝에 불필요한 쉼표(예: {"a": 1,} 또는 [1,2,])를 허용 */
        ALLOW_TRAILING_COMMA(1 << 4),
        /** Java/C 스타일의 주석을 허용 */
        ALLOW_JAVA_COMMENTS(1 << 5),
        /** YAML 스타일의 주석(#)을 허용 */
        ALLOW_YAML_COMMENTS(1 << 6),
        /** JSON 문자열에서 단일 따옴표(')를 쌍따옴표(") 대신 허용 */
        ALLOW_SINGLE_QUOTES(1 << 7),
        /** JSON 객체의 필드 이름에서 따옴표를 생략한 경우(예: {name: "John"})를 허용 */
        ALLOW_UNQUOTED_FIELD_NAMES(1 << 8),
        /** 숫자 앞에 불필요한 0(예: 001, 0123)을 허용 */
        ALLOW_LEADING_ZEROS_FOR_NUMBERS(1 << 9),
        /** 숫자 앞에 + 기호(예: +123)를 허용 */
        ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS(1 << 10),
        /** 소수점으로 시작하는 숫자(예: .123)를 허용 */
        ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS(1 << 11),
        /** 숫자 끝에 소수점(예: 123.)을 허용 */
        ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS(1 << 12);

        private final int mask;

        Feature(int mask) {
            this.mask = mask;
        }

        public int getMask() {
            return mask;
        }

        /**
         * Feature 집합을 비트 플래그로 변환
         */
        public static int toFlags(Feature... features) {
            int flags = 0;
            if (features != null) {
                for (Feature f : features) {
                    flags |= f.mask;
                }
            }
            return flags;
        }

        /**
         * 비트 플래그에 특정 Feature가 포함되어 있는지 확인
         */
        public static boolean isEnabled(int flags, Feature feature) {
            return (flags & feature.mask) != 0;
        }
    }

    // =========================================================================
    // 직렬화 (Object -> JSON String)
    // =========================================================================

    /**
     * 객체를 JSON 문자열로 변환
     *
     * @param object JSON으로 변환할 객체 (Map, POJO, List 등)
     * @return 객체를 나타내는 JSON 문자열. 변환에 실패하면 null을 반환
     * @apiNote
     *
     *          <pre>{@code
     * // Map 직렬화
     * Map<String, Object> data = new HashMap<>();
     * data.put("name", "홍길동");
     * data.put("age", 30);
     * String json = S2JsonUtil.toJsonString(data);
     * // 결과: {"name":"홍길동","age":30}
     *
     * // POJO 직렬화
     * public class User {
     *     private String name;
     *     private int age;
     *     // getters/setters...
     * }
     * User user = new User();
     * user.setName("이순신");
     * user.setAge(45);
     * String json2 = S2JsonUtil.toJsonString(user);
     * // 결과: {"name":"이순신","age":45}
     *
     * // List 직렬화
     * List<String> items = Arrays.asList("apple", "banana", "cherry");
     * String json3 = S2JsonUtil.toJsonString(items);
     * // 결과: ["apple","banana","cherry"]
     * }</pre>
     */
    public static String toJsonString(Object object) {
        return toJsonString(object);
    }

    /**
     * 객체를 JSON 문자열로 변환
     *
     * @param object   JSON으로 변환할 객체 (Map, POJO, List 등)
     * @param features 선택적으로 활성화할 Feature 배열
     * @return 객체를 나타내는 JSON 문자열. 변환에 실패하면 null을 반환
     * @apiNote
     *
     *          <pre>{@code
     * // 특수 숫자 값 포함 직렬화
     * Map<String, Object> data = new HashMap<>();
     * data.put("normal", 123.45);
     * data.put("infinity", Double.POSITIVE_INFINITY);
     * String json = S2JsonUtil.toJsonString(data, Feature.ALLOW_NON_NUMERIC_NUMBERS);
     * // 결과: {"normal":123.45,"infinity":Infinity}
     * }</pre>
     */
    public static String toJsonString(Object object, Feature... features) {
        try {
            int flags = Feature.toFlags(features);
            StringBuilder sb = new StringBuilder();
            Set<Object> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            serializeValue(sb, object, visited, flags);
            return sb.toString();
        } catch (Exception e) {
            logger.warn("객체를 JSON으로 변환 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 값을 JSON 형식으로 직렬화 (재귀 지원)
     */
    private static void serializeValue(StringBuilder sb, Object value, Set<Object> visited, int flags) {
        if (value == null) {
            sb.append("null");
            return;
        }

        if (value instanceof String) {
            sb.append('"').append(escapeJsonString((String) value)).append('"');
            return;
        }

        if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());
            return;
        }

        if (value instanceof Enum<?>) {
            sb.append('"').append(escapeJsonString(value.toString())).append('"');
            return;
        }

        // 순환 참조 체크
        if (!visited.add(value)) {
            logger.warn("순환 참조가 감지되었습니다: {}", value.getClass().getName());
            sb.append("null");
            return;
        }

        try {
            if (value instanceof Map<?, ?> map) {
                serializeMap(sb, map, visited, flags);
            } else if (value instanceof Collection<?> coll) {
                serializeCollection(sb, coll, visited, flags);
            } else if (value.getClass().isArray()) {
                serializeArray(sb, value, visited, flags);
            } else {
                // POJO - MethodHandle 기반 필드 읽기
                serializePojo(sb, value, visited, flags);
            }
        } finally {
            visited.remove(value);
        }
    }

    /**
     * Map을 JSON 객체로 직렬화
     */
    private static void serializeMap(StringBuilder sb, Map<?, ?> map, Set<Object> visited, int flags) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first)
                sb.append(',');
            first = false;
            sb.append('"').append(escapeJsonString(String.valueOf(entry.getKey()))).append('"');
            sb.append(':');
            serializeValue(sb, entry.getValue(), visited, flags);
        }
        sb.append('}');
    }

    /**
     * Collection을 JSON 배열로 직렬화
     */
    private static void serializeCollection(StringBuilder sb, Collection<?> coll, Set<Object> visited, int flags) {
        sb.append('[');
        boolean first = true;
        for (Object item : coll) {
            if (!first)
                sb.append(',');
            first = false;
            serializeValue(sb, item, visited, flags);
        }
        sb.append(']');
    }

    /**
     * 배열을 JSON 배열로 직렬화
     */
    private static void serializeArray(StringBuilder sb, Object array, Set<Object> visited, int flags) {
        sb.append('[');
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0)
                sb.append(',');
            serializeValue(sb, Array.get(array, i), visited, flags);
        }
        sb.append(']');
    }

    /**
     * POJO를 JSON 객체로 직렬화 (MethodHandle 기반)
     */
    private static void serializePojo(StringBuilder sb, Object pojo, Set<Object> visited, int flags) {
        sb.append('{');
        boolean first = true;

        Class<?> clazz = pojo.getClass();
        var fields = clazz.getDeclaredFields();

        for (var field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object fieldValue = field.get(pojo);

                if (!first)
                    sb.append(',');
                first = false;

                sb.append('"').append(escapeJsonString(field.getName())).append('"');
                sb.append(':');
                serializeValue(sb, fieldValue, visited, flags);
            } catch (Throwable e) {
                logger.debug("필드 읽기 실패: {}.{}", clazz.getName(), field.getName());
            }
        }
        sb.append('}');
    }

    /**
     * JSON 문자열 이스케이핑 처리
     */
    private static String escapeJsonString(String str) {
        if (str == null)
            return "";

        StringBuilder sb = new StringBuilder(str.length() + 16);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // =========================================================================
    // 역직렬화 (JSON String -> Object)
    // =========================================================================

    /**
     * JSON 문자열을 Map으로 파싱
     *
     * @param jsonString 파싱할 JSON 형식의 문자열
     * @return 파싱된 Map 객체. 파싱에 실패하면 null을 반환
     * @throws IllegalArgumentException jsonString이 null이거나 빈 문자열인 경우
     * @apiNote
     *
     *          <pre>{@code
     * // 기본 JSON 파싱
     * String json = "{\"name\":\"홍길동\",\"age\":30,\"active\":true}";
     * Map<String, Object> data = S2JsonUtil.parseJson(json);
     * System.out.println(data.get("name")); // 출력: 홍길동
     * System.out.println(data.get("age"));  // 출력: 30
     *
     * // 중첩된 JSON 파싱
     * String json2 = "{\"user\":{\"name\":\"이순신\",\"email\":\"lee@example.com\"}}";
     * Map<String, Object> result = S2JsonUtil.parseJson(json2);
     * Map<String, Object> user = (Map<String, Object>) result.get("user");
     * System.out.println(user.get("name")); // 출력: 이순신
     * }</pre>
     */
    public static Map<String, Object> parseJson(String jsonString) {
        return parseJson(jsonString);
    }

    /**
     * JSON 문자열을 Map으로 파싱
     *
     * @param jsonString 파싱할 JSON 형식의 문자열
     * @param features   선택적으로 활성화할 Feature 가변 인자
     * @return 파싱된 Map 객체. 파싱에 실패하면 null을 반환
     * @throws IllegalArgumentException jsonString이 null이거나 빈 문자열인 경우
     * @apiNote
     *
     *          <pre>
     *          {@code
     * // 주석이 포함된 JSON 파싱
     * String jsonWithComments = """{
     *     // 사용자 이름
     *     "name": "홍길동",
     *     // 나이 정보
     *     "age":30
     * }""";
     *
     * Map<String, Object> data = S2JsonUtil.parseJson(
     *     jsonWithComments,
     *     Feature.ALLOW_JAVA_COMMENTS
     * );
     *
     * // 단일 따옴표 허용
     * String json2 = "{'name':'이순신','age':45}";
     * Map<String, Object> result = S2JsonUtil.parseJson(
     *     json2,
     *     Feature.ALLOW_SINGLE_QUOTES
     * );
     *
     * // Trailing comma 허용
     * String json3 = "{\"items\":[1,2,3,]}";
     * Map<String, Object> data2 = S2JsonUtil.parseJson(
     *     json3,
     *     Feature.ALLOW_TRAILING_COMMA
     * );
     * }</pre>
     */

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseJson(String jsonString, Feature... features) {
        if (jsonString == null || jsonString.isBlank()) {
            logger.warn("JSON 문자열이 null이거나 비어 있습니다.");
            throw new IllegalArgumentException("JSON 문자열은 null이거나 비어 있을 수 없습니다.");
        }

        try {
            int flags = Feature.toFlags(features);
            JsonParser parser = new JsonParser(jsonString, flags);
            Object result = parser.parse();
            return result instanceof Map ? (Map<String, Object>) result : null;
        } catch (Exception e) {
            logger.warn("JSON 파싱 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 문자열을 지정된 클래스 타입으로 파싱
     *
     * @param jsonString 파싱할 JSON 형식의 문자열
     * @param valueType  변환할 대상 클래스 타입
     * @param <T>        대상 클래스 타입
     * @return 파싱된 객체. 파싱에 실패하면 null을 반환
     * @throws IllegalArgumentException jsonString이 null이거나 빈 문자열인 경우
     * @apiNote
     *
     *          <pre>{@code
     * // POJO로 파싱
     * public class User {
     *     private String name;
     *     private int age;
     *     private String email;
     *     // getters/setters...
     * }
     *
     * String json = "{\"name\":\"홍길동\",\"age\":30,\"email\":\"hong@example.com\"}";
     * User user = S2JsonUtil.parseJsonTo(json, User.class);
     * System.out.println(user.getName()); // 출력: 홍길동
     * System.out.println(user.getAge());  // 출력: 30
     *
     * // Map으로 파싱
     * Map<String, Object> map = S2JsonUtil.parseJsonTo(json, Map.class);
     *
     * // List로 파싱
     * String jsonArray = "[{\"id\":1},{\"id\":2},{\"id\":3}]";
     * List<Map<String, Object>> list = S2JsonUtil.parseJsonTo(jsonArray, List.class);
     * }</pre>
     */
    public static <T> T parseJsonTo(String jsonString, Class<T> valueType) {
        return parseJsonTo(jsonString, valueType);
    }

    /**
     * JSON 문자열을 지정된 클래스 타입으로 파싱
     *
     * @param jsonString 파싱할 JSON 형식의 문자열
     * @param valueType  변환할 대상 클래스 타입
     * @param features   선택적으로 활성화할 Feature 배열
     * @param <T>        대상 클래스 타입
     * @return 파싱된 객체. 파싱에 실패하면 null을 반환
     * @throws IllegalArgumentException jsonString이 null이거나 빈 문자열이거나, valueType이 null인 경우
     * @apiNote
     *
     *          <pre>{@code
     * // 주석이 포함된 JSON을 POJO로 파싱
     * public class Product {
     *     private String name;
     *     private double price;
     *     // getters/setters...
     * }
     *
     * String jsonWithComments = """{
     *     // 상품명
     *     "name": "노트북",
     *     "price": 1500000
     * }""";
     * Product product = S2JsonUtil.parseJsonTo(
     *     jsonWithComments,
     *     Product.class,
     *     Feature.ALLOW_JAVA_COMMENTS
     * );
     *
     * // 여러 Feature 조합
     * String relaxedJson = "{'name':'마우스',price:25000,}";
     * Product product2 = S2JsonUtil.parseJsonTo(
     *     relaxedJson,
     *     Product.class,
     *     Feature.ALLOW_SINGLE_QUOTES,
     *     Feature.ALLOW_UNQUOTED_FIELD_NAMES,
     *     Feature.ALLOW_TRAILING_COMMA
     * );
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseJsonTo(String jsonString, Class<T> valueType, Feature... features) {
        if (jsonString == null || jsonString.isBlank()) {
            logger.warn("JSON 문자열이 null이거나 비어 있습니다.");
            throw new IllegalArgumentException("JSON 문자열은 null이거나 비어 있을 수 없습니다.");
        }
        if (valueType == null) {
            logger.warn("대상 클래스 타입이 null입니다.");
            throw new IllegalArgumentException("대상 클래스 타입은 null일 수 없습니다.");
        }

        try {
            int flags = Feature.toFlags(features);
            JsonParser parser = new JsonParser(jsonString, flags);
            Object parsed = parser.parse();

            // Map이면 POJO로 변환
            if (parsed instanceof Map && !Map.class.isAssignableFrom(valueType)) {
                return mapToPojo((Map<String, Object>) parsed, valueType);
            }

            // List면 타입 체크
            if (parsed instanceof List && List.class.isAssignableFrom(valueType)) {
                return (T) parsed;
            }

            // 직접 캐스팅 가능한 경우
            if (valueType.isInstance(parsed)) {
                return valueType.cast(parsed);
            }

            return null;
        } catch (Exception e) {
            logger.warn("JSON 파싱 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Map을 POJO로 변환 (MethodHandle 기반)
     */
    private static <T> T mapToPojo(Map<String, Object> map, Class<T> clazz) throws Exception {
        T instance = clazz.getDeclaredConstructor().newInstance();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            try {
                var field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                Object convertedValue = convertValue(value, fieldType);
                field.set(instance, convertedValue);
            } catch (Throwable e) {
                logger.debug("필드 설정 실패: {}.{}", clazz.getName(), fieldName);
            }
        }

        return instance;
    }

    /**
     * 값을 대상 타입으로 변환
     */
    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }

        // String -> Number
        if (value instanceof String str) {
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(str);
            }
            if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(str);
            }
            if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(str);
            }
            if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(str);
            }
        }

        // Number -> Number
        if (value instanceof Number num) {
            if (targetType == int.class || targetType == Integer.class) {
                return num.intValue();
            }
            if (targetType == long.class || targetType == Long.class) {
                return num.longValue();
            }
            if (targetType == double.class || targetType == Double.class) {
                return num.doubleValue();
            }
        }

        return value;
    }

    // =========================================================================
    // JSON Parser (상태 머신 기반)
    // =========================================================================

    private static class JsonParser {
        private final String json;
        private final int flags;
        private int pos;

        JsonParser(String json, int flags) {
            this.json = json;
            this.flags = flags;
            this.pos = 0;
        }

        Object parse() {
            skipWhitespace();
            return parseValue();
        }

        private Object parseValue() {
            skipWhitespace();
            if (pos >= json.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }

            char c = json.charAt(pos);
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseString('"');
                case '\'':
                    if (Feature.isEnabled(flags, Feature.ALLOW_SINGLE_QUOTES)) {
                        return parseString('\'');
                    }
                    throw new IllegalArgumentException("Single quotes not allowed");
                case 't':
                case 'f':
                    return parseBoolean();
                case 'n':
                    return parseNull();
                case 'N':
                case 'I':
                    if (Feature.isEnabled(flags, Feature.ALLOW_NON_NUMERIC_NUMBERS)) {
                        return parseSpecialNumber();
                    }
                    return parseNumber();
                default:
                    return parseNumber();
            }
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // skip '{'
            skipWhitespace();

            if (pos < json.length() && json.charAt(pos) == '}') {
                pos++;
                return map;
            }

            while (true) {
                skipWhitespace();

                // 키 파싱
                String key;
                char c = json.charAt(pos);
                if (c == '"' || c == '\'') {
                    key = parseString(c);
                } else if (Feature.isEnabled(flags, Feature.ALLOW_UNQUOTED_FIELD_NAMES)) {
                    key = parseUnquotedKey();
                } else {
                    throw new IllegalArgumentException("Expected quoted key at position " + pos);
                }

                skipWhitespace();
                if (pos >= json.length() || json.charAt(pos) != ':') {
                    throw new IllegalArgumentException("Expected ':' after key");
                }
                pos++; // skip ':'

                // 값 파싱
                Object value = parseValue();
                map.put(key, value);

                skipWhitespace();
                if (pos >= json.length()) {
                    throw new IllegalArgumentException("Unexpected end of JSON");
                }

                c = json.charAt(pos);
                if (c == '}') {
                    pos++;
                    break;
                } else if (c == ',') {
                    pos++;
                    skipWhitespace();
                    // Trailing comma 체크
                    if (Feature.isEnabled(flags, Feature.ALLOW_TRAILING_COMMA) && pos < json.length()
                            && json.charAt(pos) == '}') {
                        pos++;
                        break;
                    }
                } else {
                    throw new IllegalArgumentException("Expected ',' or '}' at position " + pos);
                }
            }

            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // skip '['
            skipWhitespace();

            if (pos < json.length() && json.charAt(pos) == ']') {
                pos++;
                return list;
            }

            while (true) {
                skipWhitespace();

                // Missing value 체크
                if (Feature.isEnabled(flags, Feature.ALLOW_MISSING_VALUES) && pos < json.length()
                        && (json.charAt(pos) == ',' || json.charAt(pos) == ']')) {
                    list.add(null);
                    if (json.charAt(pos) == ']') {
                        pos++;
                        break;
                    }
                    pos++;
                    continue;
                }

                Object value = parseValue();
                list.add(value);

                skipWhitespace();
                if (pos >= json.length()) {
                    throw new IllegalArgumentException("Unexpected end of JSON");
                }

                char c = json.charAt(pos);
                if (c == ']') {
                    pos++;
                    break;
                } else if (c == ',') {
                    pos++;
                    skipWhitespace();
                    // Trailing comma 체크
                    if (Feature.isEnabled(flags, Feature.ALLOW_TRAILING_COMMA) && pos < json.length()
                            && json.charAt(pos) == ']') {
                        pos++;
                        break;
                    }
                } else {
                    throw new IllegalArgumentException("Expected ',' or ']' at position " + pos);
                }
            }

            return list;
        }

        private String parseString(char quote) {
            pos++; // skip opening quote
            StringBuilder sb = new StringBuilder();

            while (pos < json.length()) {
                char c = json.charAt(pos);

                if (c == quote) {
                    pos++;
                    return sb.toString();
                }

                if (c == '\\') {
                    pos++;
                    if (pos >= json.length()) {
                        throw new IllegalArgumentException("Unexpected end of JSON in string");
                    }
                    char escaped = json.charAt(pos);
                    switch (escaped) {
                        case '"':
                        case '\'':
                        case '\\':
                        case '/':
                            sb.append(escaped);
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u':
                            // Unicode escape
                            pos++;
                            if (pos + 3 >= json.length()) {
                                throw new IllegalArgumentException("Invalid unicode escape");
                            }
                            String hex = json.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 3;
                            break;
                        default:
                            if (Feature.isEnabled(flags, Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)) {
                                sb.append(escaped);
                            } else {
                                throw new IllegalArgumentException("Invalid escape sequence: \\" + escaped);
                            }
                    }
                    pos++;
                } else {
                    if (c < 0x20 && !Feature.isEnabled(flags, Feature.ALLOW_UNESCAPED_CONTROL_CHARS)) {
                        throw new IllegalArgumentException("Unescaped control character at position " + pos);
                    }
                    sb.append(c);
                    pos++;
                }
            }

            throw new IllegalArgumentException("Unterminated string");
        }

        private String parseUnquotedKey() {
            StringBuilder sb = new StringBuilder();
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '$') {
                    sb.append(c);
                    pos++;
                } else {
                    break;
                }
            }
            return sb.toString();
        }

        private Boolean parseBoolean() {
            if (json.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            } else if (json.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid boolean at position " + pos);
        }

        private Object parseNull() {
            if (json.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid null at position " + pos);
        }

        private Number parseSpecialNumber() {
            if (json.startsWith("NaN", pos)) {
                pos += 3;
                return Double.NaN;
            } else if (json.startsWith("Infinity", pos)) {
                pos += 8;
                return Double.POSITIVE_INFINITY;
            } else if (json.startsWith("-Infinity", pos)) {
                pos += 9;
                return Double.NEGATIVE_INFINITY;
            }
            return parseNumber();
        }

        private Number parseNumber() {
            int start = pos;
            boolean isDouble = false;

            // Leading +
            if (pos < json.length() && json.charAt(pos) == '+') {
                if (!Feature.isEnabled(flags, Feature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)) {
                    throw new IllegalArgumentException("Leading + not allowed");
                }
                pos++;
            }

            // Leading -
            if (pos < json.length() && json.charAt(pos) == '-') {
                pos++;
            }

            // Leading zeros
            if (pos < json.length() && json.charAt(pos) == '0') {
                pos++;
                if (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                    if (!Feature.isEnabled(flags, Feature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)) {
                        throw new IllegalArgumentException("Leading zeros not allowed");
                    }
                }
            }

            // Digits
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                pos++;
            }

            // Decimal point
            if (pos < json.length() && json.charAt(pos) == '.') {
                isDouble = true;
                pos++;
                while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                    pos++;
                }
            }

            // Exponent
            if (pos < json.length() && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
                isDouble = true;
                pos++;
                if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) {
                    pos++;
                }
                while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                    pos++;
                }
            }

            String numStr = json.substring(start, pos);
            try {
                return isDouble ? Double.parseDouble(numStr) : Long.parseLong(numStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number: " + numStr);
            }
        }

        private void skipWhitespace() {
            while (pos < json.length()) {
                char c = json.charAt(pos);

                // 공백 문자
                if (Character.isWhitespace(c)) {
                    pos++;
                    continue;
                }

                // Java/C 주석
                if (Feature.isEnabled(flags, Feature.ALLOW_JAVA_COMMENTS)) {
                    if (c == '/' && pos + 1 < json.length()) {
                        char next = json.charAt(pos + 1);
                        if (next == '/') {
                            // 한 줄 주석
                            pos += 2;
                            while (pos < json.length() && json.charAt(pos) != '\n') {
                                pos++;
                            }
                            continue;
                        } else if (next == '*') {
                            // 블록 주석
                            pos += 2;
                            while (pos + 1 < json.length()) {
                                if (json.charAt(pos) == '*' && json.charAt(pos + 1) == '/') {
                                    pos += 2;
                                    break;
                                }
                                pos++;
                            }
                            continue;
                        }
                    }
                }

                // YAML 주석
                if (Feature.isEnabled(flags, Feature.ALLOW_YAML_COMMENTS) && c == '#') {
                    pos++;
                    while (pos < json.length() && json.charAt(pos) != '\n') {
                        pos++;
                    }
                    continue;
                }

                break;
            }
        }
    }
}
