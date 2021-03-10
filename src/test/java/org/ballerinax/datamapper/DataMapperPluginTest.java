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
import java.nio.file.Path;
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
                "type 'ballerinax/test2.module_test2:0.1.0:Issue' does not have an attribute named 'id2'",
                4, 13);
        BAssertUtil.validateError(result, diagnosticIndex[j++],
                "type 'ballerinax/test2.module_test2:0.1.0:Creator' does not have an attribute " +
                        "named 'resourcePath5'", 11, 17);
        BAssertUtil.validateError(result, diagnosticIndex[j],
                "type 'ballerinax/test2.module_test2:0.1.0:Issue' does not have an attribute " +
                        "named 'bodyText3'", 25, 13);
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
                "invalid attribute count: expected '13', found '12'", 9, 18);
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
                "invalid attribute count: expected '13', found '11'", 9, 18);
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
                "type 'ballerinax/test6.module_test6:0.1.0:Creator' does not have an attribute " +
                        "named 'login2'", 4, 13);
        BAssertUtil.validateError(result, diagnosticIndex[j],
             "type 'ballerinax/test6.module_test6:0.1.0:Creator' does not have an attribute " +
                     "named 'avatarUrl3'", 7, 13);
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
    public void testHappyPathDefaultModuleBuildProject() {
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
                "invalid JSON content: 'Unexpected character (':' (code 58)): was " +
                        "expecting double-quote to start field name\n" +
                        " at [Source: java.io.InputStreamReader@OBJECTREF; line: 4, column: 14]'",
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

    @Test
    public void testHappyPathSingleSourceFile() {
        CompileResult result = BCompileUtil.compile("test16/main.bal");
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testHappyPathWithMultiModuleProject() {
        CompileResult result = BCompileUtil.compile("test17/modules/module_test17_1");
        File jsonFile = new File("src/test/resources/test17/modules/module_test17_1/resources/Assignee_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test17/modules/module_test17_1/resources/Client_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test17/modules/module_test17_1/resources/Creator_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test17/modules/module_test17_1/resources/Issue_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test17/modules/module_test17_1/resources/Label_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test17/modules/module_test17_3/resources/Email_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testHappyPathWithMultipleReturnTypes() {
        CompileResult result = BCompileUtil.compile("test18/modules/module_test18");
        File jsonFile = new File("src/test/resources/test18/modules/module_test18/resources/Assignee_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test18/modules/module_test18/resources/Client_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test18/modules/module_test18/resources/Creator_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void testHappyPathWithUnionParameterTypes() {
        CompileResult result = BCompileUtil.compile("test19/modules/module_test19");
        File jsonFile = new File("src/test/resources/test19/modules/module_test19/resources/Assignee_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test19/modules/module_test19/resources/Client_functions.json");
        Assert.assertEquals(jsonFile.exists(), true);
        jsonFile = new File("src/test/resources/test19/modules/module_test19/resources/Creator_schema.json");
        Assert.assertEquals(jsonFile.exists(), true);
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    public static boolean deleteDirectory(Path directoryPath) {
        File directory = new File(String.valueOf(directoryPath));
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File f : files) {
                    boolean success = deleteDirectory(f.toPath());
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return directory.delete();
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
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test2/modules/module_test2/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_data.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test2/target/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test3/modules/module_test3/resources/";

            Files.deleteIfExists(Paths.get(path + "Creator_data.json"));
            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test3/target/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test4/modules/module_test4/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_data.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test4/target/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test5/modules/module_test5/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test5/target/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test6/modules/module_test6/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test6/target/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test7/modules/module_test7/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test7/target/";
            deleteDirectory(Path.of(path));


            path = "src/test/resources/test8/target/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test9/modules/module_test9/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test9/target/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test10/modules/module_test10/resources/";

            Files.deleteIfExists(Paths.get(path + "Label_data.json"));
            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test10/target/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test11/modules/module_test11/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client_functions.json"));

            path = "src/test/resources/test11/target/";
            deleteDirectory(Path.of(path));

            Files.deleteIfExists(Paths.get("src/test/resources/test12/modules/module_test12/resources/" +
                    "Client_functions.json"));

            path = "src/test/resources/test12/target/";
            deleteDirectory(Path.of(path));

            Files.deleteIfExists(Paths.get("src/test/resources/test13/modules/module_test13/resources/" +
                    "Client_functions.json"));

            path = "src/test/resources/test13/target/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test14/modules/module_test14/resources/";

            Files.deleteIfExists(Paths.get(path + "Assignee_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Creator_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Issue_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Label_schema.json"));
            Files.deleteIfExists(Paths.get(path + "Client1_functions.json"));
            Files.deleteIfExists(Paths.get(path + "Client2_functions.json"));

            path = "src/test/resources/test14/target/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test15/modules/module_test15/resources/";

            Files.deleteIfExists(Paths.get(path +
                    "%5C+%5C%2F%5C%3A%5C%40%5C%5B%5C%60%5C%7B%5C%7E_Connector_functions.json"));

            path = "src/test/resources/test15/target/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test16/";
            Files.deleteIfExists(Paths.get(path + "main.jar"));

            path = "src/test/resources/test17/modules/module_test17_1/resources/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test17/modules/module_test17_3/resources/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test17/target/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test18/modules/module_test18/resources/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test18/target/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test19/modules/module_test19/resources/";
            deleteDirectory(Path.of(path));

            path = "src/test/resources/test19/target/";
            deleteDirectory(Path.of(path));

        } catch (IOException e) {
            Reporter.log("Error : " + e.getMessage(), true);
            throw e;
        }
    }
}
