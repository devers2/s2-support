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

/*
 * [Version Catalog]
 * 의존성 및 플러그인 버전은 'gradle/libs.versions.toml' 파일에서 통합 관리
 * 별도 설정 없이 Gradle이 기본 경로(gradle/libs.versions.toml)를 자동으로 인식하여 'libs' 접근자로 제공
 */
plugins {
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.s2.build.support)

    /*
     * ⭐ [Shadow / Relocation 스위치 — 이 alias와 아래의 shadedPackagePrefix는 반드시 함께 켜고 꺼야 한다]
     * 이 alias만 단독으로 켜면 S2BuildUtils는 이를 사용하지 않는다 (shadowJar 태스크는 등록되지만
     * assemble/publish 파이프라인과 무관하게 방치됨). shadedPackagePrefix까지 함께 설정해야만
     * S2BuildUtils가 shadowJar를 실제 패키징에 편입시킨다.
     *
     * - 둘 다 꺼짐(기본값, 지금 상태): 배포 시 Standard JAR(의존성 미포함, POM으로 전이),
     *   로컬 빌드 시 Fat JAR(runtimeClasspath 전체 병합, relocation 없음) — Shadow 플러그인 미관여.
     * - 둘 다 켜짐: 배포 시 Shaded JAR(implementation/runtimeOnly는 relocate되어 JAR에 포함,
     *   api는 JAR에서 빠지고 POM에만 compile scope로 추가됨),
     *   로컬 빌드 시 Relocated Fat JAR(api 포함 전체를 담되 api를 제외한 나머지만 relocate) — shadowJar 사용.
     *
     * 자세한 4가지 조합 표는 S2BuildUtils 클래스 최상단 Javadoc(Packaging Strategies) 참고.
     */
    // alias(libs.plugins.shadow)
}

import io.github.devers2.buildsupport.S2BuildUtils

/*
 * Maven 좌표 설정
 * - group: 프로젝트의 그룹 ID (예: io.github.devers2, com.example)
 * - version: 프로젝트 버전
 *
 * ❗중요: 동일한 소스를 다른 조직/목적으로 배포하는 경우 group을 다르게 설정해야 한다.
 *   예시) 원본: io.github.devers2, 포크: com.company
 *   이렇게 하면 의존성 관리 도구가 서로 다른 아티팩트로 인식하여 같은 리포지토리라도 별도 아티팩트로 취급된다.
 */
group = "io.github.devers2.internal"
version = "1.1.3"

// Shadow Plugin - Relocation 패키지 설정
// ⚠️ 이 값을 설정해도 위 plugins{} 블록의 shadow alias가 함께 켜져 있지 않으면 아무 효과가 없다 (둘 다 켜야 함).
// extra["shadedPackagePrefix"] = "io.github.devers2.s2util.shaded"

// ========================================================================
// ⭐ [사용자 설정 (User Configuration)]
// 개발자가 프로젝트 상황에 맞춰 자주 변경하거나 확인해야 하는 설정
// ========================================================================

/**
 * 기본 Java 버전 설정: JavaVersion.current() 또는 JavaVersion.VERSION_17, JavaVersion.VERSION_25 등 설정 가능
 * (JavaVersion.current(): 현재 실행 중인 JVM 버전)
 *
 * Java 버전에 따른 의존성 설정
 * JavaVersion.VERSION_10 이하: javax.servlet
 * JavaVersion.VERSION_11 이상: jakarta.servlet
 *
 * Jakarta EE 버전   서블릿 버전     패키지 명칭        일반적으로 사용되는 Java 버전
 * -----------------------------------------------------------------------------
 * Jakarta EE 8      Servlet 4.0    javax.servlet      Java 8, Java 11 등
 * Jakarta EE 9      Servlet 5.0    jakarta.servlet    Java 11, Java 17 등
 * Jakarta EE 10     Servlet 6.0    jakarta.servlet    Java 17, Java 21 등
 */
extra["javaVersion"] = JavaVersion.VERSION_21

/*
 * [추가 소스 목록]
 * dynamicSourceInfoMap에 정의된 기능 키(예: 'S2PdfUtil')를 추가하여 관련된 소스 파일 및 라이브러리 의존성을 빌드에 자동으로 포함시킬 수 있다.
 */
extra["activeFeatures"] = setOf("licensesInfo")

/**
 * [동적 기능 소스 정보 (Feature Toggles)]
 * - 특정 기능(Feature)에 포함될 소스 파일과 라이선스 정보 정의
 */
extra["dynamicSourceInfoMap"] = mapOf(
    "licensesInfo" to mapOf(
        "licenses" to listOf(
            "README.md",
            "LICENSE",
            "licenses/LICENSE-APACHE-2.0",
            "licenses/LICENSE-EPL-2.0",
            "licenses/LICENSE-LGPL-2.1",
            "licenses/LICENSE-MIT",
            "licenses/LICENSE-MPL-2.0",
            "licenses/LICENSE-JSCH-BSD",
            "licenses/NOTICE"
        )
    ),
    "S2PdfUtil" to mapOf(
        /*
         * OpenHTML to PDF (LGPL 2.1) - S2Pdf 관련 의존성으로 LGPL 2.1 라이선스 준수를 위해 compileOnly로 사용(Shadow/Bundle 방지)
         * 최종 사용자가 의존성을 직접 추가해야 하며 Shadow JAR에서 쉐이딩/번들링 되지 않도록 방지해야 함
         */
        "variantId" to "pdf",
        "sources" to listOf("io/github/devers2/s2util/support/S2PdfUtil.java"),
        "dependencies" to listOf(
            mapOf( // (jsoup은 MIT이지만 openhtmltopdf와 함께 동작하므로 동일하게 처리)
                "configuration" to "compileOnly",
                "group" to "org.jsoup",
                "name" to "jsoup",
                "version" to "1.18.3"
            ),
            mapOf( // LGPL 2.1 라이선스
                "configuration" to "compileOnly",
                "group" to "io.github.openhtmltopdf",
                "name" to "openhtmltopdf-core",
                "version" to "1.1.24"
            ),
            mapOf( // LGPL 2.1 라이선스
                "configuration" to "compileOnly",
                "group" to "io.github.openhtmltopdf",
                "name" to "openhtmltopdf-pdfbox",
                "version" to "1.1.24"
            )
        ),
        "licenses" to listOf("README-LGPL-2.1-PDF.md")
    )
)

/*
 * 기본 제외 소스 목록 (항상 제외되는 파일들)
 */
extra["excludedSources"] = emptySet<String>()

/*
 * 빌드 완료 후 결과물을 테스트할 클래스 지정 (shadowJar 실행 후 testArtifact 태스크로 수행됨, 여러 개 지정 가능)
 */
extra["artifactTestClassNames"] = emptyList<String>()

/**
 * [기준 Java 버전: Artifact ID 생성 기준]
 * javaVersion이 baselineJavaVersion과 다를 경우 아티팩트 ID에 접미사 추가
 */
extra["baselineJavaVersion"] = JavaVersion.VERSION_21

/**
 * 안전한 태스크 목록 (로컬 빌드/테스트용)
 * - 이 태스크 실행 시에는 소스 JAR를 생성해도 안전하다고 판단
 */
extra["safeTasks"] = setOf("assemble", "build", "jar", "sourcesJar", "publishToMavenLocal")


// ========================================================================
// ⭐ [상수 및 환경 설정 (Constants & Environment)]
// 프로젝트 구조나 외부 환경과 관련된 설정 (변경 빈도 낮음)
// ========================================================================

val javaSrcRoot = "src/main/java"
val resourcesSrcRoot = "src/main/resources"
extra["JAVA_SRC_ROOT"] = javaSrcRoot
extra["RESOURCES_SRC_ROOT"] = resourcesSrcRoot


// ========================================================================
// ⭐ [빌드 로직 (Build Logic & Calculations)]
// 위 설정값들을 기반으로 실제 빌드에 필요한 값 계산 및 유틸리티 호출
// ※ 특별한 이유가 없다면 수동 수정 지양
// ========================================================================

/*
 * [Java 버전 오버라이드]
 * -PtargetJavaVersion=21 옵션으로 덮어쓰기 가능
 */
if (project.hasProperty("targetJavaVersion")) {
    extra["javaVersion"] = JavaVersion.toVersion(project.findProperty("targetJavaVersion")!!)
}

/*
 * [동적 기능 모듈 오버라이드]
 * -PtargetSources=S2PdfUtil,OtherFeature 옵션으로 덮어쓰기 가능
 */
if (project.hasProperty("targetSources")) {
    val targetSourcesVal = project.findProperty("targetSources")
    val sources = targetSourcesVal.toString().split(",")
    extra["activeFeatures"] = sources.map { it.trim() }.toSet()
}


// --------------------------------------------------------------------------------------
// [Dynamic Artifact ID 설정] 기본 Java 버전과 다르거나 추가 소스가 있는 경우 접미사 추가
// --------------------------------------------------------------------------------------
var artifactSuffix = ""

// 1. Java 버전 체크
@Suppress("UNCHECKED_CAST")
val projectJavaVersion = extra["javaVersion"] as JavaVersion

@Suppress("UNCHECKED_CAST")
val projectBaselineJavaVersion = extra["baselineJavaVersion"] as JavaVersion

if (projectJavaVersion != projectBaselineJavaVersion) {
    // Java MAJOR 버전만 추출 (예: 1.8 -> 8, 11 -> 11)
    artifactSuffix += "-java${projectJavaVersion.majorVersion}"
}

// 2. 추가 소스 체크 (variantId 사용)
@Suppress("UNCHECKED_CAST")
val projectActiveFeatures = extra["activeFeatures"] as Set<String>

@Suppress("UNCHECKED_CAST")
val projectDynamicSourceInfoMap = extra["dynamicSourceInfoMap"] as Map<String, Map<String, Any>>

projectActiveFeatures.forEach { srcName ->
    val vId = projectDynamicSourceInfoMap[srcName]?.get("variantId") as String?
    if (vId != null) {
        artifactSuffix += "-$vId"
    }
}

extra["globalArtifactSuffix"] = artifactSuffix


// ========================================================================
// ⭐ [단일 프로젝트 구성]
// ========================================================================

base {
    // archivesName 업데이트 (접미사가 있는 경우만)
    archivesName.set("${project.name}${artifactSuffix}")
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java.setSrcDirs(listOf(javaSrcRoot))
        resources.setSrcDirs(listOf(resourcesSrcRoot))
    }
}

// 프로젝트 통합 설정
S2BuildUtils.configureProject(project)

// 저작권 연도 업데이트 (수정이 필요한 경우에만 파일 IO 발생)
S2BuildUtils.updateCopyright(
    project,
    arrayOf(
        javaSrcRoot,
        "src/main/kotlin",
        "src/main/python",
        resourcesSrcRoot,
        "src/main/webapp",
        "README.md",
        "build.gradle.kts"
    )
)

/**
 * 컴파일러 옵션 설정
 */
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // 파라미터명 정상적으로 보이도록 수정
    options.compilerArgs.add("-parameters")
    /**
     * --release 옵션의 제약을 해제하고, 구형 방식인 -source 및 -target 설정을 강제로 사용
     * Java 21로 컴파일 하고 결과물을 Java 17으로 실행할 수 있도록 함
     */
    options.release.set(null as Int?)
}

/**
 * [JAR 패키징 명시적 필터링]
 * S2BuildUtils를 통해 자동 관리되지만, activeFeatures 설정에 따라
 * 특정 라이선스 파일이 중복 포함되는 것을 방지하기 위해 명시적으로 exclude 처리한다.
 */
tasks.withType<Jar>().configureEach {
    if (!projectActiveFeatures.contains("S2PdfUtil")) {
        exclude("README-LGPL-2.1-PDF.md")
    }
}

/*
 * [라이브러리 배포 아티팩트 및 Java 호환성 설정]
 *
 * 표준 배포 파일(Artifacts) 생성과 Java 호환성 레벨 정의
 *
 * 1. 배포 아티팩트:
 * - withSourcesJar(): 소스 파일(*-sources.jar) 생성
 * - withJavadocJar(): Javadoc 문서(*-javadoc.jar) 생성
 *
 * 2. IDE 활용:
 * - 메인 JAR와 함께 sources/javadoc JAR 배포 시 IDE가 자동 감지하여 연결
 * - assemble 또는 publish 계열 태스크로 생성됨
 *
 * 3. 호환성 설정:
 * - sourceCompatibility: 소스 코드 레벨
 * - targetCompatibility: 바이트코드(.class) 실행 레벨
 *
 * ★★★ 'assemble' 실행 시 모든 주요 아티팩트 생성 ★★★
 *
 * ※ Javadoc 작성 시 HTML 태그 주의 (<pre> 같은 실제 태그는 사용 가능)
 *   - 금지: <, >, &, {, }, @
 *   - 대체: List<String> → {@code List<String>}
 */
java {
    withJavadocJar()
    if (S2BuildUtils.determineSourceJarStatus(project)) {
        withSourcesJar()
    }

    /**
     * [Java Toolchain]
     * 빌드 실행 환경(JAVA_HOME)과 프로젝트 컴파일 환경을 분리하는 현대적인 방식
     * 1. 일관성: 팀원 모두가 동일한 JDK 버전으로 빌드하도록 강제
     * 2. 자동화: 로컬에 해당 JDK가 없으면 설정된 리졸버(Foojay 등)를 통해 자동 다운로드
     * 3. 유연성: Gradle은 Java 17로 실행하면서, 프로젝트는 Java 21로 컴파일하는 등의 설정이 가능
     */
    toolchain {
        // extra["javaVersion"] (JavaVersion 타입)에서 숫자 버전만 추출하여 설정함
        languageVersion.set(JavaLanguageVersion.of(projectJavaVersion.majorVersion.toInt()))
    }

    // --release 옵션을 제거하여 이 설정들이 컴파일러 인자(-source, -target)로 확실히 전달된다. (Java 17으로 실행할 수 있도록 함)
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            // 생성될 pom.xml 상세 설정 (Maven Central 필수 요건)
            pom {
                name = project.name
                description = "S2Util Library - A comprehensive utility library for Java"
                url = "https://github.com/devers2/s2-util"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "devers2"
                        name = "이승수"
                        email = "eseungsu.dev@gmail.com"
                        organization = "devers2"
                        organizationUrl = "https://github.com/devers2"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/devers2/s2-util.git"
                    developerConnection = "scm:git:ssh://github.com/devers2/s2-util.git"
                    url = "https://github.com/devers2/s2-util"
                }
            }
        }
    }
}

// 배포 리포지토리 설정 (S2BuildUtils 공통 로직 재사용 - CentralPortal 등록 + 서명 필수화까지 자동 처리됨)
// [참고] GitHub Packages(s2-packages) 배포가 다시 필요해지면 아래 한 줄만 추가하면 된다:
//   S2BuildUtils.configureGitHubPackagesRepository(project, "devers2", "s2-util")
S2BuildUtils.configureCentralPortalRepository(project)

tasks.named<Test>("test") {
    /**
     * JUnit 5(Jupiter) 플랫폼 사용 설정.
     * Gradle은 기본적으로 JUnit 4를 사용하려 하므로, JUnit 5 테스트를 실행하려면 이 설정이 필수이다.
     */
    useJUnitPlatform()
    // JVM 인코딩 설정 (테스트 환경에서 한글 깨짐 방지)
    jvmArgs("-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8")
}

// 'Tasks → other → copyDependencies' 실행 시 지정 디렉토리로 의존성 복사
S2BuildUtils.registerCopyDependenciesTask(project)


// --------------------------------------------------------------------------------------
// README 파일 버전 & 의존성 가이드 업데이트
// --------------------------------------------------------------------------------------
S2BuildUtils.updateReadmeWithVersionAndDependencies(project, file("README.md"))


// ========================================================================
// ⭐ [의존성 설정]
// ========================================================================

dependencies {
    /**
     * implementation: 컴파일 및 런타임 시 모두 사용함
     * - 해당 의존성이 프로젝트의 빌드 결과물(JAR)에 직접적인 영향을 미침
     * - 이 라이브러리를 사용하는 다른 프로젝트(상위 모듈)에는 의존성이 노출되지 않음 (API 캡슐화)
     * - 런타임에 반드시 필요한 라이브러리인 경우 이 방식을 사용함
     */
    implementation(libs.jsch) // SFTP
    implementation(libs.commons.pool2) // Object Pooling

    // LZ4 압축 알고리즘: xxHash 사용을 위해 추가
    implementation(libs.lz4)

    /*
     * 표준 UUID 생성 라이브러리 (v1~v7 지원, RFC 9562)
     * v4: 완전 무작위 (java.util.UUID 호환)
     * v7: 시간순 정렬 가능 + 난수 (DB 성능/보안 최적)
     * v1: 시간순 정렬 가능하나 보안(MAC 노출) 취약 미사용 권장
     */
    implementation(libs.uuid.creator)

    /**
     * compileOnly: 컴파일 시 사용함
     * - 런타임 시 사용하지 않는 라이브러리인 경우 이 방식을 사용함
     * - Shadow JAR 생성 시 포함되지 않음
     */
    compileOnly(libs.s2.core)
    compileOnly(libs.spring6.context) // Java 17 이상으로 개발하므로 Spring 6 및 Spring Boot 3 계열이 표준
    compileOnly(libs.spring6.web) // Java 17 이상으로 개발하므로 Spring 6 및 Spring Boot 3 계열이 표준
    compileOnly(libs.spring.integration.sftp)
    compileOnly(libs.jakarta.servlet.api) // Java 17 이상으로 개발하므로 jakarta 가 표준
    compileOnly(libs.jakarta.servlet.jsp) // Java 17 이상으로 개발하므로 jakarta 가 표준
    compileOnly(libs.aspectj.weaver)

    /**
     * testImplementation: 테스트 컴파일 및 런타임 시 모두 사용
     * - 테스트 코드에서 사용하는 라이브러리인 경우 이 방식을 사용
     */
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.s2.core)
}
