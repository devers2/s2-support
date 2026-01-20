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

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import io.github.devers2.s2util.core.S2Util;

/**
 * Spring 프레임워크 컨텍스트 환경에서 다양한 편의 기능을 제공하는 유틸리티 클래스.
 *
 * @author devers2
 * @since 2025. 12. 20
 */
public final class S2ContextUtil {

    private S2ContextUtil() {
        // Prevent instantiation
    }

    /**
     * AOP JoinPoint에서 파라미터명 또는 객체 내부 필드명과 일치하는 값을 추출한다.
     * <p>
     * <b>검색 우선순위:</b>
     * <ol>
     * <li>파라미터 이름과 일치하는 아규먼트 값</li>
     * <li>아규먼트가 객체(VO, Map)일 경우, 해당 객체 내부의 필드 값 ({@link S2Util#getValue(Object, Object)} 활용)</li>
     * </ol>
     * </p>
     *
     * @param joinPoint        AspectJ JoinPoint 객체
     * @param schParameterName 찾고자 하는 파라미터명 또는 VO 내 필드명
     * @return 추출된 값 (없을 경우 null)
     */
    public static Object getJoinPointParameter(final JoinPoint joinPoint, final String schParameterName) {
        return getJoinPointParameter(joinPoint, null, schParameterName);
    }

    /**
     * AOP JoinPoint에서 특정 파라미터 값 또는 파라미터 객체 내부의 필드 값을 추출한다.
     * <p>
     * <b>검색 우선순위:</b>
     * <ol>
     * <li>파라미터 이름과 일치하는 아규먼트 값</li>
     * <li>아규먼트가 객체(VO, Map)일 경우, 해당 객체 내부의 필드 값 ({@link S2Util#getValue(Object, Object)} 활용)</li>
     * </ol>
     * </p>
     *
     * @param <T>              VO 클래스 타입
     * @param joinPoint        AOP JoinPoint 객체
     * @param voClass          검사할 VO 클래스 타입 (null이 아닐 경우, 해당 타입의 객체만 내부 필드를 검색함)
     * @param schParameterName 찾고자 하는 파라미터 또는 필드명
     * @return 찾은 값 (없으면 null)
     */
    public static <T> Object getJoinPointParameter(final JoinPoint joinPoint, Class<T> voClass, final String schParameterName) {
        if (joinPoint == null || schParameterName == null) {
            return null;
        }

        final var signature = (MethodSignature) joinPoint.getSignature();
        final var parameterNames = signature.getParameterNames();
        final var arguments = joinPoint.getArgs();

        if (parameterNames == null || arguments == null) {
            return null;
        }

        Object resultValue = null;

        for (var i = 0; i < parameterNames.length; i++) {
            var paramName = parameterNames[i];
            var argument = arguments[i];

            // 1. 파라미터 이름 자체가 찾으려는 이름과 같은 경우 (최우선)
            // 예: method(String userId) -> "userId" 검색 시 바로 반환
            if (schParameterName.equals(paramName)) {
                if (S2Util.isNotEmpty(argument)) {
                    return argument;
                }
            }

            // 2. 객체 내부 필드 검색 (Deep Search)
            // argument가 null이 아니고, voClass 제약이 없거나 해당 타입인 경우에만 검색
            if (argument != null && (voClass == null || voClass.isInstance(argument))) {

                // [성능 개선]
                // 기존에는 getValueAll로 모든 필드를 뒤졌지만,
                // 이제는 캐싱된 MethodHandle을 사용하는 getValue로 핀포인트 조회합니다.
                var deepValue = S2Util.getValue(argument, schParameterName);

                if (S2Util.isNotEmpty(deepValue)) {
                    resultValue = deepValue;
                    // 값을 찾았으면 루프 종료 (우선순위에 따라 첫 번째 발견 값 반환)
                    break;
                }
            }
        }

        return resultValue;
    }

}
