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

import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticCode;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.Location;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Diagnostic log for Data Mapper Compiler Extension.
 */
public class DataMapperDiagnosticLog {

    private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("datamapper", Locale.getDefault());
    private static final String ERROR_PREFIX = "error";

    public List<Diagnostic> getDataMapperPluginDiagnostic() {
        return dataMapperPluginDiagnostic;
    }

    private List<Diagnostic> dataMapperPluginDiagnostic;

    public DataMapperDiagnosticLog() {
        this.dataMapperPluginDiagnostic = new ArrayList<>();
    }

    public void addDiagnostics(Location position, DiagnosticErrorCode diagnosticErrorCode, Object... args) {
        String msg = formatMessage(ERROR_PREFIX, diagnosticErrorCode, args);
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                diagnosticErrorCode.diagnosticId(), msg,
                diagnosticErrorCode.severity());

        dataMapperPluginDiagnostic.add(DiagnosticFactory.createDiagnostic(diagnosticInfo, position));
    }

    private String formatMessage(String prefix, DiagnosticCode code, Object[] args) {
        String msgKey = MESSAGES.getString(prefix + "." + code.messageKey());
        return MessageFormat.format(msgKey, args);
    }

}
