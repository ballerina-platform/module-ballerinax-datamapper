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

package org.ballerinax.datamapper;

import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DataMapperPluginTest {
    CompileResult result1;
    CompileResult result2;


    @BeforeClass
    public void setup() {
        result1 = BCompileUtil.compileAndGetBIR("data/test1/github_type.bal");
        result2 = BCompileUtil.compileAndGetBIR("data/test2/github_type.bal");
    }

    @Test
    public void testHappyPath() {
        Assert.assertEquals(result1.getErrorCount(), 0);
    }

    @Test
    public void testErrorNegative() {
        Assert.assertEquals(result2.getErrorCount(), 3);
    }
}
