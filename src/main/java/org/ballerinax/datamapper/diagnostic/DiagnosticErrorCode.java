/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinax.datamapper.diagnostic;

import io.ballerina.tools.diagnostics.DiagnosticCode;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

/**
 * Diagnostic Error Codes for Compiler Extension.
 */
public enum DiagnosticErrorCode implements DiagnosticCode {
    ERROR_INVALID_ATTRIBUTE_NAME("DME0001", "invalid.attribute.name"),
    ERROR_INVALID_ATTRIBUTE_COUNT("DME0002", "invalid.attribute.count"),
    ERROR_INVALID_JSON_CONTENT("DME0003", "invalid.json.content"),
    ERROR_INVALID_JSON_TOKEN("DME0004", "invalid.json.token"),
    ERROR_INVALID_RECORD_NAME("DME0005", "invalid.record.name"),
    ERROR_RECORD_NAME_NOT_FOUND("DME0006", "record.name.not.found");

    private String diagnosticId;
    private String messageKey;

    private DiagnosticErrorCode(String diagnosticId, String messageKey) {
        this.diagnosticId = diagnosticId;
        this.messageKey = messageKey;
    }

    @Override
    public DiagnosticSeverity severity() {
        return DiagnosticSeverity.ERROR;
    }

    @Override
    public String diagnosticId() {
        return this.diagnosticId;
    }

    @Override
    public String messageKey() {
        return this.messageKey;
    }
}
