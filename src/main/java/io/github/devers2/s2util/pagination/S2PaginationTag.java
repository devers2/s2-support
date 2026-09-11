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
package io.github.devers2.s2util.pagination;

import java.io.IOException;
import java.text.MessageFormat;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.TagSupport;

import io.github.devers2.s2util.core.S2StringUtil;
import io.github.devers2.s2util.core.S2Util;

/**
 * s2's utilities
 *
 * @author devers2
 * @version 1.0
 * @since 2023. 05. 31.
 */
public class S2PaginationTag extends TagSupport {

    private static final long serialVersionUID = 1431221408661801454L;

    private S2PaginationInfo<Object> paginationInfo;
    private String jsFunction;
    private String jsParam;

    public int doEndTag() throws JspException {
        try {
            JspWriter out = pageContext.getOut();
            out.println(this.renderPagination(jsParam));
            return 6;
        } catch (IOException e) {
            throw new JspException(e);
        }
    }

    /**
     * Pagination 을 랜더링한다.
     *
     * @param jsParams js 매개변수
     * @return Pagination 문자열
     */
    public final String renderPagination(String... jsParams) {
        var strBuff = new StringBuilder();

        var firstPageLabel = this.paginationInfo.getFirstPageLabel();
        var previousPageLabel = this.paginationInfo.getPreviousPageLabel();
        var currentPageLabel = this.paginationInfo.getCurrentPageLabel();
        var otherPageLabel = this.paginationInfo.getOtherPageLabel();
        var nextPageLabel = this.paginationInfo.getNextPageLabel();
        var lastPageLabel = this.paginationInfo.getLastPageLabel();

        var firstPageNo = this.paginationInfo.getFirstPageNo();
        var firstPageNoOnPageList = this.paginationInfo.getFirstPageNoOnPageList();
        var totalPageCount = this.paginationInfo.getTotalPageCount();
        // 페이지 "묶음(윈도우)" 이동 판단에는 페이지당 레코드 수(pageUnit)가 아니라 페이지 목록에
        // 게시되는 페이지 건수(pageSize)를 써야 한다. getFirstPageNoOnPageList()/
        // getLastPageNoOnPageList() 도 pageSize 기준으로 윈도우를 계산한다.
        var pageSize = this.paginationInfo.getPageSize();
        var lastPageNoOnPageList = this.paginationInfo.getLastPageNoOnPageList();
        var pageNo = this.paginationInfo.getPageNo();
        var lastPageNo = this.paginationInfo.getLastPageNo();

        {
            /*
             * function(JavaScript)을 만들어 pageLabel들에 설정한다.
             * '<a href="#" onclick="{0} return false;">' → '<a href="#" onclick="fn_action('list', {0}); return false;">'
             */
            var jsParamString = "";
            if (S2Util.isNotEmpty(jsParams)) {
                for (var param : jsParams) {
                    if (jsParamString != null && !jsParamString.isBlank()) {
                        jsParamString += ", ";
                    }
                    jsParamString += S2StringUtil.isNaN(param) ? "''" + param + "''" : param;
                }
            }

            // pageNo 매개변수 추가
            if (jsParamString != null && !jsParamString.isBlank()) {
                jsParamString += ", ";
            }
            jsParamString += "{0}";

            var jsFunctionString = this.jsFunction + "(" + jsParamString + ");";

            firstPageLabel = MessageFormat.format(firstPageLabel, jsFunctionString);
            previousPageLabel = MessageFormat.format(previousPageLabel, jsFunctionString);
            otherPageLabel = MessageFormat.format(otherPageLabel, jsFunctionString);
            nextPageLabel = MessageFormat.format(nextPageLabel, jsFunctionString);
            lastPageLabel = MessageFormat.format(lastPageLabel, jsFunctionString);
        }

        if (totalPageCount > pageSize) {
            if (firstPageNoOnPageList > pageSize) {
                strBuff.append(MessageFormat.format(firstPageLabel, Integer.toString(firstPageNo)));
                strBuff.append(MessageFormat.format(previousPageLabel, Integer.toString(firstPageNoOnPageList - 1)));
            } else {
                strBuff.append(MessageFormat.format(firstPageLabel, Integer.toString(firstPageNo)));
                strBuff.append(MessageFormat.format(previousPageLabel, Integer.toString(firstPageNo)));
            }
        }
        for (int i = firstPageNoOnPageList; i <= lastPageNoOnPageList; i++) {
            if (i == pageNo) {
                strBuff.append(MessageFormat.format(currentPageLabel, Integer.toString(i)));
            } else {
                strBuff.append(MessageFormat.format(otherPageLabel, Integer.toString(i), Integer.toString(i)));
            }
        }

        if (totalPageCount > pageSize) {
            if (lastPageNoOnPageList < totalPageCount) {
                strBuff.append(MessageFormat.format(nextPageLabel, Integer.toString(firstPageNoOnPageList + pageSize)));
                strBuff.append(MessageFormat.format(lastPageLabel, Integer.toString(lastPageNo)));
            } else {
                strBuff.append(MessageFormat.format(nextPageLabel, Integer.toString(lastPageNo)));
                strBuff.append(MessageFormat.format(lastPageLabel, Integer.toString(lastPageNo)));
            }
        }
        return strBuff.toString();
    }

    public void setPaginationInfo(S2PaginationInfo<Object> paginationInfo) {
        this.paginationInfo = paginationInfo;
    }

    public void setJsFunction(String jsFunction) {
        this.jsFunction = jsFunction;
    }

    public void setJsParam(String jsParam) {
        this.jsParam = jsParam;
    }

}
