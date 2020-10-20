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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test cases for data mapper compiler plugin.
 */
public class DataMapperPluginTest {
    @Test
    public void testHappyPath() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test1", "module_test1",
                CompilerPhase.COMPILER_PLUGIN);
        File jsonFile = new File("src/test/resources/test1/src/module_test1/resources/Assignee_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test1/src/module_test1/resources/Client_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test1/src/module_test1/resources/Creator_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test1/src/module_test1/resources/Issue_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test1/src/module_test1/resources/Label_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testErrorNegativeThreeFieldsWithDifferentNames() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test2", "module_test2",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 3);
        int i = 0;
        BAssertUtil.validateError(result, i++,
                "Error: Type ballerinax/module_test2:0.1.0:Issue does not have an attribute named id2",
                5, 14);
        BAssertUtil.validateError(result, i++,
                "Error: Type ballerinax/module_test2:0.1.0:Creator does not have an attribute named " +
                        "resourcePath5", 12, 18);
        BAssertUtil.validateError(result, i,
                "Error: Type ballerinax/module_test2:0.1.0:Issue does not have an attribute named " +
                        "bodyText3", 26, 14);
    }

    @Test
    public void testErrorNegativeMissingOneField() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test3", "module_test3",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 1);
        BAssertUtil.validateError(result, 0,
                "Error: Sample data provided for ballerinax/module_test3:0.1.0:Issue is different in " +
                        "terms of attributes count", 10, 25);
    }

    @Test
    public void testErrorNegativeMissingTwoFields() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test4", "module_test4",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 1);
        BAssertUtil.validateError(result, 0,
                "Error: Sample data provided for ballerinax/module_test4:0.1.0:Issue is different in " +
                        "terms of attributes count", 10, 25);
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
                "Error: Type ballerinax/module_test6:0.1.0:Creator does not have an attribute named login2", 5, 14);
        BAssertUtil.validateError(result, 1,
                "Error: Type ballerinax/module_test6:0.1.0:Creator does not have an attribute named avatarUrl3", 8, 14);
    }

    @Test
    public void testHappyPathMultipleRecordsMissingNestedRecord() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test7", "module_test7",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 0);
        File jsonFile = new File("src/test/resources/test7/src/module_test7/resources/Assignee_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test7/src/module_test7/resources/Creator_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test7/src/module_test7/resources/Issue_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test7/src/module_test7/resources/Label_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test7/src/module_test7/resources/Client_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
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
        Assert.assertEquals(result.getErrorCount(), 1);
        BAssertUtil.validateError(result, 0,
                "Error: Unexpected character (':' (code 58)): was expecting double-quote to start field name\n" +
                        " at [Source: java.io.InputStreamReader@OBJECTREF; line: 4, column: 14]", 5, 15);
    }

    @Test
    public void testHappyPathWithPropertyHavingArray() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test10", "module_test10",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 0);
        File jsonFile = new File("src/test/resources/test10/src/module_test10/resources/Label_data.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test10/src/module_test10/resources/Assignee_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test10/src/module_test10/resources/Creator_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test10/src/module_test10/resources/Issue_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test10/src/module_test10/resources/Label_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test10/src/module_test10/resources/Client_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
    }

    @Test
    public void testHappyPathSchemaExtraction() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test11", "module_test11",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 0);
        File jsonFile = new File("src/test/resources/test11/src/module_test11/resources/Assignee_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test11/src/module_test11/resources/Creator_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test11/src/module_test11/resources/Issue_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test11/src/module_test11/resources/Label_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
    }

    @Test
    public void testFunctionWithNoAssociatedRecordTypes() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test12", "module_test12",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testDataJSONsWrittenWithoutReferenceToAnyConnectorTypes() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test13", "module_test13",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testHappyPathMultipleClients() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test14", "module_test14",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 0);
        File jsonFile = new File("src/test/resources/test14/src/module_test14/resources/Assignee_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test14/src/module_test14/resources/Creator_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test14/src/module_test14/resources/Issue_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test14/src/module_test14/resources/Label_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test14/src/module_test14/resources/Client1_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test14/src/module_test14/resources/Client2_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
    }

    @Test
    public void testHappyPathClientsWithOddClassAndFunctionNames() {
        CompileResult result = BCompileUtil.compile("src/test/resources/test15", "module_test15",
                CompilerPhase.COMPILER_PLUGIN);
        Assert.assertEquals(result.getErrorCount(), 0);
        File jsonFile = new File("src/test/resources/test15" +
                "/src/module_test15/resources/%5C+%5C%2F%5C%3A%5C%40%5C%5B%5C%60%5C%7B%5C%7E_Connector_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
    }

    @AfterClass
    public void tearDown() throws IOException {
        //Cleanup the test projects if they already have generated json files
        try {
            String path = "src/test/resources/test1/src/module_test1/resources/";
            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));

            path = "src/test/resources/test2/src/module_test2/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_data.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test3/src/module_test3/resources/";

            Files.deleteIfExists(Paths.get(path + "Creator_data.json"));
            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test4/src/module_test4/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_data.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test5/src/module_test5/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test6/src/module_test6/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test7/src/module_test7/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test9/src/module_test9/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test10/src/module_test10/resources/";

            Files.deleteIfExists(Paths.get(path + "Label_data.json"));
            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test11/src/module_test11/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            Files.deleteIfExists(Paths.get("src/test/resources/test12/src/module_test12/resources/" +
                    "Client_functions.json"));

            Files.deleteIfExists(Paths.get("src/test/resources/test13/src/module_test13/resources/" +
                    "Client_functions.json"));

            path = "src/test/resources/test14/src/module_test14/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client1_functions.json"));
            Files.deleteIfExists(Paths.get(path + "Client2_functions.json"));

            path = "src/test/resources/test15/src/module_test15/resources/";

            Files.deleteIfExists(Paths.get(path +
                    "%5C+%5C%2F%5C%3A%5C%40%5C%5B%5C%60%5C%7B%5C%7E_Connector_functions.json"));
        } catch (IOException e) {
            Reporter.log("Error : " + e.getMessage(), true);
            throw e;
        }
    }
}
