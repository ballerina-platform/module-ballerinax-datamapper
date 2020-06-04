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

import org.ballerinalang.compiler.CompilerPhase;
import org.ballerinalang.test.util.BAssertUtil;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DataMapperPluginTest {
    @Test
    public void testHappyPath() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test1", "module_test1",
                CompilerPhase.COMPILER_PLUGIN);
        Reporter.log(result.toString(), true);
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testErrorNegativeThreeFieldsWithDifferentNames() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test2", "module_test2",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 3);
        int i = 0;
        BAssertUtil.validateError(result, i++,
                "Error: Type miyurud/module_test2:0.1.0:Issue does not have an attribute named id2", 4, 13);
        BAssertUtil.validateError(result, i++,
                "Error: Type miyurud/module_test2:0.1.0:Creator does not have an attribute named resourcePath5", 11, 17);
        BAssertUtil.validateError(result, i,
                "Error: Type miyurud/module_test2:0.1.0:Issue does not have an attribute named bodyText3", 25, 13);
    }

    @Test
    public void testErrorNegativeMissingOneField() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test3", "module_test3",
                CompilerPhase.COMPILER_PLUGIN);
        Reporter.log(result.toString(), true);
        Assert.assertEquals(result.getErrorCount(), 1);
        BAssertUtil.validateError(result, 0,
                "Error: Sample data provided for miyurud/module_test3:0.1.0:Issue is different in terms of attributes count", 9, 24);
    }

    @Test
    public void testErrorNegativeMissingTwoFields() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test4", "module_test4",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 1);
        BAssertUtil.validateError(result, 0,
                "Error: Sample data provided for miyurud/module_test4:0.1.0:Issue is different in terms of attributes count", 9, 24);
    }

    @Test
    public void testHappyPathMultipleRecords() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test5", "module_test5",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testErrorNegativeMultipleRecordsTwoFieldsDifferentNames() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test6", "module_test6",
                CompilerPhase.COMPILER_PLUGIN);
                Assert.assertEquals(result.getErrorCount(), 2);
        BAssertUtil.validateError(result, 0,
                "Error: Type miyurud/module_test6:0.1.0:Creator does not have an attribute named login2", 4, 13);
        BAssertUtil.validateError(result, 1,
                "Error: Type miyurud/module_test6:0.1.0:Creator does not have an attribute named avatarUrl3", 7, 13);
    }

    @Test
    public void testHappyPathMultipleRecordsMissingNestedRecord() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test7", "module_test7",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testHappyPathSingleSourceFile() {
        CompileResult result = BCompileUtil.compile("test8/main.bal", CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testErrorNegativeMalformedDataJSON() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test9", "module_test9",
                CompilerPhase.COMPILER_PLUGIN);
        Reporter.log(result.toString(), true);
        Assert.assertEquals(result.getErrorCount(), 1);
        BAssertUtil.validateError(result, 0,
                "Error: Unexpected character (':' (code 58)): was expecting double-quote to start field name\n" +
                        " at [Source: java.io.FileReader@OBJECTREF; line: 4, column: 14]", 4, 14);
    }

    @Test
    public void testHappyPathWithPropertyHavingArray() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test10", "module_test10",
                CompilerPhase.COMPILER_PLUGIN);
        Reporter.log(result.toString(), true);
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @AfterClass
    public void tearDown() throws IOException {
        //Cleanup the test projects if they already have generated json files
        try {
            Files.deleteIfExists(Paths.get("src/test/resources/test1/src/module_test1/resources/module_test1_Creator_data.json"));
            Files.deleteIfExists(Paths.get("src/test/resources/test2/src/module_test2/resources/module_test2_Creator_data.json"));
            Files.deleteIfExists(Paths.get("src/test/resources/test3/src/module_test3/resources/module_test3_Creator_data.json"));
            Files.deleteIfExists(Paths.get("src/test/resources/test4/src/module_test4/resources/module_test4_Creator_data.json"));
            Files.deleteIfExists(Paths.get("src/test/resources/test7/src/module_test7/resources/module_test7_Creator_data.json"));
            Files.deleteIfExists(Paths.get("src/test/resources/test10/src/module_test10/resources/module_test10_Label_data.json"));
        } catch (IOException e) {
            Reporter.log("Error : " + e.getMessage(), true);
            throw e;
        }
    }
}
