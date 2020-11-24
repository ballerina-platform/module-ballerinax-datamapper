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

import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinalang.test.BAssertUtil;
import org.ballerinalang.test.BCompileUtil;
import org.ballerinalang.test.CompileResult;
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
        CompileResult result = BCompileUtil.compile("test1/modules/module_test1");
        File jsonFile = new File("src/test/resources/test1/modules/module_test1/resources/Assignee_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test1/modules/module_test1/resources/Client_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test1/modules/module_test1/resources/Creator_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test1/modules/module_test1/resources/Issue_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test1/modules/module_test1/resources/Label_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testErrorNegativeThreeFieldsWithDifferentNames() {
        CompileResult result = BCompileUtil.compile("test2/modules/module_test2");
        Assert.assertEquals(result.getErrorCount(), 3);
        int i = 0;
        int j = 0;
        Diagnostic[] diagnostics = result.getDiagnostics();
        int[] diagnosticIndex = new int[result.getErrorCount()];

        for (Diagnostic diag : diagnostics) {
            if (diag.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                diagnosticIndex[j] = i;
                j++;
            }
            i++;
        }
        j = 0;
        BAssertUtil.validateError(result, diagnosticIndex[j++],
                "incompatible types: expected 'Error: Type ballerinax/test2.module_test2:0.1.0:Issue " +
                        "does not have an attribute named id2', found '{1}'",
                4, 13);
        BAssertUtil.validateError(result, diagnosticIndex[j++],
                "incompatible types: expected 'Error: Type ballerinax/test2.module_test2:0.1.0:Creator " +
                        "does not have an attribute named resourcePath5', found '{1}'", 11, 17);
        BAssertUtil.validateError(result, diagnosticIndex[j],
                "incompatible types: expected 'Error: Type ballerinax/test2.module_test2:0.1.0:Issue " +
                        "does not have an attribute named bodyText3', found '{1}'", 25, 13);
    }

    @Test
    public void testErrorNegativeMissingOneField() {
        CompileResult result = BCompileUtil.compile("test3/modules/module_test3");
        Assert.assertEquals(result.getErrorCount(), 1);

        int i = 0;
        Diagnostic[] diagnostics = result.getDiagnostics();
        int diagnosticIndex = 0;

        for (Diagnostic diag : diagnostics) {
            if (diag.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                diagnosticIndex = i;
            }
            i++;
        }

        BAssertUtil.validateError(result, diagnosticIndex,
                "incompatible types: expected 'Error: Sample data provided for " +
                        "ballerinax/test3.module_test3:0.1.0:Issue is different in terms of attributes count', " +
                        "found '{1}'", 9, 18);
    }

    @Test
    public void testErrorNegativeMissingTwoFields() {
        CompileResult result = BCompileUtil.compile("test4/modules/module_test4");
        Assert.assertEquals(result.getErrorCount(), 1);

        int i = 0;
        Diagnostic[] diagnostics = result.getDiagnostics();
        int diagnosticIndex = 0;

        for (Diagnostic diag : diagnostics) {
            if (diag.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                diagnosticIndex = i;
            }
            i++;
        }

        BAssertUtil.validateError(result, diagnosticIndex,
                "incompatible types: expected 'Error: Sample data provided for " +
                        "ballerinax/test4.module_test4:0.1.0:Issue is different in terms of attributes count', " +
                        "found '{1}'", 9, 18);
    }

    @Test
    public void testHappyPathMultipleRecords() {
        CompileResult result = BCompileUtil.compile("test5/modules/module_test5");
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testErrorNegativeMultipleRecordsTwoFieldsDifferentNames() {
        CompileResult result = BCompileUtil.compile("test6/modules/module_test6");
        Assert.assertEquals(result.getErrorCount(), 2);

        int i = 0;
        int j = 0;
        Diagnostic[] diagnostics = result.getDiagnostics();
        int[] diagnosticIndex = new int[result.getErrorCount()];

        for (Diagnostic diag : diagnostics) {
            if (diag.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                diagnosticIndex[j] = i;
                j++;
            }
            i++;
        }
        j = 0;

        BAssertUtil.validateError(result, diagnosticIndex[j++],
                "incompatible types: expected 'Error: Type ballerinax/test6.module_test6:0.1.0:Creator " +
                        "does not have an attribute named login2', found '{1}'", 4, 13);
        BAssertUtil.validateError(result, diagnosticIndex[j],
             "incompatible types: expected 'Error: Type ballerinax/test6.module_test6:0.1.0:Creator " +
                     "does not have an attribute named avatarUrl3', found '{1}'", 7, 13);
    }

    @Test
    public void testHappyPathMultipleRecordsMissingNestedRecord() {
        CompileResult result = BCompileUtil.compile("test7/modules/module_test7");
        Assert.assertEquals(result.getErrorCount(), 0);
        File jsonFile = new File("src/test/resources/test7/modules/module_test7/resources/Assignee_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test7/modules/module_test7/resources/Creator_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test7/modules/module_test7/resources/Issue_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test7/modules/module_test7/resources/Label_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test7/modules/module_test7/resources/Client_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
    }

    @Test
    public void testHappyPathSingleSourceFile() {
        CompileResult result = BCompileUtil.compile("test8/main.bal");
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testErrorNegativeMalformedDataJSON() {
        CompileResult result = BCompileUtil.compile("test9/modules/module_test9");
        Assert.assertEquals(result.getErrorCount(), 1);

        int i = 0;
        Diagnostic[] diagnostics = result.getDiagnostics();
        int diagnosticIndex = 0;

        for (Diagnostic diag : diagnostics) {
            if (diag.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                diagnosticIndex = i;
            }
            i++;
        }

        BAssertUtil.validateError(result, diagnosticIndex,
                "incompatible types: expected 'Error: Unexpected character (':' (code 58)): was " +
                        "expecting double-quote to start field name\n" +
                        " at [Source: java.io.InputStreamReader@OBJECTREF; line: 4, column: 14]', found '{1}'",
                4, 14);
    }

    @Test
    public void testHappyPathWithPropertyHavingArray() {
        CompileResult result = BCompileUtil.compile("test10/modules/module_test10");
        Assert.assertEquals(result.getErrorCount(), 0);
        File jsonFile = new File("src/test/resources/test10/modules/module_test10/resources/Label_data.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test10/modules/module_test10/resources/Assignee_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test10/modules/module_test10/resources/Creator_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test10/modules/module_test10/resources/Issue_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test10/modules/module_test10/resources/Label_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test10/modules/module_test10/resources/Client_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
    }

    @Test
    public void testHappyPathSchemaExtraction() {
        CompileResult result = BCompileUtil.compile("test11/modules/module_test11");
        Assert.assertEquals(result.getErrorCount(), 0);
        File jsonFile = new File("src/test/resources/test11/modules/module_test11/resources/Assignee_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test11/modules/module_test11/resources/Creator_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test11/modules/module_test11/resources/Issue_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test11/modules/module_test11/resources/Label_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
    }

    @Test
    public void testFunctionWithNoAssociatedRecordTypes() {
        CompileResult result = BCompileUtil.compile("test12/modules/module_test12");
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testDataJSONsWrittenWithoutReferenceToAnyConnectorTypes() {
        CompileResult result = BCompileUtil.compile("test13/modules/module_test13");
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testHappyPathMultipleClients() {
        CompileResult result = BCompileUtil.compile("test14/modules/module_test14");
        Assert.assertEquals(result.getErrorCount(), 0);
        File jsonFile = new File("src/test/resources/test14/modules/module_test14/resources/Assignee_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test14/modules/module_test14/resources/Creator_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test14/modules/module_test14/resources/Issue_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test14/modules/module_test14/resources/Label_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test14/modules/module_test14/resources/Client1_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test14/modules/module_test14/resources/Client2_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
    }

    @Test
    public void testHappyPathClientsWithOddClassAndFunctionNames() {
        CompileResult result = BCompileUtil.compile("test15/modules/module_test15");
        Assert.assertEquals(result.getErrorCount(), 0);
        File jsonFile = new File("src/test/resources/test15" +
            "/modules/module_test15/resources/%5C+%5C%2F%5C%3A%5C%40%5C%5B%5C%60%5C%7B%5C%7E_Connector_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
    }

    @AfterClass
    public void tearDown() throws IOException {
        //Cleanup the test projects if they already have generated json files
        try {
            String path = "src/test/resources/test1/modules/module_test1/resources/";
            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));

            path = "src/test/resources/test1/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test1/0.1.0/bir/test1.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test1/0.1.0/bir/test1.module_test1.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test1/0.1.0/java11/test1.jar"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test1/0.1.0/java11/test1.module_test1.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test1/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test1/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test1/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test1"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            path = "src/test/resources/test2/modules/module_test2/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_data.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test2/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test2/0.1.0/bir/test2.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test2/0.1.0/java11/test2.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test2/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test2/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test2/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test2"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            path = "src/test/resources/test3/modules/module_test3/resources/";

            Files.deleteIfExists(Paths.get(path + "Creator_data.json"));
            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test3/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test3/0.1.0/bir/test3.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test3/0.1.0/java11/test3.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test3/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test3/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test3/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test3"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            path = "src/test/resources/test4/modules/module_test4/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_data.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test4/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test4/0.1.0/bir/test4.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test4/0.1.0/java11/test4.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test4/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test4/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test4/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test4"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            path = "src/test/resources/test5/modules/module_test5/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test5/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test5/0.1.0/bir/test5.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test5/0.1.0/bir/test5.module_test5.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test5/0.1.0/java11/test5.jar"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test5/0.1.0/java11/test5.module_test5.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test5/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test5/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test5/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test5"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            path = "src/test/resources/test6/modules/module_test6/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test6/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test6/0.1.0/bir/test6.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test6/0.1.0/java11/test6.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test6/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test6/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test6/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test6"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            path = "src/test/resources/test7/modules/module_test7/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test7/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test7/0.1.0/bir/test7.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test7/0.1.0/bir/test7.module_test7.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test7/0.1.0/java11/test7.jar"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test7/0.1.0/java11/test7.module_test7.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test7/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test7/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test7/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test7"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            path = "src/test/resources/test8/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test8/0.1.0/bir/test8.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test8/0.1.0/java11/test8.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test8/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test8/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test8/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test8"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            path = "src/test/resources/test9/modules/module_test9/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test9/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test9/0.1.0/bir/test9.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test9/0.1.0/java11/test9.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test9/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test9/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test9/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test9"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            path = "src/test/resources/test10/modules/module_test10/resources/";

            Files.deleteIfExists(Paths.get(path + "Label_data.json"));
            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test10/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test10/0.1.0/bir/test10.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test10/0.1.0/bir/test10.module_test10.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test10/0.1.0/java11/test10.jar"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test10/0.1.0/java11/test10.module_test10.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test10/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test10/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test10/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test10"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            path = "src/test/resources/test11/modules/module_test11/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test11/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test11/0.1.0/bir/test11.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test11/0.1.0/bir/test11.module_test11.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test11/0.1.0/java11/test11.jar"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test11/0.1.0/java11/test11.module_test11.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test11/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test11/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test11/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            Files.deleteIfExists(Paths.get("src/test/resources/test12/modules/module_test12/resources/" +
                    "Client_functions.json"));

            path = "src/test/resources/test12/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test12/0.1.0/bir/test12.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test12/0.1.0/bir/test12.module_test12.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test12/0.1.0/java11/test12.jar"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test12/0.1.0/java11/test12.module_test12.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test12/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test12/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test12/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test12"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            Files.deleteIfExists(Paths.get("src/test/resources/test13/modules/module_test13/resources/" +
                    "Client_functions.json"));

            path = "src/test/resources/test13/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test13/0.1.0/bir/test13.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test13/0.1.0/bir/test13.module_test13.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test13/0.1.0/java11/test13.jar"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test13/0.1.0/java11/test13.module_test13.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test13/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test13/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test13/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test13"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            path = "src/test/resources/test14/modules/module_test14/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client1_functions.json"));
            Files.deleteIfExists(Paths.get(path + "Client2_functions.json"));

            path = "src/test/resources/test14/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test14/0.1.0/bir/test14.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test14/0.1.0/bir/test14.module_test14.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test14/0.1.0/java11/test14.jar"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test14/0.1.0/java11/test14.module_test14.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test14/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test14/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test14/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test14"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

            path = "src/test/resources/test15/modules/module_test15/resources/";

            Files.deleteIfExists(Paths.get(path +
                    "%5C+%5C%2F%5C%3A%5C%40%5C%5B%5C%60%5C%7B%5C%7E_Connector_functions.json"));

            path = "src/test/resources/test15/target/";

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test15/0.1.0/bir/test15.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test15/0.1.0/bir/test15.module_test15.bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test15/0.1.0/java11/test15.jar"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test15/0.1.0/java11/test15.module_test15.jar"));

            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test15/0.1.0/bir"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test15/0.1.0/java11"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test15/0.1.0"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax/test15"));
            Files.deleteIfExists(Paths.get(path + "cache/ballerinax"));
            Files.deleteIfExists(Paths.get(path + "cache"));
            Files.deleteIfExists(Paths.get(path));

        } catch (IOException e) {
            Reporter.log("Error : " + e.getMessage(), true);
            throw e;
        }
    }
}
