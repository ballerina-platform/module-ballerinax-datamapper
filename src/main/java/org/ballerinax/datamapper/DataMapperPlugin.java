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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ballerina.tools.diagnostics.Location;
import org.ballerinalang.compiler.CompilerOptionName;
import org.ballerinalang.compiler.plugins.AbstractCompilerPlugin;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.tree.NodeKind;
import org.ballerinalang.model.tree.PackageNode;
import org.ballerinalang.model.tree.TopLevelNode;
import org.ballerinalang.util.diagnostic.DiagnosticErrorCode;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.ballerinax.datamapper.util.Utils;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLog;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BRecordTypeSymbol;
import org.wso2.ballerinalang.compiler.tree.BLangClassDefinition;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;
import org.wso2.ballerinalang.compiler.tree.types.BLangType;
import org.wso2.ballerinalang.compiler.tree.types.BLangUnionTypeNode;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.CompilerOptions;
import org.wso2.ballerinalang.util.Flags;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
    private BLangDiagnosticLog dlog;
    private HashMap<String, ArrayList<JsonNode>> sampleDataMap;
    private HashMap<String, String> typeInformationMap;
    private HashSet<String> typeSet;
    private StringBuilder functions;
    private Location nodePosition;
    private BLangPackage lastPackageNode;
    private String projectSourceFolder;
    private boolean clientFlag;
    private boolean noFunctionsFlag;
    private JsonParser parser;
    private String currentTypeStructure;
    private CompilerContext context = null;
    private String projectDirectory;

    @Override
    public void setCompilerContext(CompilerContext ctx) {
        context = ctx;
        CompilerOptions options = CompilerOptions.getInstance(ctx);
        projectDirectory = options.get(CompilerOptionName.PROJECT_DIR);
    }

    @Override
    public void init(DiagnosticLog diagnosticLog) {
        this.dlog = (BLangDiagnosticLog) diagnosticLog;
        this.sampleDataMap = new HashMap<String, ArrayList<JsonNode>>();
        this.typeInformationMap = new HashMap<String, String>();
        this.typeSet = new HashSet<String>();
        this.functions = new StringBuilder();
    }

    @Override
    public void process(PackageNode packageNode) {
        for (TopLevelNode node : ((BLangPackage) packageNode).topLevelNodes) {
            if (node instanceof BLangClassDefinition) {
                if ((((BLangClassDefinition) node).symbol.flags & Flags.CLIENT) == Flags.CLIENT) {
                    clientFlag = true;
                    break;
                }
            }
        }

        if (clientFlag) {
            for (TopLevelNode topLevelNode : ((BLangPackage) packageNode).topLevelNodes) {
                if (topLevelNode.getKind() == NodeKind.CLASS_DEFN &&
                        (((BLangClassDefinition) topLevelNode).symbol.flags & Flags.CLIENT) == Flags.CLIENT) {
                    try {
                        extractFunctionCalls((BLangClassDefinition) topLevelNode);
                    } catch (Exception e) {
                        dlog.error(packageNode.getPosition(), DiagnosticErrorCode.INCOMPATIBLE_TYPES, e.getMessage());
                    }
                } else if (topLevelNode.getKind() == NodeKind.TYPE_DEFINITION &&
                        ((BLangTypeDefinition) topLevelNode).typeNode.getKind() == NodeKind.RECORD_TYPE) {
                    BLangTypeDefinition recordTypeDef = (BLangTypeDefinition) topLevelNode;
                    Map<String, String> structureMap = new LinkedHashMap<String, String>();
                    DataMapperStructureVisitor visitor = new DataMapperStructureVisitor(structureMap);
                    visitor.visit(recordTypeDef);
                    String typeName = null;
                    String serialized = null;
                    if (recordTypeDef.symbol instanceof  BRecordTypeSymbol) {
                        typeName = ((BRecordTypeSymbol) recordTypeDef.symbol).toString();
                        String[] typeNameSegments = typeName.split("/");
                        String organizationName = typeNameSegments[0];
                        String[] moduleNameSegments = typeNameSegments[1].split(":");

                        String encodedTypeName = encode(organizationName) + "/" + encode(moduleNameSegments[0]) +
                                ":" + encode(moduleNameSegments[1]) + ":" + encode(moduleNameSegments[2]);
                        try {
                            serialized = new ObjectMapper().writeValueAsString(structureMap);
                            serialized = "{\"" + encodedTypeName + "\":" + serialized + "}";
                        } catch (JsonProcessingException e) {
                            dlog.error(packageNode.getPosition(),
                                    DiagnosticErrorCode.INCOMPATIBLE_TYPES, e.getMessage());
                        }
                        structureMap.clear();
                        this.typeInformationMap.put(encodedTypeName, serialized);
                    }
                }
            }

            nodePosition = packageNode.getPosition();
            lastPackageNode = (BLangPackage) packageNode;
        }
    }

    @Override
    public void pluginExecutionCompleted(PackageID packageID) {
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
                moduleDirectoryName = moduleDirectoryName.substring(moduleDirectoryName.indexOf(".") + 1);
                String structureFileName = encode(key.substring(key.lastIndexOf(splitCharacter) + 1)) + "_schema.json";
                Path targetStructureFilePath = Paths.get(projectDirectory, "modules", moduleDirectoryName,
                        "resources", structureFileName);
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
                    dlog.error(nodePosition, DiagnosticErrorCode.INCOMPATIBLE_TYPES, e.getMessage());
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
            moduleDirectoryName = moduleDirectoryName.substring(moduleDirectoryName.indexOf(".") + 1);
            String structureFileName = encode(key.substring(key.lastIndexOf(splitCharacter) + 1)) + "_schema.json";
            Path targetStructureFilePath = Paths.get(projectDirectory, "modules", moduleDirectoryName,
                    "resources", structureFileName);
            String recordEntry = this.typeInformationMap.get(key);

            try {
                Utils.writeToFile(recordEntry, targetStructureFilePath);
            } catch (IOException e) {
                dlog.error(nodePosition, DiagnosticErrorCode.INCOMPATIBLE_TYPES, e.getMessage());
            }
        }

        Set<String> listOfModuleNames = new HashSet<String>();
        for (Map.Entry<String, String> entry : this.typeInformationMap.entrySet()) {
            String key = entry.getKey();
            String moduleName = null;
            moduleName = key.substring(key.indexOf("/") + 1);
            moduleName = moduleName.substring(0, moduleName.indexOf(":"));
            moduleName = moduleName.substring(moduleName.indexOf(".") + 1);
            listOfModuleNames.add(moduleName);
        }

        for (String moduleName : listOfModuleNames) {
            processSampleDataFiles(moduleName);
        }
    }

    private void processSampleDataFiles(String moduleName) {
        Path issueDataFilePath = Paths.get(projectDirectory, "modules", moduleName, "resources");
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
                    dlog.error(position, DiagnosticErrorCode.INCOMPATIBLE_TYPES, "Error: " +
                            getCustomizedErrorMessage(e));
                }
            }

            for (Map.Entry<String, ArrayList<JsonNode>> entry : this.sampleDataMap.entrySet()) {
                String key = entry.getKey();
                Path targetStructureFilePath = null;
                StringBuilder sb = new StringBuilder();

                String moduleDirectoryName = key.substring(key.indexOf("/") + 1);
                moduleDirectoryName = moduleDirectoryName.substring(0, moduleDirectoryName.indexOf(":"));
                moduleDirectoryName = moduleDirectoryName.substring(moduleDirectoryName.indexOf(".") + 1);
                String structureFileName = key.substring(key.lastIndexOf(":") + 1) + "_data.json";
                targetStructureFilePath = Paths.get(projectDirectory, "modules", moduleDirectoryName,
                        "resources", structureFileName);

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
        } catch (IOException e) {
            dlog.error(nodePosition, DiagnosticErrorCode.INCOMPATIBLE_TYPES, e.getMessage());
        }
    }

    private List<String> readDataArray(String path) throws IOException {
        JsonFactory factory = new JsonFactory();
        InputStream inputStream = new FileInputStream(path);
        Reader fileReader = new InputStreamReader(inputStream, "UTF-8");
        parser  = factory.createParser(fileReader);

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
                        dlog.error(position, DiagnosticErrorCode.INCOMPATIBLE_TYPES,
                                "Error: Sample data provided for " +
                                typeName + " is different in terms of attributes count");
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
                                    location.getColumnNr() - (name.length() + 6), location.getColumnNr() - 4);
                            dlog.error(position, DiagnosticErrorCode.INCOMPATIBLE_TYPES, "Error: Type " + typeName +
                                    " does not have an attribute named " + name);
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
                                    location.getColumnNr() - (name.length() + 5), location.getColumnNr() - 3);
                            dlog.error(position, DiagnosticErrorCode.INCOMPATIBLE_TYPES, "Error: Type " +
                                    typeName + " does not have an attribute named " + name);
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
                    dlog.error(nodePosition, DiagnosticErrorCode.INCOMPATIBLE_TYPES,
                            "Error: Unexpected JSON token value: "
                            + jsonToken);
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

    private void extractFunctionCalls(BLangClassDefinition typeDefinition) throws IOException {
        noFunctionsFlag = false;
        String name = typeDefinition.name.toString();
        Iterator<BLangFunction> iterator = typeDefinition.functions.iterator();
        boolean flag = false;
        while (iterator.hasNext()) {
            BLangFunction function =  iterator.next();
            if (function.flagSet.contains(Flag.REMOTE)) {
                functions.append("{\"function\":{\"name\":\"" + encode(function.name.toString()) +
                        "\",\"requiredParams\":[");
                Iterator<BLangSimpleVariable> iterator2 = function.requiredParams.iterator();
                while (iterator2.hasNext()) {
                    BLangSimpleVariable requiredParam = iterator2.next();
                    functions.append("{\"");
                    functions.append(encode(requiredParam.name.toString()));
                    functions.append("\":\"");
                    String typeVariable = requiredParam.symbol.type.toString();
                    functions.append(typeVariable);
                    if (typeVariable.endsWith("?")) {
                        this.typeSet.add(typeVariable.substring(0, typeVariable.length() - 1));
                    } else if (typeVariable.endsWith("[]")) {
                        this.typeSet.add(typeVariable.substring(0, typeVariable.length() - 2));
                    } else {
                        this.typeSet.add(typeVariable);
                    }
                    if (iterator2.hasNext()) {
                        functions.append("\"},");
                    } else {
                        functions.append("\"}");
                    }
                }
                functions.append("],\"returnType\":\"");
                if (function.returnTypeNode.getKind() == NodeKind.UNION_TYPE_NODE) {
                    List<BLangType> lst = ((BLangUnionTypeNode) function.returnTypeNode).memberTypeNodes;
                    for (int i = 0; i < lst.size(); i++) {
                        String item = lst.get(i).toString();
                        if (item.equals("error")) {
                            continue;
                        } else if (item.equals("error?")) {
                            continue;
                        } else if (item.equals("error<>")) {
                            continue;
                        } else {
                            String returnType = lst.get(i).type.toString();
                            functions.append(returnType);
                            if (returnType.endsWith("?")) {
                                this.typeSet.add(returnType.substring(0, returnType.length() - 1));
                            } else if (returnType.endsWith("[]")) {
                                this.typeSet.add(returnType.substring(0, returnType.length() - 2));
                            } else {
                                this.typeSet.add(returnType);
                            }
                            break;
                        }
                    }
                } else {
                    functions.append(function.returnTypeNode.type.tsymbol);
                }
                flag = iterator.hasNext();

                if (flag) {
                    functions.append("\"}},");
                } else {
                    functions.append("\"}}");
                }
            }
        }

        if (functions.length() != 0) {
            String symbol = String.valueOf(typeDefinition.symbol);
            String organization = symbol.substring(0, symbol.indexOf("/"));
            String moduleName = symbol.substring(symbol.indexOf("/") + 1);
            String versionNumber = null;

            String splitCharacter = ":";
            if (moduleName.contains(URL_ENCODED_COLON)) {
                splitCharacter = URL_ENCODED_COLON;
            }

            if (moduleName.contains(splitCharacter)) {
                String[] moduleNameSegments = moduleName.split(splitCharacter);
                moduleName = moduleNameSegments[0];
                versionNumber = moduleNameSegments[1];
            }

            String functionsJson = functions.toString();

            if (functionsJson.endsWith(",")) {
                functionsJson = functionsJson.substring(0, functionsJson.lastIndexOf(","));
            }

            functionsJson = "{\"" + organization + "/" + moduleName + ":" + versionNumber + "\" : " +
                    "[" + functionsJson + "]}";
            functions = new StringBuilder();
            name = encode(name);

            String functionsFileName = name + "_functions.json";
            moduleName = moduleName.substring(moduleName.indexOf(".") + 1);
            Path targetFunctionsFilePath = Paths.get(projectDirectory, "modules", moduleName, "resources",
                    functionsFileName);
            Utils.writeToFile(functionsJson, targetFunctionsFilePath);
        } else {
            noFunctionsFlag = true;
        }
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
            dlog.error(nodePosition, DiagnosticErrorCode.INCOMPATIBLE_TYPES, e.getMessage());
        }

        return null;
    }
}
