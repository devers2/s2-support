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
import { S2Util } from './s2.util.js';

let g_interval;

/**
 * 프로그레스 바의 애니메이션 진행을 제어합니다.
 * @private
 * @param {number} timeout - 인터벌 시간 (밀리초)
 */
const controlProgressBar = (timeout) => {
  let progress = 0;
  g_interval = setInterval(
    function () {
      if (progress >= 98) {
        clearInterval(g_interval);
        return;
      }
      setProgressBar(++progress);
    },
    timeout && !isNaN(timeout) ? timeout : 1200
  );
};

/**
 * 프로그레스 바의 너비와 텍스트를 업데이트합니다.
 * @private
 * @param {number} percent - 진행률 (0-100)
 */
const setProgressBar = (percent) => {
  const progressBar = document.querySelector('.progress-bar');
  const progressPercent = document.querySelector('.progress-text');
  if (progressBar) progressBar.style.width = percent + '%';
  if (progressPercent) progressPercent.textContent = percent + '%';
};

/**
 * 로딩 페이지 오버레이를 화면에 표시합니다.
 * 옵션에 따라 프로그레스 바를 표시하고 애니메이션을 시작합니다.
 *
 * @param {Object} [options] - 로딩 설정 옵션
 * @param {boolean} [options.showOverlay=false] - 오버레이 표시 여부
 * @param {boolean} [options.showProgress=false] - 프로그레스 바 표시 여부
 * @param {number} [options.timeout=1200] - 프로그레스 바 진행 속도 (인터벌 시간, 기본 1200ms)
 * @returns {void}
 *
 * @example
 * import { showS2Loading, hideS2Loading } from './js/s2.loading.js'; // Thymeleaf 는 '[[@{/js/s2.loading.js}]]' 형태로 경로 지정
 *
 * ex1) 프로그레스 바 없이 로딩 표시
 * showS2Loading();
 * ex2) 프로그레스 바를 표시하며 로딩 시작
 * showS2Loading({ showProgress: true, timeout: 800 });
 */
export const showS2Loading = (options) => {
  // 기존에 존재하는 로딩 오버레이 제거
  hideS2Loading();

  const loadingHtml = `
        <div class="s2-loading-overlay">
            <div class="loader-container">
                <div class="hexagon-wrap">
                    <div>
                        <div class="hexagon hex1 blue">
                            <div class="inner-line"></div>
                        </div>
                    </div>
                    <div>
                        <div class="hexagon hex2 gray">
                            <div class="inner-line"></div>
                            <div class="hexagon-line"></div>
                        </div>
                        <div class="hexagon hex3 gray">
                            <div class="inner-line"></div>
                            <div class="hexagon-line"></div>
                        </div>
                    </div>
                    <div>
                        <div class="hexagon hex4 blue">
                            <div class="inner-line"></div>
                            <div class="hexagon-line blue line1"></div>
                            <div class="hexagon-line blue line2"></div>
                            <div class="hexagon-line blue line3"></div>
                        </div>
                    </div>
                </div>
                <div class="progress-wrap ${options && options.showProgress ? '' : 'd-none'}">
                    <span class="progress-text"></span>
                    <div class="progress-bar"></div>
                </div>
            </div>
        </div>
    `;

  S2Util.replaceChildren(document.body, loadingHtml, {
    isAppend: true,
    onNodeReady: (node) => {
      if (options && (options.showOverlay || options.showProgress)) {
        node.classList.add('background');
      }
    }
  });

  if (options && options.showProgress) {
    setProgressBar(0);
    controlProgressBar(options ? options.timeout : 0);
  }
};

/**
 * 화면에 표시된 로딩 페이지 오버레이를 제거하고, 프로그레스 바 애니메이션을 중지합니다.
 *
 * @returns {void}
 *
 * @example
 * import { hideS2Loading } from './js/s2.loading.js'; // Thymeleaf 는 '[[@{/js/s2.loading.js}]]' 형태로 경로 지정
 * ex) 로딩 페이지 숨기기
 * hideS2Loading();
 */
export const hideS2Loading = () => {
  // 기존에 설정된 인터벌 제거
  if (g_interval) {
    clearInterval(g_interval);
  }
  const loadingOverlay = document.querySelector('.s2-loading-overlay');
  if (loadingOverlay) {
    loadingOverlay.remove();
  }
};
