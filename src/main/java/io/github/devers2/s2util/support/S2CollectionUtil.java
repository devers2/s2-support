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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import io.github.devers2.s2util.core.S2Util;

/**
 * s2's utilities
 *
 * @author devers2
 * @version 1.0
 * @since 2025. 05. 27.
 */
public class S2CollectionUtil {

    /**
     * Map 또는 VO 객체의 List 에서 조건에 맞는 Index 를 반환한다. (1개 이상의 객체가 조건에 맞는다면 첫번째 객체의 Index 를 반환)
     *
     * @param <K>        조건 키의 타입
     * @param <V>        조건 값의 타입
     * @param <T>        리스트 요소의 타입
     * @param list       (Map 또는 VO 객체의 List)
     * @param conditions 조건(가변인자)
     * @return Index
     * @implNote
     *
     *           <pre>{@code
     * int findIdx = S2Util.listFindIndex(list, Map.entry("typeA", "a"), Map.entry("typeB", 3));
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public static <K, V, T> int listFindIndex(List<T> list, Entry<K, V>... conditions) {
        if (S2Util.isEmpty(list) || S2Util.isEmpty(conditions)) {
            return -1;
        }

        if (Arrays.stream(conditions).anyMatch(c -> S2Util.isEmpty(c.getKey()) || S2Util.isEmpty(c.getValue()))) {
            return -1;
        }

        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            boolean allConditionsMet = true;

            for (Entry<K, V> condition : conditions) {
                if (!Objects.equals(condition.getValue(), S2Util.getValue(item, condition.getKey()))) {
                    allConditionsMet = false;
                    break;
                }
            }

            if (allConditionsMet) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Map 또는 VO 객체의 List 에서 조건에 맞는 객체 목록을 반환한다.
     *
     * @param <K>        조건 키의 타입
     * @param <V>        조건 값의 타입
     * @param <T>        리스트 요소의 타입
     * @param list       (Map 또는 VO 객체의 List)
     * @param conditions 조건(가변인자)
     * @return 객체 목록
     * @implNote
     *
     *           <pre>{@code
     * List<Map < String, Object>> filterList = (List<Map<String, Object>>);
     * S2Util.listFilter(testList, Map.entry("typeA", "a"), Map.entry("typeB", 3));
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public static <K, V, T> List<T> listFilter(List<T> list, Entry<K, V>... conditions) {
        if (S2Util.isEmpty(list) || S2Util.isEmpty(conditions)) {
            return new ArrayList<>();
        }

        List<T> resultList = new ArrayList<>();
        for (T item : list) {
            boolean allConditionsMet = true;
            for (Entry<K, V> condition : conditions) {
                boolean currentConditionMet = S2Util.isNotEmpty(condition.getKey())
                        && S2Util.isNotEmpty(condition.getValue())
                        && Objects.equals(S2Util.getValue(item, condition.getKey()), condition.getValue());

                if (!currentConditionMet) {
                    allConditionsMet = false;
                    break;
                }
            }

            if (allConditionsMet) {
                resultList.add(item);
            }
        }
        return resultList;
    }

    /**
     * Map 또는 VO 객체의 목록을 정렬하여 반환한다.
     *
     * @param <V>       정렬기준 필드의 타입
     * @param <T>       리스트 요소의 타입
     * @param list      (Map 또는 VO 객체의 List)
     * @param fieldName 정렬기준 필드명(VO) 또는 Key(Map)
     * @param orderBy   정렬순서("DESC", "ASC")
     * @return 정렬된 객체 목록
     * @implNote
     *
     *           <pre>{@code
     * S2Util.listSort(testList, "key", "ASC");
     * }</pre>
     */
    public static <V, T> List<T> listSort(List<T> list, V fieldName, String orderBy) {
        if (S2Util.isEmpty(list) || S2Util.isEmpty(fieldName) || orderBy == null || orderBy.isBlank() ||
                (!"DESC".equalsIgnoreCase(orderBy) && !"ASC".equalsIgnoreCase(orderBy))) {
            return list;
        }

        Comparator<T> comparator = (a, b) -> {
            Object va = S2Util.getValue(a, fieldName);
            Object vb = S2Util.getValue(b, fieldName);

            if (va == null && vb == null) {
                return 0;
            }
            if (va == null) {
                return -1;
            }
            if (vb == null) {
                return 1;
            }

            // 숫자끼리 비교 (정밀도 위해 BigDecimal 사용)
            if (va instanceof Number && vb instanceof Number) {
                BigDecimal na = new BigDecimal(String.valueOf(va));
                BigDecimal nb = new BigDecimal(String.valueOf(vb));
                return na.compareTo(nb);
            }

            // 날짜 비교 (java.util.Date 기준)
            if (va instanceof Date && vb instanceof Date) {
                return ((Date) va).compareTo((Date) vb);
            }

            // Comparable 처리 (제네릭 경고 회피 및 타입 불일치 시 문자열 폴백)
            if (va instanceof Comparable && vb instanceof Comparable) {
                try {
                    @SuppressWarnings("unchecked")
                    Comparable<Object> ca = (Comparable<Object>) va;
                    return ca.compareTo(vb);
                } catch (ClassCastException e) {
                    return String.valueOf(va).compareTo(String.valueOf(vb));
                }
            }

            // 그 외 문자열 기반 비교
            return String.valueOf(va).compareTo(String.valueOf(vb));
        };

        if ("DESC".equalsIgnoreCase(orderBy)) {
            list.sort(comparator.reversed());
        } else {
            list.sort(comparator);
        }
        return list;
    }

}
