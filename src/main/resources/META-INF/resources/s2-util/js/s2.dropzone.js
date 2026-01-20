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

/**
 * 드롭영역
 * @author devers2
 * @since  2025
 * @version 1.0
 * @see
 * Copyright (C)  All right reserved.
 */
export class S2DropZone {
  /**
   * DropZone 생성자
   *
   * @param {Object} options - 드롭존 설정 옵션
   * @param {Element|string} options.dropZone - 드롭존으로 사용할 DOM 요소 또는 선택자
   * @param {Function} [options.dragenter] - 드래그 엔터 이벤트 발생 시 호출될 콜백 함수
   * @param {Function} [options.dragleave] - 드래그 리브 이벤트 발생 시 호출될 콜백 함수
   * @param {Function} [options.dragover] - 드래그 오버 이벤트 발생 시 호출될 콜백 함수
   * @param {Function} [options.drop] - 파일이 드롭됐을 때 호출될 콜백 함수
   *
   * @example
   * import { S2DropZone } from './js/s2.dropzone.js'; // Thymeleaf 는 '[[@{/js/s2.dropzone.js}]]' 형태로 경로 지정
   *
   * const dropZone = new S2DropZone({
   *     dropZone: document.querySelector('.dropZone'),
   *     dragenter: function (event, dropZoneElement) {
   *         // 드래그 엔터 이벤트 발생 시 호출
   *         // event: 드래그 이벤트 객체, dropZoneElement: 드롭존 DOM 요소 (dropZone)
   *     },
   *     dragover: function (event, dropZoneElement) {
   *         // 드래그 오버 이벤트 발생 시 호출
   *         // event: 드래그 이벤트 객체, dropZoneElement: 드롭존 DOM 요소 (dropZone)
   *     },
   *     dragleave: function (event, dropZoneElement) {
   *         // 드래그 리브 이벤트 발생 시 호출
   *         // event: 드래그 이벤트 객체, dropZoneElement: 드롭존 DOM 요소 (dropZone)
   *     },
   *     drop: function (event, dropZoneElement, dropFiles) {
   *         // 드롭 이벤트 발생 시 호출
   *         // event: 드래그 이벤트 객체, dropZoneElement: 드롭존 DOM 요소 (dropZone)
   *         // dropFiles: 드롭된 파일 목록 (File 객체 배열)
   *     }
   * });
   */
  constructor(options = {}) {
    let dropZone = options.dropZone;

    // 문자열 선택자인 경우 DOM 요소로 변환
    if (typeof dropZone === 'string') {
      dropZone = document.querySelector(dropZone);
    }

    if (!dropZone) {
      console.error('유효하지 않은 dropZone 입니다.');
      return;
    }

    this.dropZone = dropZone;
    this.options = options;
    this.enableDrop = true;

    // 이벤트 리스너 등록
    this.setupEventListeners();
  }

  /**
   * 드롭존 활성화
   */
  enable() {
    this.enableDrop = true;
  }

  /**
   * 드롭존 비활성화
   */
  disable() {
    this.enableDrop = false;
  }

  /**
   * 이벤트 리스너 설정
   * @private
   */
  setupEventListeners() {
    const self = this;

    if (!self.options) {
      return;
    }

    // dragenter 이벤트
    this.dropZone.addEventListener('dragenter', function (e) {
      if (self.enableDrop === false) return;

      e.stopPropagation();
      e.preventDefault();

      this.classList.remove('dragover');

      // 사용자 정의 dragenter 콜백 호출
      if (self.options.dragenter && typeof self.options.dragenter === 'function') {
        self.options.dragenter(e, this);
      }
    });

    // dragleave 이벤트
    this.dropZone.addEventListener('dragleave', function (e) {
      if (self.enableDrop === false) return;

      e.stopPropagation();
      e.preventDefault();

      this.classList.remove('dragover');

      // 사용자 정의 dragleave 콜백 호출
      if (self.options.dragleave && typeof self.options.dragleave === 'function') {
        self.options.dragleave(e, this);
      }
    });

    // dragover 이벤트
    this.dropZone.addEventListener('dragover', function (e) {
      if (self.enableDrop === false) return;

      e.stopPropagation();
      e.preventDefault();

      this.classList.add('dragover');

      // 사용자 정의 dragover 콜백 호출
      if (self.options.dragover && typeof self.options.dragover === 'function') {
        self.options.dragover(e, this);
      }
    });

    // drop 이벤트
    this.dropZone.addEventListener('drop', function (e) {
      if (self.enableDrop === false) return;

      e.stopPropagation();
      e.preventDefault();

      this.classList.remove('dragover');

      const dropFiles = [];

      // 파일 처리
      if (e.dataTransfer.items) {
        // DataTransferItemList 인터페이스 사용
        Array.from(e.dataTransfer.items).forEach((item) => {
          if (item.kind === 'file') {
            const file = item.getAsFile();
            if (file) {
              dropFiles.push(file);
            }
          }
        });
      } else {
        // 구형 브라우저 지원
        Array.from(e.dataTransfer.files).forEach((file) => {
          dropFiles.push(file);
        });
      }

      // 사용자 정의 drop 콜백 호출
      if (self.options.drop && typeof self.options.drop === 'function') {
        self.options.drop(e, this, dropFiles);
      }
    });
  }
}
