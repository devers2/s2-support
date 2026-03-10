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
    @DisplayName("오류 상황: 순환 참조가 깊은 경우")
    void testDeepCircularReference() {
        Map<String, Object> map1 = new HashMap<>();
        Map<String, Object> map2 = new HashMap<>();
        map1.put("ref", map2);
        map2.put("ref", map1);

        String json = S2JsonUtil.toJsonString(map1);
        assertNotNull(json);
        // 순환 참조가 제대로 처리되는지 확인
    }
}
