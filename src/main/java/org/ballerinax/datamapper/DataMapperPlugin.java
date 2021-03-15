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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.impl.symbols.BallerinaClassSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.Location;
import org.ballerinalang.compiler.plugins.AbstractCompilerPlugin;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.ballerinax.datamapper.diagnostic.DataMapperDiagnosticLog;
import org.ballerinax.datamapper.diagnostic.DiagnosticErrorCode;
import org.ballerinax.datamapper.exceptions.DataMapperException;
import org.ballerinax.datamapper.util.Utils;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compiler extension to generate sample JSON files for data mapper.
 */
public class DataMapperPlugin extends AbstractCompilerPlugin {
    public static final String URL_ENCODED_COLON = "%3A";
    private DataMapperDiagnosticLog dataMapperLog;
    private HashMap<String, ArrayList<JsonNode>> sampleDataMap;
    private HashMap<String, String> typeInformationMap;
    private HashSet<String> typeSet;
    public StringBuilder functions;
    private Location nodePosition;
    private String projectSourceFolder;
    private boolean clientFlag;
    private boolean noFunctionsFlag;
    private JsonParser parser;
    private String currentTypeStructure;
    private String projectDirectory;
    private DiagnosticLog dlog;

    public DataMapperPlugin() {
        this.sampleDataMap = new HashMap<String, ArrayList<JsonNode>>();
        this.typeSet = new HashSet<String>();
        this.functions = new StringBuilder();
        this.typeInformationMap = new HashMap<>();
    }

    @Override
    public void init(DiagnosticLog diagnosticLog) {
        this.dlog = diagnosticLog;
    }


    @Override
    public List<Diagnostic> codeAnalyze(Project project) {
        this.dataMapperLog = new DataMapperDiagnosticLog();
        projectDirectory = project.sourceRoot().toString();
        Package currentPackage = project.currentPackage();
        Collection<ModuleId> moduleIds = currentPackage.moduleIds();
        clientFlag = checkForClient(currentPackage, moduleIds);

        if (clientFlag) {
            String organization = currentPackage.packageOrg().toString();
            String versionNumber = currentPackage.packageVersion().toString();
            for (ModuleId moduleId : moduleIds) {
                String moduleName = moduleId.moduleName();

                Module module = currentPackage.module(moduleId);
                DataMapperNodeVisitor nodeVisitor = new DataMapperNodeVisitor();
                nodeVisitor.setModule(module);
                for (DocumentId documentId : module.documentIds()) {
                    SyntaxTree syntaxTree = module.document(documentId).syntaxTree();
                    syntaxTree.rootNode().accept(nodeVisitor);
                    try {
                        writeFunctionJson(nodeVisitor.getClientMap(), organization, module, versionNumber);
                    } catch (IOException e) {
                        throw new DataMapperException(e);
                    }
                    if (!nodeVisitor.getRecordTypes().isEmpty()) {
                        typeInformationMap.putAll(nodeVisitor.getRecordTypes());
                    }
                }
            }
        }
        pluginExecutionCompleted();

        if (this.dataMapperLog.getDataMapperPluginDiagnostic().isEmpty()) {
            return Collections.emptyList();
        } else {
            return this.dataMapperLog.getDataMapperPluginDiagnostic();
        }
    }

    private void writeFunctionJson(HashMap<String, Map<String, FunctionRecord>> clientMap, String organization,
                                   Module module, String versionNumber) throws IOException {
        if (!clientMap.isEmpty()) {
            for (Map.Entry<String, Map<String, FunctionRecord>> client : clientMap.entrySet()) {
                String clientName = client.getKey();
                Map<String, FunctionRecord> functionMaps = client.getValue();
                boolean flag = false;
                if (!functionMaps.isEmpty()) {
                    noFunctionsFlag = false;
                    Iterator<Map.Entry<String, FunctionRecord>> iterator = functionMaps.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, FunctionRecord> function = iterator.next();
                        String functionName = function.getKey();
                        HashMap<String, List<String>> parameters = function.getValue().getParameters();
                        functions.append("{\"function\":{\"name\":\"" + encode(functionName) +
                                "\",\"requiredParams\":[");
                        if (!parameters.isEmpty()) {
                            Iterator<String> iterator1 = function.getValue().getParameters().keySet().iterator();
                            while (iterator1.hasNext()) {
                                String parameterName = iterator1.next();
                                functions.append("{\"");
                                functions.append(encode(parameterName));
                                functions.append("\":[\"");
                                List<String> typeVariables = parameters.get(parameterName);
                                for (Iterator<String> iter = typeVariables.iterator(); iter.hasNext(); ) {
                                    String typeVariable = iter.next();
                                    functions.append(typeVariable);
                                    if (typeVariable.endsWith("?")) {
                                        this.typeSet.add(typeVariable.substring(0, typeVariable.length() - 1));
                                    } else if (typeVariable.endsWith("[]")) {
                                        this.typeSet.add(typeVariable.substring(0, typeVariable.length() - 2));
                                    } else {
                                        this.typeSet.add(typeVariable);
                                    }
                                    if (iter.hasNext()) {
                                        functions.append("\", \"");
                                    }
                                }
                                functions.append("\"]");
                                if (iterator1.hasNext()) {
                                    functions.append("},");
                                } else {
                                    functions.append("}");
                                }
                            }
                        }
                        functions.append("],\"returnType\":[\"");
                        List<String> returnTypes = function.getValue().getReturnTypes();
                        for (Iterator<String> iter = returnTypes.iterator(); iter.hasNext(); ) {
                            String returnType = iter.next();
                            functions.append(returnType);
                            if (returnType.endsWith("?")) {
                                this.typeSet.add(returnType.substring(0, returnType.length() - 1));
                            } else if (returnType.endsWith("[]")) {
                                this.typeSet.add(returnType.substring(0, returnType.length() - 2));
                            } else {
                                this.typeSet.add(returnType);
                            }
                            if (iter.hasNext()) {
                                functions.append("\", \"");
                            }
                        }
                        functions.append("\"]");

                        flag = iterator.hasNext();

                        if (flag) {
                            functions.append("}},");
                        } else {
                            functions.append("}}");
                        }
                    }
                }

                if (functions.length() != 0) {
                    String functionsJson = functions.toString();

                    if (functionsJson.endsWith(",")) {
                        functionsJson = functionsJson.substring(0, functionsJson.lastIndexOf(","));
                    }

                    String moduleName = module.moduleName().toString();
                    functionsJson = "{\"" + encode(organization) + "/" + encode(moduleName) + ":" + versionNumber +
                            "\" : " + "[" + functionsJson + "]}";
                    functions = new StringBuilder();

                    String functionsFileName = encode(clientName) + "_functions.json";
                    moduleName = moduleName.substring(moduleName.indexOf(".") + 1);

                    Path targetFunctionsFilePath;
                    if (module.isDefaultModule()) {
                        targetFunctionsFilePath = module.project().sourceRoot().resolve("resources").
                                resolve(functionsFileName);
                    } else {
                        targetFunctionsFilePath = Paths.get(projectDirectory, "modules",
                                moduleName, "resources", functionsFileName);
                    }
                    Utils.writeToFile(functionsJson, targetFunctionsFilePath);
                } else {
                    noFunctionsFlag = true;
                }
            }
        }
    }


    public void pluginExecutionCompleted() {
        if (!clientFlag || noFunctionsFlag) {
            return;
        }

        Set<String> secondaryItemsSet = new HashSet<String>();
        Set<String> itemsWrittenSet = new HashSet<String>();

        for (Map.Entry<String, String> entry : this.typeInformationMap.entrySet()) {
            String key = entry.getKey();
            if (this.typeSet.contains(key)) {
                String moduleDirectoryName = key.substring(key.indexOf("/") + 1);
                String splitCharacter = ":";
                if (moduleDirectoryName.contains(URL_ENCODED_COLON)) {
                    splitCharacter = URL_ENCODED_COLON;
                }
                moduleDirectoryName = moduleDirectoryName.substring(0, moduleDirectoryName.indexOf(splitCharacter));
                Path targetStructureFilePath;
                String structureFileName = encode(key.substring(key.lastIndexOf(splitCharacter) + 1)) + "_schema.json";
                if (moduleDirectoryName.indexOf(".") + 1 == 0) {
                    targetStructureFilePath = Paths.get(projectDirectory, "resources", structureFileName);
                } else {
                    moduleDirectoryName = moduleDirectoryName.substring(moduleDirectoryName.indexOf(".") + 1);
                    targetStructureFilePath = Paths.get(projectDirectory, "modules", moduleDirectoryName,
                            "resources", structureFileName);
                }
                String recordEntry = entry.getValue();

                Set<String> keysSet = this.typeInformationMap.keySet();
                for (String s : keysSet) {
                    if (recordEntry.contains(s)) {
                        secondaryItemsSet.add(s);
                    }
                }

                try {
                    Utils.writeToFile(recordEntry, targetStructureFilePath);
                    itemsWrittenSet.add(key);
                } catch (IOException e) {
                    throw new DataMapperException(e);
                }
            }
        }

        secondaryItemsSet.removeAll(itemsWrittenSet);

        for (String key : secondaryItemsSet) {
            String moduleDirectoryName = key.substring(key.indexOf("/") + 1);

            String splitCharacter = ":";
            if (moduleDirectoryName.contains(URL_ENCODED_COLON)) {
                splitCharacter = URL_ENCODED_COLON;
            }

            moduleDirectoryName = moduleDirectoryName.substring(0, moduleDirectoryName.indexOf(splitCharacter));
//            moduleDirectoryName = moduleDirectoryName.substring(moduleDirectoryName.indexOf(".") + 1);
            String structureFileName = encode(key.substring(key.lastIndexOf(splitCharacter) + 1)) + "_schema.json";
            Path targetStructureFilePath;
            if (moduleDirectoryName.indexOf(".") + 1 == 0) {
                targetStructureFilePath = Paths.get(projectDirectory, "resources", structureFileName);
            } else {
                moduleDirectoryName = moduleDirectoryName.substring(moduleDirectoryName.indexOf(".") + 1);
                targetStructureFilePath = Paths.get(projectDirectory, "modules", moduleDirectoryName,
                        "resources", structureFileName);
            }
            String recordEntry = this.typeInformationMap.get(key);

            try {
                Utils.writeToFile(recordEntry, targetStructureFilePath);
            } catch (IOException e) {
                throw new DataMapperException(e);
            }
        }

        Set<String> listOfModuleNames = new HashSet<String>();
        for (Map.Entry<String, String> entry : this.typeInformationMap.entrySet()) {
            String key = entry.getKey();
            String moduleName = null;
            moduleName = key.substring(key.indexOf("/") + 1);
            moduleName = moduleName.substring(0, moduleName.indexOf(":"));
            Path issueDataFilePath;
            if (moduleName.indexOf(".") + 1 == 0) {
                issueDataFilePath = Paths.get(projectDirectory, "resources");
            } else {
                String moduleNameTest = moduleName.substring(moduleName.indexOf(".") + 1);
                issueDataFilePath = Paths.get(projectDirectory, "modules", moduleNameTest, "resources");
            }
            listOfModuleNames.add(moduleName);
        }

        for (String moduleName : listOfModuleNames) {
            processSampleDataFiles(moduleName);
        }
    }

    private void processSampleDataFiles(String moduleName) {
//        moduleName = moduleName.substring(moduleName.indexOf(".") + 1);
        Path issueDataFilePath;
        if (moduleName.indexOf(".") + 1 == 0) {
            issueDataFilePath = Paths.get(projectDirectory, "resources");
        } else {
            moduleName = moduleName.substring(moduleName.indexOf(".") + 1);
            issueDataFilePath = Paths.get(projectDirectory, "modules", moduleName, "resources");
        }
//        Path issueDataFilePath = Paths.get(projectDirectory, "modules", moduleName, "resources");
        List<String> listOfSampleDataJSONFiles = null;

        try {
            Stream<Path> files = Files.walk(issueDataFilePath);
            listOfSampleDataJSONFiles = files.map(x -> x.toString())
                    .filter(f -> f.endsWith("_data.json")).collect(Collectors.toList());

            for (String path : listOfSampleDataJSONFiles) {
                try {
                    readDataArray(path);
                } catch (IOException e) {
                    JsonLocation location = parser.getCurrentLocation();
                    Location position = new BLangDiagnosticLocation(path,
                            location.getLineNr() - 1, location.getLineNr() - 1,
                            location.getColumnNr() - 1, location.getColumnNr() - 1);
                    dataMapperLog.addDiagnostics(position, DiagnosticErrorCode.ERROR_INVALID_JSON_CONTENT,
                            getCustomizedErrorMessage(e));
                }
            }

            for (Map.Entry<String, ArrayList<JsonNode>> entry : this.sampleDataMap.entrySet()) {
                String key = entry.getKey();
                Path targetStructureFilePath = null;
                StringBuilder sb = new StringBuilder();

                String moduleDirectoryName = key.substring(key.indexOf("/") + 1);
                moduleDirectoryName = moduleDirectoryName.substring(0, moduleDirectoryName.indexOf(":"));
                String structureFileName = key.substring(key.lastIndexOf(":") + 1) + "_data.json";
//                moduleDirectoryName = moduleDirectoryName.substring(moduleDirectoryName.indexOf(".") + 1);
                if (moduleDirectoryName.indexOf(".") + 1 == 0) {
                    targetStructureFilePath = Paths.get(projectDirectory, "resources", structureFileName);
                } else {
                    moduleDirectoryName = moduleDirectoryName.substring(moduleDirectoryName.indexOf(".") + 1);
                    targetStructureFilePath = Paths.get(projectDirectory, "modules", moduleDirectoryName,
                            "resources", structureFileName);
                }
//                targetStructureFilePath = Paths.get(projectDirectory, "modules", moduleDirectoryName,
//                        "resources", structureFileName);

                sb.append("{\"");
                sb.append(key);
                sb.append("\":[");

                for (Iterator<JsonNode> iterator = entry.getValue().iterator(); iterator.hasNext(); ) {
                    JsonNode jsonObject = iterator.next();
                    sb.append(jsonObject.get(key).toString());
                    if (iterator.hasNext()) {
                        sb.append(",");
                    }
                }

                sb.append("]}");
                Utils.writeToFile(sb.toString(), targetStructureFilePath);
            }
        } catch (NoSuchFileException e) {
            // safe to ignore
        } catch (IOException e) {
            throw new DataMapperException(e);
        }
    }

    private List<String> readDataArray(String path) throws IOException {
        JsonFactory factory = new JsonFactory();
        InputStream inputStream = new FileInputStream(path);
        Reader fileReader = new InputStreamReader(inputStream, "UTF-8");
        parser = factory.createParser(fileReader);

        List<String> messages = new ArrayList<String>();
        String typeName = null;
        JsonNode typeRecord = null;
        JsonNode dataRecord = null;
        int expectedNumberOfAttributes = 0;
        int previousExpectedNumberOfAttributes = 0;
        int attributeCounter = 0;
        long counter = -1;
        Stack<JsonNode> typeStack = new Stack<JsonNode>();
        Stack<Integer> attributeCounterStack = new Stack<Integer>();
        Stack<JsonLocation> startLocationStack = new Stack<JsonLocation>();
        String previousTypeName = null;
        String previousName = null;
        boolean readyToMoveUpFlag = false;
        boolean errorFlag = false;

        while (!parser.isClosed() && !errorFlag) {
            JsonToken jsonToken = parser.nextToken();
            if (jsonToken == null) {
                break;
            }
            JsonLocation startLocation = null;
            JsonLocation endLocation = null;

            switch (jsonToken) {
                case START_ARRAY:
                    break;
                case END_ARRAY:
                    break;
                case START_OBJECT:
                    if (readyToMoveUpFlag) {
                        readyToMoveUpFlag = false;
                        attributeCounter = 0;
                        dataRecord = constructJSON(currentTypeStructure);
                    } else {
                        if (startLocationStack.size() != 0) {
                            startLocationStack.pop();
                        }
                        startLocation = parser.getCurrentLocation();
                        startLocationStack.push(startLocation);
                        if (counter == -1) {
                            counter = 1;
                        } else {
                            counter++;
                        }
                    }

                    break;
                case END_OBJECT:
                    if (readyToMoveUpFlag) {
                        counter--;
                    }

                    if ((expectedNumberOfAttributes != attributeCounter) && (counter != 1)) {
                        endLocation = parser.getCurrentLocation();
                        startLocation = startLocationStack.pop();
                        Location position = new BLangDiagnosticLocation(path,
                                startLocation.getLineNr() - 1, endLocation.getLineNr() - 1,
                                startLocation.getColumnNr() - 1, endLocation.getColumnNr() - 1);
                        dataMapperLog.addDiagnostics(position, DiagnosticErrorCode.ERROR_INVALID_ATTRIBUTE_COUNT,
                                expectedNumberOfAttributes, attributeCounter);
                    } else {
                        if (!typeStack.isEmpty()) {
                            ArrayList<JsonNode> lst = sampleDataMap.getOrDefault(typeName, new ArrayList<JsonNode>());
                            lst.add(dataRecord);
                            sampleDataMap.put(typeName, lst);
                        }
                    }

                    readyToMoveUpFlag = true;
                    break;
                case FIELD_NAME:
                    if (readyToMoveUpFlag) {
                        readyToMoveUpFlag = false;
                        dataRecord = null;
                        if (!typeStack.isEmpty()) {
                            typeRecord = typeStack.pop();
                            attributeCounter = attributeCounterStack.pop();
                            typeName = previousTypeName;
                            expectedNumberOfAttributes = previousExpectedNumberOfAttributes;
                            currentTypeStructure = typeRecord.toString();
                        } else {
                            counter--;
                        }
                    }

                    final String name = parser.getCurrentName();
                    if (counter == 1) {
                        typeName = name;
                        String value = typeInformationMap.get(name);
                        if (value == null) {
                            errorFlag = true;
                            continue;
                        }

                        typeRecord = constructJSON(value);
                        currentTypeStructure = typeRecord.toString();
                        Iterator<String> iterator = typeRecord.get(name).fieldNames();
                        expectedNumberOfAttributes = 0;
                        while (iterator.hasNext()) {
                            expectedNumberOfAttributes++;
                            iterator.next();
                        }
                    } else if (counter == 2) {
                        if (typeRecord.get(typeName).get(name) == null) {
                            JsonLocation location = parser.getCurrentLocation();
                            Location position = new BLangDiagnosticLocation(path,
                                    location.getLineNr() - 1, location.getLineNr() - 1,
                                    location.getColumnNr() - (name.length() + 6),
                                    location.getColumnNr() - 4);
                            dataMapperLog.addDiagnostics(position, DiagnosticErrorCode.ERROR_INVALID_ATTRIBUTE_NAME,
                                    typeName, name);
                        }
                        attributeCounter++;
                    } else {
                        JsonNode result = typeRecord.get(typeName).get(previousName);
                        typeStack.push(typeRecord);
                        attributeCounterStack.push(attributeCounter);
                        previousTypeName = typeName;
                        attributeCounter = 1;
                        typeName = result.asText();
                        if (typeName.endsWith("[]")) {
                            typeName = typeName.substring(0, typeName.length() - 2);
                        }

                        String value = typeInformationMap.get(typeName);
                        typeRecord = constructJSON(value);
                        dataRecord = constructJSON(value);
                        currentTypeStructure = value;
                        previousExpectedNumberOfAttributes = expectedNumberOfAttributes;

                        Iterator<String> iterator = typeRecord.get(typeName).fieldNames();
                        int attributeCount = 0;
                        while (iterator.hasNext()) {
                            attributeCount++;
                            iterator.next();
                        }

                        expectedNumberOfAttributes = attributeCount;

                        if (!typeRecord.get(typeName).has(name)) {
                            JsonLocation location = parser.getCurrentLocation();
                            Location position = new BLangDiagnosticLocation(path,
                                    location.getLineNr(), location.getLineNr(),
                                    location.getColumnNr() - (name.length() + 5),
                                    location.getColumnNr() - 3);
                            dataMapperLog.addDiagnostics(position, DiagnosticErrorCode.ERROR_INVALID_ATTRIBUTE_NAME,
                                    typeName, name);
                        }

                        counter--;
                    }

                    previousName = name;

                    break;
                case VALUE_STRING:
                    final String s = parser.getValueAsString();
                    if (dataRecord != null && dataRecord.get(typeName).has(previousName)) {
                        ((ObjectNode) dataRecord.get(typeName)).put(previousName, s);
                    }
                    break;
                case VALUE_NUMBER_INT:
                    break;
                case VALUE_NUMBER_FLOAT:
                    break;
                case VALUE_TRUE:
                    break;
                case VALUE_FALSE:
                    break;
                case VALUE_NULL:
                    break;
                default:
                    dataMapperLog.addDiagnostics(nodePosition, DiagnosticErrorCode.ERROR_INVALID_JSON_TOKEN, jsonToken);
                    break;
            }
        }

        return messages;
    }

    public JsonNode constructJSON(String jsonData) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode result = objectMapper.readTree(jsonData);
        return result;
    }


    private String getCustomizedErrorMessage(Exception e) {
        String errorMessage = null;
        String originalMessage = e.getMessage();

        if (originalMessage.contains("Source: java.io.InputStreamReader@")) {
            String[] arr = originalMessage.split("Source: java.io.InputStreamReader@");
            int index = arr[1].indexOf(";");
            String endString = arr[1].substring(index);
            errorMessage = arr[0] + "Source: java.io.InputStreamReader@OBJECTREF" + endString;
        } else if (originalMessage.contains("(InputStreamReader)")) {
            errorMessage = originalMessage;
        }

        return errorMessage;
    }

    private String encode(String fileName) {
        try {
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8");
            return encodedFileName;
        } catch (UnsupportedEncodingException e) {
            throw new DataMapperException(e);
        }
    }

    private boolean checkForClient(Package currentPackage, Collection<ModuleId> moduleIds) {
        for (ModuleId moduleId : moduleIds) {
            Module module = currentPackage.module(moduleId);
            SemanticModel semanticModel = module.getCompilation().getSemanticModel();
            for (Symbol moduleSymbol : semanticModel.moduleSymbols()) {
                if (moduleSymbol.kind() == SymbolKind.CLASS) {
                    List<Qualifier> qualifiers = ((BallerinaClassSymbol) moduleSymbol).qualifiers();
                    if (qualifiers.contains(Qualifier.CLIENT)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
