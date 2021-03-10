/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinax.datamapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class FunctionRecord {
    private HashMap<String, List<String>> parameters;
    private List<String> returnTypes;

    public FunctionRecord() {
        parameters = new HashMap<>();
        returnTypes = new ArrayList<>();
    }

    public HashMap<String, List<String>> getParameters() {
        return parameters;
    }

    public void addParameter(String name, List<String> type) {
        this.parameters.put(name, type);
    }

    public List<String> getReturnTypes() {
        return returnTypes;
    }

    public void addReturnType(String returnTypeName) {
        this.returnTypes.add(returnTypeName);
    }
}
