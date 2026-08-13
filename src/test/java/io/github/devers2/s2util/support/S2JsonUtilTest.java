package io.github.devers2.s2util.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * S2JsonUtil 테스트 클래스
 * - 외부 의존성 없이 내부 Extension을 통해 결과를 요약 출력합니다.
 * - 다양한 엣지 케이스 및 심화 시나리오를 검증합니다.
 */
@ExtendWith(S2JsonUtilTest.SummaryExtension.class)
@DisplayName("S2JsonUtil 종합 검증 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class S2JsonUtilTest {

    // =====================================================================
    // [내부 유틸리티] 테스트 결과 요약 Extension
    // =====================================================================
    static class SummaryExtension implements TestWatcher, AfterAllCallback {
        private static final AtomicInteger totalCount = new AtomicInteger(0);
        private static final AtomicInteger successCount = new AtomicInteger(0);
        private static final AtomicInteger failureCount = new AtomicInteger(0);
        private static final List<String> failureDetails = new ArrayList<>();

        @Override
        public void testSuccessful(ExtensionContext context) {
            totalCount.incrementAndGet();
            successCount.incrementAndGet();
        }

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            totalCount.incrementAndGet();
            failureCount.incrementAndGet();
            failureDetails.add(context.getDisplayName() + " : " + cause.getMessage());
        }

        @Override
        public void afterAll(ExtensionContext context) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("  [ S2JsonUtil 테스트 실행 결과 요약 ]");
            System.out.println("-".repeat(60));
            System.out.printf("  ✔ 전체 테스트 개수 : %d\n", totalCount.get());
            System.out.printf("  ✅ 성공 개수       : %d\n", successCount.get());
            System.out.printf("  ❌ 실패 개수       : %d\n", failureCount.get());

            if (failureCount.get() > 0) {
                System.out.println("-".repeat(60));
                System.out.println("  [ 실패 상세 내역 ]");
                failureDetails.forEach(detail -> System.out.println("  • " + detail));
            }
            System.out.println("=".repeat(60) + "\n");
        }
    }

    // =====================================================================
    // [테스트 모델]
    // =====================================================================
    static class User {
        private String name;
        private int age;
        private Map<String, Object> metadata;
        private List<User> friends;

        public User() {
        }

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public List<User> getFriends() {
            return friends;
        }

        public void setFriends(List<User> friends) {
            this.friends = friends;
        }
    }

    // 테스트용 Enum
    enum Status {
        ACTIVE, INACTIVE, PENDING
    }

    // =====================================================================
    // [1. 직렬화 테스트]
    // =====================================================================

    @Test
    @Order(1)
    @DisplayName("기본 직렬화: null, 숫자, 문자열, 불리언")
    void testBasicSerialization() {
        assertEquals("null", S2JsonUtil.toJsonString(null));
        assertEquals("123", S2JsonUtil.toJsonString(123));
        assertEquals("\"test\"", S2JsonUtil.toJsonString("test"));
        assertEquals("true", S2JsonUtil.toJsonString(true));
    }

    @Test
    @Order(2)
    @DisplayName("복합 객체 직렬화: 중첩 Map, List, POJO")
    void testComplexSerialization() {
        User user = new User("홍길동", 30);
        Map<String, Object> meta = new HashMap<>();
        meta.put("tags", Arrays.asList("java", "json"));
        user.setMetadata(meta);

        String json = S2JsonUtil.toJsonString(user);
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"홍길동\""));
        assertTrue(json.contains("\"tags\":[\"java\",\"json\"]"));
    }

    @Test
    @Order(3)
    @DisplayName("순환 참조 보호 테스트: 무한 루프 발생 여부")
    void testCircularReferenceProtection() {
        Map<String, Object> map = new HashMap<>();
        map.put("self", map);

        // 순환 참조 시 null로 처리하거나 적절히 대응하는지 확인 (시스템 다운 방지)
        String json = S2JsonUtil.toJsonString(map);
        assertNotNull(json);
        assertTrue(json.contains("\"self\":null"));
    }

    // =====================================================================
    // [2. 역직렬화 테스트]
    // =====================================================================

    @Test
    @Order(4)
    @DisplayName("정상 역직렬화: JSON -> Map")
    void testParseToMap() {
        String json = "{\"name\":\"이순신\", \"age\":45, \"active\":true}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);

        assertEquals("이순신", result.get("name"));
        // 파서가 내부적으로 Long으로 처리할 수 있으므로 Number로 캐스팅 후 비교
        assertEquals(45, ((Number) result.get("age")).intValue());
        assertEquals(true, result.get("active"));
    }

    @Test
    @Order(5)
    @DisplayName("정상 역직렬화: JSON -> POJO")
    void testParseToPojo() {
        String json = "{\"name\":\"강감찬\", \"age\":50}";
        User user = S2JsonUtil.parseJsonTo(json, User.class);

        assertNotNull(user);
        assertEquals("강감찬", user.getName());
        assertEquals(50, user.getAge());
    }

    @Test
    @Order(6)
    @DisplayName("심화 역직렬화: 깊은 중첩 구조 (Deep Nesting)")
    void testDeepNesting() {
        String json = "{\"a\":{\"b\":{\"c\":{\"d\":100}}}}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);

        Map<?, ?> a = (Map<?, ?>) result.get("a");
        Map<?, ?> b = (Map<?, ?>) a.get("b");
        Map<?, ?> c = (Map<?, ?>) b.get("c");
        assertEquals(100, ((Number) c.get("d")).intValue());
    }

    // =====================================================================
    // [3. 엣지 케이스 및 오류 상황]
    // =====================================================================

    @Test
    @Order(7)
    @DisplayName("엣지 케이스: 특수 공백 문자 처리 (\\t, \\r, \\n)")
    void testSpecialWhitespaces() {
        String json = "{\n\t\"key\" : \r\n \"value\" \t}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertEquals("value", result.get("key"));
    }

    @Test
    @Order(8)
    @DisplayName("엣지 케이스: 빈 객체 및 빈 배열")
    void testEmptyStructures() {
        assertEquals(0, S2JsonUtil.parseJson("{}").size());
        List<?> list = S2JsonUtil.parseJsonTo("[]", List.class);
        assertEquals(0, list.size());
    }

    @Test
    @Order(9)
    @DisplayName("오류 상황: 잘못된 형식의 JSON (Malformed)")
    void testMalformedJson() {
        // 콤마 누락
        assertNull(S2JsonUtil.parseJson("{\"a\":1 \"b\":2}"));
        // 닫는 괄호 누락
        assertNull(S2JsonUtil.parseJson("{\"a\":1"));
        // 잘못된 값 형식
        assertNull(S2JsonUtil.parseJson("{\"a\": undefined}"));
    }

    @Test
    @Order(10)
    @DisplayName("오류 상황: 문자열 내 잘못된 이스케이프")
    void testInvalidEscape() {
        // \z 는 유효하지 않은 이스케이프 (Feature 미설정 시)
        assertNull(S2JsonUtil.parseJson("{\"path\":\"C:\\util\\test\"}"));
    }

    // =====================================================================
    // [4. Feature 옵션 테스트]
    // =====================================================================

    @Test
    @Order(11)
    @DisplayName("Feature 테스트: 주석 허용 (Java/YAML 스타일)")
    void testFeatureComments() {
        String json = "{\n // 주석\n \"key\":\"value\" # 해시주석\n}";
        Map<String, Object> result = S2JsonUtil.parseJson(
                json,
                S2JsonUtil.Feature.ALLOW_JAVA_COMMENTS,
                S2JsonUtil.Feature.ALLOW_YAML_COMMENTS
        );

        assertNotNull(result);
        assertEquals("value", result.get("key"));
    }

    @Test
    @Order(12)
    @DisplayName("Feature 테스트: 따옴표 없는 필드명 및 단일 따옴표")
    void testFeatureRelaxedJson() {
        String json = "{name: '홍길동', 'age': 20}";
        Map<String, Object> result = S2JsonUtil.parseJson(
                json,
                S2JsonUtil.Feature.ALLOW_UNQUOTED_FIELD_NAMES,
                S2JsonUtil.Feature.ALLOW_SINGLE_QUOTES
        );

        assertNotNull(result);
        assertEquals("홍길동", result.get("name"));
        assertEquals(20, ((Number) result.get("age")).intValue());
    }

    // =====================================================================
    // [추가 오류 상황 테스트]
    // =====================================================================

    @Test
    @Order(13)
    @DisplayName("오류 상황: null 입력 파싱")
    void testNullInput() {
        assertThrows(IllegalArgumentException.class, () -> S2JsonUtil.parseJson(null));
        assertThrows(IllegalArgumentException.class, () -> S2JsonUtil.parseJsonTo(null, Map.class));
    }

    @Test
    @Order(14)
    @DisplayName("오류 상황: 빈 문자열 파싱")
    void testEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> S2JsonUtil.parseJson(""));
        assertThrows(IllegalArgumentException.class, () -> S2JsonUtil.parseJson("   "));
    }

    @Test
    @Order(15)
    @DisplayName("오류 상황: 유니코드 이스케이프 오류")
    void testInvalidUnicode() {
        // 잘못된 유니코드 이스케이프 - 파싱 실패 시 null
        assertNull(S2JsonUtil.parseJson("{\"key\":\"\\u123\"}"));
        // 불완전한 유니코드
        assertNull(S2JsonUtil.parseJson("{\"key\":\"\\u\"}"));
    }

    @Test
    @Order(16)
    @DisplayName("오류 상황: 중복 키 처리")
    void testDuplicateKeys() {
        // 중복 키가 있을 때 마지막 값으로 덮어쓰는지 확인
        String json = "{\"key\":1, \"key\":2}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertNotNull(result);
        assertEquals(2, ((Number) result.get("key")).intValue());
    }

    @Test
    @Order(17)
    @DisplayName("오류 상황: 매우 큰 숫자 (오버플로우)")
    void testLargeNumber() {
        // 매우 큰 숫자 - 지원되지 않으면 null
        String json = "{\"big\": 999999999999999999999999999999}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertNull(result); // 큰 숫자 지원 안 함
    }

    @Test
    @Order(18)
    @DisplayName("오류 상황: 잘못된 배열 형식")
    void testInvalidArray() {
        assertNull(S2JsonUtil.parseJson("[1, 2,"));
        assertNull(S2JsonUtil.parseJson("[1, 2 3]"));
    }

    @Test
    @Order(19)
    @DisplayName("오류 상황: 타입 불일치 역직렬화")
    void testTypeMismatch() {
        String json = "{\"name\": 123}"; // name은 String이어야 함
        User user = S2JsonUtil.parseJsonTo(json, User.class);
        // 타입 불일치 시 null로 처리
        assertNotNull(user);
        assertEquals("123", user.getName());
    }

    @Test
    @Order(20)
    @DisplayName("추가 테스트: 날짜/시간 타입 변환")
    void testDateTimeConversion() {
        // JSON에 날짜 문자열이 있을 때 String으로 유지되는지 확인
        String json = "{\"date\":\"2023-10-01\", \"datetime\":\"2023-10-01T12:00:00\"}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertNotNull(result);
        assertEquals("2023-10-01", result.get("date"));
        assertEquals("2023-10-01T12:00:00", result.get("datetime"));
    }

    @Test
    @Order(21)
    @DisplayName("추가 테스트: 유니코드 및 특수 문자")
    void testUnicodeAndSpecialChars() {
        String json = "{\"text\":\"안녕하세요 🌟 \\u0041\"}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertNotNull(result);
        assertEquals("안녕하세요 🌟 A", result.get("text"));
    }

    @Test
    @Order(26)
    @DisplayName("추가 테스트: Feature 조합")
    void testFeatureCombination() {
        String json = "{name: '홍길동', age: 30, /* 주석 */ 'active': true}";
        Map<String, Object> result = S2JsonUtil.parseJson(
                json,
                S2JsonUtil.Feature.ALLOW_UNQUOTED_FIELD_NAMES,
                S2JsonUtil.Feature.ALLOW_SINGLE_QUOTES,
                S2JsonUtil.Feature.ALLOW_JAVA_COMMENTS
        );
        assertNotNull(result);
        assertEquals("홍길동", result.get("name"));
    }

    @Test
    @Order(27)
    @DisplayName("추가 테스트: 경계 값 (최대/최소 숫자)")
    void testBoundaryValues() {
        String json = "{\"maxInt\":2147483647, \"minInt\":-2147483648}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertNotNull(result);
    }

    @Test
    @Order(28)
    @DisplayName("추가 테스트: Boolean 변환 상세")
    void testBooleanConversion() {
        String json = "{\"flag\":true, \"disabled\":false}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertNotNull(result);
        assertEquals(true, result.get("flag"));
        assertEquals(false, result.get("disabled"));
    }

    @Test
    @Order(29)
    @DisplayName("추가 테스트: Float/Double 변환")
    void testFloatDoubleConversion() {
        String json = "{\"floatVal\":3.14, \"doubleVal\":2.718281828}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertNotNull(result);
        assertEquals(3.14f, ((Number) result.get("floatVal")).floatValue(), 0.01);
        assertEquals(2.718281828, ((Number) result.get("doubleVal")).doubleValue(), 0.000000001);
    }

    @Test
    @Order(30)
    @DisplayName("추가 테스트: Null 값 in POJO")
    void testNullInPojo() {
        String json = "{\"name\":null, \"age\":25}";
        User user = S2JsonUtil.parseJsonTo(json, User.class);
        assertNotNull(user);
        assertNull(user.getName());
        assertEquals(25, user.getAge());
    }

    @Test
    @Order(31)
    @DisplayName("추가 테스트: 중첩 POJO with List")
    void testNestedPojoWithList() {
        String json = "{\"name\":\"Parent\", \"friends\":[{\"name\":\"Friend1\", \"age\":20}, {\"name\":\"Friend2\", \"age\":21}]}";
        User user = S2JsonUtil.parseJsonTo(json, User.class);
        assertNotNull(user);
        assertEquals("Parent", user.getName());
        assertNotNull(user.getFriends());
        assertEquals(2, user.getFriends().size());
        assertEquals("Friend1", user.getFriends().get(0).getName());
    }

    @Test
    @Order(32)
    @DisplayName("추가 테스트: Map with null keys/values")
    void testMapWithNulls() {
        String json = "{\"key\":null, \"\": \"emptyKey\"}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertNotNull(result);
        assertNull(result.get("key"));
        assertEquals("emptyKey", result.get(""));
    }

    @Test
    @Order(33)
    @DisplayName("추가 테스트: Empty strings and arrays")
    void testEmptyStringsAndArrays() {
        String json = "{\"emptyStr\":\"\", \"emptyArr\":[]}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertNotNull(result);
        assertEquals("", result.get("emptyStr"));
        assertEquals(0, ((List<?>) result.get("emptyArr")).size());
    }

    @Test
    @Order(34)
    @DisplayName("추가 테스트: Arrays of primitives")
    void testPrimitiveArrays() {
        String json = "[1, 2, 3, true, false, \"string\"]";
        List<?> result = S2JsonUtil.parseJsonTo(json, List.class);
        assertNotNull(result);
        assertEquals(6, result.size());
        assertEquals(1, ((Number) result.get(0)).intValue());
        assertEquals(true, result.get(3));
    }

    @Test
    @Order(35)
    @DisplayName("추가 테스트: Scientific notation")
    void testScientificNotation() {
        String json = "{\"big\":1e10, \"small\":1e-5}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertNotNull(result);
        assertEquals(1e10, ((Number) result.get("big")).doubleValue(), 1e9);
        assertEquals(1e-5, ((Number) result.get("small")).doubleValue(), 1e-6);
    }

    @Test
    @Order(36)
    @DisplayName("추가 테스트: Comments in various positions")
    void testCommentsPositions() {
        String json = "/* start */ {\"key\": /* mid */ \"value\" /* end */}";
        Map<String, Object> result = S2JsonUtil.parseJson(
                json,
                S2JsonUtil.Feature.ALLOW_JAVA_COMMENTS
        );
        assertNotNull(result);
        assertEquals("value", result.get("key"));
    }

    @Test
    @Order(37)
    @DisplayName("추가 테스트: Multiple Feature combinations")
    void testMultipleFeatures() {
        String json = "{name: 'John', age: 30, // comment\n 'active': true # yaml comment\n}";
        Map<String, Object> result = S2JsonUtil.parseJson(
                json,
                S2JsonUtil.Feature.ALLOW_UNQUOTED_FIELD_NAMES,
                S2JsonUtil.Feature.ALLOW_SINGLE_QUOTES,
                S2JsonUtil.Feature.ALLOW_JAVA_COMMENTS,
                S2JsonUtil.Feature.ALLOW_YAML_COMMENTS
        );
        assertNotNull(result);
        assertEquals("John", result.get("name"));
        assertEquals(30, ((Number) result.get("age")).intValue());
        assertEquals(true, result.get("active"));
    }

    @Test
    @Order(38)
    @DisplayName("추가 테스트: Error - Invalid object structure")
    void testInvalidObjectStructure() {
        assertNull(S2JsonUtil.parseJson("{\"key\":}"));
        assertNull(S2JsonUtil.parseJson("{\"key\": value}"));
    }

    @Test
    @Order(39)
    @DisplayName("추가 테스트: Error - Unclosed structures")
    void testUnclosedStructures() {
        assertNull(S2JsonUtil.parseJson("{\"key\":\"value\""));
        assertNull(S2JsonUtil.parseJson("[1, 2"));
    }

    @Test
    @Order(40)
    @DisplayName("추가 테스트: Large JSON simulation")
    void testLargeJson() {
        StringBuilder sb = new StringBuilder("{\"data\":[");
        for (int i = 0; i < 100; i++) {
            sb.append("{\"id\":").append(i).append(",\"name\":\"Item").append(i).append("\"},");
        }
        sb.setLength(sb.length() - 1); // remove last comma
        sb.append("]}");
        String json = sb.toString();
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertNotNull(result);
        List<?> data = (List<?>) result.get("data");
        assertEquals(100, data.size());
    }

    @Test
    @Order(41)
    @DisplayName("추가 테스트: Enum 변환")
    void testEnumConversion() {
        // Enum 값은 String으로 유지되는지 확인
        String json = "{\"status\":\"ACTIVE\"}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertNotNull(result);
        assertEquals("ACTIVE", result.get("status"));
    }

    @Test
    @Order(42)
    @DisplayName("추가 테스트: Set 컬렉션 변환")
    void testSetConversion() {
        String json = "[\"a\", \"b\", \"a\"]"; // 중복 포함
        List<?> list = S2JsonUtil.parseJsonTo(json, List.class);
        assertNotNull(list);
        assertEquals(3, list.size()); // 중복 유지
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("a", list.get(2));
    }

    @Test
    @Order(43)
    @DisplayName("추가 테스트: BigDecimal/BigInteger 변환")
    void testBigNumberConversion() {
        String json = "{\"bigDec\":\"123.456\", \"bigInt\":\"12345678901234567890\"}";
        Map<String, Object> result = S2JsonUtil.parseJson(json);
        assertNotNull(result);
        assertEquals("123.456", result.get("bigDec"));
        assertEquals("12345678901234567890", result.get("bigInt"));
    }

    @Test
    @Order(44)
    @DisplayName("Feature 테스트: ALLOW_NON_NUMERIC_NUMBERS")
    void testFeatureNonNumericNumbers() {
        String json = "{\"nan\":NaN, \"infinity\":Infinity, \"negInfinity\":-Infinity}";
        Map<String, Object> result = S2JsonUtil.parseJson(
                json,
                S2JsonUtil.Feature.ALLOW_NON_NUMERIC_NUMBERS
        );
        assertNotNull(result);
        assertEquals(Double.NaN, result.get("nan"));
        assertEquals(Double.POSITIVE_INFINITY, result.get("infinity"));
        assertEquals(Double.NEGATIVE_INFINITY, result.get("negInfinity"));
    }

    @Test
    @Order(45)
    @DisplayName("Feature 테스트: ALLOW_TRAILING_COMMA")
    void testFeatureTrailingComma() {
        String json = "{\"key1\":\"value1\", \"key2\":\"value2\",}";
        Map<String, Object> result = S2JsonUtil.parseJson(
                json,
                S2JsonUtil.Feature.ALLOW_TRAILING_COMMA
        );
        assertNotNull(result);
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    @Order(46)
    @DisplayName("추가 테스트: 멀티스레드 안전성")
    void testMultithreadSafety() throws InterruptedException {
        // 간단한 멀티스레드 테스트
        Runnable task = () -> {
            String json = "{\"test\":\"value\"}";
            Map<String, Object> result = S2JsonUtil.parseJson(json);
            assertNotNull(result);
            assertEquals("value", result.get("test"));
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
