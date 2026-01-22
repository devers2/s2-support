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
package io.github.devers2.s2util.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.github.devers2.s2util.support.S2Template;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.TypedQuery;

/**
 * S2Template의 동적 쿼리 생성 기능과 JPA TypedQuery 생성을 통합한 빌더 클래스입니다.
 * 메서드 체이닝을 통해 파라미터를 설정하고 최종적으로 실행 가능한 TypedQuery를 반환합니다.
 */
public class S2Jpql<T> extends S2Template {

    private final EntityManager em;
    private final Class<T> resultClass;
    private final Map<String, Object> parameters;

    private S2Jpql(String sql, EntityManager em, Class<T> resultClass) {
        super(sql);
        this.em = Objects.requireNonNull(em, "EntityManager must not be null");
        this.resultClass = Objects.requireNonNull(resultClass, "Result class must not be null");
        this.parameters = new HashMap<>();
    }

    /**
     * S2Jpql 빌더 인스턴스를 생성합니다.
     *
     * @param em          JPA 엔티티 매니저
     * @param sql         치환자가 포함된 기본 JPQL 문자열
     * @param resultClass 결과 엔티티 클래스
     * @return S2Jpql 빌더 객체
     */
    public static <T> S2Jpql<T> of(EntityManager em, String sql, Class<T> resultClass) {
        return new S2Jpql<>(sql, em, resultClass);
    }

    /**
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key    템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param name   파라미터 이름 (예: "name")
     * @param value  파라미터 값
     * @param clause 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return 메서드 체이닝을 위한 현재 객체
     * @param prefix 쿼리 절({@code clause}) 앞에 붙을 문자열
     * @param suffix 쿼리 절({@code clause}) 뒤에 붙을 문자열
     * @return 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, boolean condition, String name, Object value, Object clause, String prefix, String suffix) {
        super.bindWhen(key, condition, clause, prefix, suffix);
        parameters.put(name, value);
        return this;
    }

    /**
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key    템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param name   파라미터 이름 (예: "name")
     * @param value  파라미터 값
     * @param clause 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return 메서드 체이닝을 위한 현재 객체
     * @param prefix 쿼리 절({@code clause}) 앞에 붙을 문자열
     * @return 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, boolean condition, String name, Object value, Object clause, String prefix) {
        super.bindWhen(key, condition, clause, prefix);
        parameters.put(name, value);
        return this;
    }

    /**
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key    템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param name   파라미터 이름 (예: "name")
     * @param value  파라미터 값
     * @param clause 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, boolean condition, String name, Object value, Object clause) {
        super.bindWhen(key, condition, clause);
        parameters.put(name, value);
        return this;
    }

    /**
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key    템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param name   파라미터 이름 (예: "name")
     * @param value  파라미터 값
     * @param clause 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return 메서드 체이닝을 위한 현재 객체
     * @param prefix 쿼리 절({@code clause}) 앞에 붙을 문자열
     * @param suffix 쿼리 절({@code clause}) 뒤에 붙을 문자열
     * @return 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, String name, Object value, Object clause, String prefix, String suffix) {
        super.bindWhen(key, value, clause, prefix, suffix);
        parameters.put(name, value);
        return this;
    }

    /**
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key    템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param name   파라미터 이름 (예: "name")
     * @param value  파라미터 값
     * @param clause 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return 메서드 체이닝을 위한 현재 객체
     * @param prefix 쿼리 절({@code clause}) 앞에 붙을 문자열
     * @return 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, String name, Object value, Object clause, String prefix) {
        super.bindWhen(key, value, clause, prefix);
        parameters.put(name, value);
        return this;
    }

    /**
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key    템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param name   파라미터 이름 (예: "name")
     * @param value  파라미터 값
     * @param clause 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, String name, Object value, Object clause) {
        super.bindWhen(key, value, clause);
        parameters.put(name, value);
        return this;
    }

    /**
     * 지금까지 설정된 내용을 바탕으로 파라미터 바인딩이 완료된 TypedQuery를 생성합니다.
     *
     * @return 실행 가능한 TypedQuery 객체
     */
    public TypedQuery<T> build() {
        // 1. 템플릿 렌더링
        String renderedSql = super.render();
        TypedQuery<T> query = em.createQuery(renderedSql, resultClass);

        // 2. 자동 바인딩 (S2Template의 rawValues 활용)
        for (Parameter<?> param : query.getParameters()) {
            String name = param.getName();
            if (name != null && parameters.containsKey(name)) {
                query.setParameter(name, parameters.get(name));
            }
        }

        return query;
    }

}
