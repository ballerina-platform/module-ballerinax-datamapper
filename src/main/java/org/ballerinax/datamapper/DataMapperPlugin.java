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
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ballerinalang.compiler.plugins.AbstractCompilerPlugin;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.symbols.SymbolKind;
import org.ballerinalang.model.tree.PackageNode;
import org.ballerinalang.model.tree.NodeKind;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.ballerinax.datamapper.util.Utils;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BRecordTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeSymbol;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;
import org.wso2.ballerinalang.compiler.tree.types.BLangObjectTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangType;
import org.wso2.ballerinalang.compiler.tree.types.BLangUnionTypeNode;
import org.wso2.ballerinalang.compiler.util.diagnotic.BDiagnosticSource;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;
import org.wso2.ballerinalang.util.Flags;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParser;

/**
 * Compiler extension to generate sample JSON files for data mapper
 */
public class DataMapperPlugin extends AbstractCompilerPlugin {
    private DiagnosticLog dlog;
    private HashMap<String, ArrayList<JsonNode>> sampleDataMap;
    private HashMap<String, String> typeInformationMap;
    private HashSet<String> typeSet;
    private StringBuilder functions;
    private Diagnostic.DiagnosticPosition nodePosition;
    private BLangPackage lastPackageNode;
    private boolean singleSourceFileFlag;

    @Override
    public void init(DiagnosticLog diagnosticLog) {
        this.dlog = diagnosticLog;
        this.sampleDataMap = new HashMap<String, ArrayList<JsonNode>>();
        this.typeInformationMap = new HashMap<String, String>();
        this.typeSet = new HashSet<String>();
        this.functions = new StringBuilder();
    }

    @Override
    public void process(PackageNode packageNode) {
        if (((BLangPackage) packageNode).packageID.name.toString().equals(".")) {
            singleSourceFileFlag = true;
        }

        for (BLangTypeDefinition typeDefinition : ((BLangPackage) packageNode).typeDefinitions) {
            if ((typeDefinition.typeNode.type.tsymbol.flags & Flags.CLIENT) == Flags.CLIENT) {
                try {
                    extractFunctionCalls(typeDefinition);
                } catch (Exception e) {
                    dlog.logDiagnostic(Diagnostic.Kind.ERROR, packageNode.getPosition(), e.getMessage());
                }
            } else if (((BTypeSymbol)typeDefinition.symbol).kind == SymbolKind.RECORD) {
                StringBuilder structureStringBuilder = new StringBuilder();
                DataMapperStructureVisitor visitor = new DataMapperStructureVisitor(structureStringBuilder);
                visitor.visit(typeDefinition);
                String structure = structureStringBuilder.toString();

                structure = structure.replace(",}", "}");
                structure = structure.replace(",]", "]");
                structure = structure.substring(0, structure.length() -1);
                String typeName = ((BRecordTypeSymbol)typeDefinition.symbol).toString();
                this.typeInformationMap.put(typeName, structure);
            }
        }

        nodePosition = packageNode.getPosition();
        lastPackageNode = (BLangPackage) packageNode;
    }

    @Override
    public void pluginExecutionCompleted(PackageID packageID) {
        Set<String> secondaryItemsSet = new HashSet<String>();
        Set<String> itemsWrittenSet = new HashSet<String>();

        for (Map.Entry<String, String> entry : this.typeInformationMap.entrySet()) {
            String key = entry.getKey();
            if (this.typeSet.contains(key)) {
                String moduleDirectoryName = key.substring(key.indexOf("/") + 1);
                moduleDirectoryName = moduleDirectoryName.substring(0, moduleDirectoryName.indexOf(":"));
                String structureFileName = moduleDirectoryName + "_" + key.substring(key.lastIndexOf(":") + 1) + "_schema.json";
                Path targetStructureFilePath = Paths.get("src", moduleDirectoryName, "resources", structureFileName);
                String recordEntry = entry.getValue();

                Set<String> keysSet = this.typeInformationMap.keySet();
                for (String s : keysSet) {
                    if(recordEntry.contains(s)) {
                        secondaryItemsSet.add(s);
                    }
                }

                try {
                    Utils.writeToFile(recordEntry, targetStructureFilePath);
                    itemsWrittenSet.add(key);
                }catch(IOException e) {
                    dlog.logDiagnostic(Diagnostic.Kind.ERROR, nodePosition, e.getMessage());
                }
            }
        }

        secondaryItemsSet.removeAll(itemsWrittenSet);

        for (String key : secondaryItemsSet) {
            String moduleDirectoryName = key.substring(key.indexOf("/") + 1);
            moduleDirectoryName = moduleDirectoryName.substring(0, moduleDirectoryName.indexOf(":"));
            String structureFileName = moduleDirectoryName + "_" + key.substring(key.lastIndexOf(":") + 1) + "_schema.json";
            Path targetStructureFilePath = Paths.get("src", moduleDirectoryName, "resources", structureFileName);
            String recordEntry = this.typeInformationMap.get(key);

            try {
                Utils.writeToFile(recordEntry, targetStructureFilePath);
            }catch(IOException e) {
                dlog.logDiagnostic(Diagnostic.Kind.ERROR, nodePosition, e.getMessage());
            }
        }

        Set<String> listOfModuleNames = new HashSet<String>();
        for (Map.Entry<String, String> entry : this.typeInformationMap.entrySet()) {
            String key = entry.getKey();
            String moduleName = null;
            if (!singleSourceFileFlag) {
                moduleName = key.substring(key.indexOf("/") + 1);
                moduleName = moduleName.substring(0, moduleName.indexOf(":"));
                listOfModuleNames.add(moduleName);
            }
        }

        for(String moduleName : listOfModuleNames) {
            processSampleDataFiles(moduleName);
        }

        if (listOfModuleNames.size() == 0) {
            processSampleDataFilesDefault();
        }
    }

    private void processSampleDataFilesDefault() {
        Path issueDataFilePath = Paths.get(".");
        List<String> listOfSampleDataJSONFiles = null;

        try (Stream<Path> walk = Files.walk(issueDataFilePath)) {
            listOfSampleDataJSONFiles = walk.map(x -> x.toString())
                    .filter(f -> f.endsWith("_data.json")).collect(Collectors.toList());

            for(String path : listOfSampleDataJSONFiles) {
                if (path.contains("/src/test/resources/data")) {
                    readDataArray(path);
                }
            }

            for (Map.Entry<String, ArrayList<JsonNode>> entry : this.sampleDataMap.entrySet()) {
                String key = entry.getKey();
                Path targetStructureFilePath = null;
                StringBuilder sb = new StringBuilder();
                if (singleSourceFileFlag) {
                    String structureFileName = key + "_data.json";
                    targetStructureFilePath = Paths.get(".", structureFileName);
                } else {
                    String moduleDirectoryName = key.substring(key.indexOf("/") + 1);
                    moduleDirectoryName = moduleDirectoryName.substring(0, moduleDirectoryName.indexOf(":"));
                    String structureFileName = moduleDirectoryName + "_" + key.substring(key.lastIndexOf(":") + 1) + "_data.json";
                    targetStructureFilePath = Paths.get("src", moduleDirectoryName, "resources", structureFileName);
                }

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
            dlog.logDiagnostic(Diagnostic.Kind.ERROR, nodePosition, e.getMessage());
        }
    }

    private void processSampleDataFiles(String moduleName) {
        Path issueDataFilePath = Paths.get("src", moduleName, "resources");
        List<String> listOfSampleDataJSONFiles = null;

        try (Stream<Path> walk = Files.walk(issueDataFilePath)) {
            listOfSampleDataJSONFiles = walk.map(x -> x.toString())
                    .filter(f -> f.endsWith("_data.json")).collect(Collectors.toList());

            for(String path : listOfSampleDataJSONFiles) {
                readDataArray(path);
            }

            for (Map.Entry<String, ArrayList<JsonNode>> entry : this.sampleDataMap.entrySet()) {
                String key = entry.getKey();
                String moduleDirectoryName = key.substring(key.indexOf("/") + 1);
                moduleDirectoryName = moduleDirectoryName.substring(0, moduleDirectoryName.indexOf(":"));
                String structureFileName = moduleDirectoryName + "_" + key.substring(key.lastIndexOf(":") + 1) + "_data.json";
                Path targetStructureFilePath = Paths.get("src", moduleDirectoryName, "resources", structureFileName);
                StringBuilder sb = new StringBuilder();
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
            dlog.logDiagnostic(Diagnostic.Kind.ERROR, nodePosition, e.getMessage());
        }
    }

    private List<String> readDataArray(String path) throws IOException {
        JsonFactory factory = new JsonFactory();
        JsonParser parser  = factory.createParser(new FileReader(path));

        List<String> messages = new ArrayList<String>();
        String typeName = null;
        JsonNode typeRecord = null;
        JsonNode dataRecord = null;
        int expectedNumberofAttributes = 0;
        int previousExpectedNumberofAttributes = 0;
        int attributeCounter = 0;
        long counter = -1;
        Stack<JsonNode> typeStack = new Stack<JsonNode>();
        Stack<Integer> attributeCounterStack = new Stack<Integer>();
        Stack<JsonLocation> startLocationStack = new Stack<JsonLocation>();
        String previousTypeName = null;
        String previousName = null;

        while (!parser.isClosed()) {
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
                    startLocation = parser.getCurrentLocation();
                    startLocationStack.push(startLocation);
                    if (counter == -1) {
                        counter = 1;
                    } else {
                        counter++;
                    }
                    break;
                case END_OBJECT:
                    startLocation = startLocationStack.pop();
                    if((expectedNumberofAttributes != attributeCounter) && (counter != 1)) {
                        endLocation = parser.getCurrentLocation();
                        BDiagnosticSource source = new BDiagnosticSource(lastPackageNode.packageID , path);
                        DiagnosticPos position = new DiagnosticPos(source, startLocation.getLineNr(), endLocation.getLineNr(),
                                startLocation.getColumnNr(), endLocation.getColumnNr());
                        dlog.logDiagnostic(Diagnostic.Kind.ERROR, position, "Error: Sample data provided for " +
                                typeName + " is different in terms of attributes count");
                    } else {
                        if(!typeStack.isEmpty()) {
                            ArrayList<JsonNode> lst = sampleDataMap.getOrDefault(typeName, new ArrayList<JsonNode>());
                            lst.add(dataRecord);
                            sampleDataMap.put(typeName, lst);
                            dataRecord = null;
                        }
                    }
                    if (!typeStack.isEmpty()) {
                        typeRecord = typeStack.pop();
                        attributeCounter = attributeCounterStack.pop();
                        typeName = previousTypeName;
                        expectedNumberofAttributes = previousExpectedNumberofAttributes;
                    } else {
                        attributeCounter = 0;
                        counter--;
                    }
                    break;
                    case FIELD_NAME:
                    final String name = parser.getCurrentName();
                    if (counter == 1) {
                        typeName = name;
                        String value = typeInformationMap.get(name);
                        typeRecord = constructJSON(value);
                        Iterator<String> iterator = typeRecord.get(name).fieldNames();
                        int attributeCount = 0;
                        while (iterator.hasNext()) {
                            attributeCount++;
                            iterator.next();
                        }
                        expectedNumberofAttributes = attributeCount;
                    } else if (counter == 2) {
                        if(typeRecord.get(typeName).get(name) == null) {
                            JsonLocation location = parser.getCurrentLocation();
                            BDiagnosticSource source = new BDiagnosticSource(lastPackageNode.packageID , path);
                            DiagnosticPos position = new DiagnosticPos(source, location.getLineNr(), location.getLineNr(),
                                    location.getColumnNr() - (name.length() + 5), location.getColumnNr() - 3);
                            dlog.logDiagnostic(Diagnostic.Kind.ERROR, position, "Error: Type " + typeName +
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
                        String value = typeInformationMap.get(typeName);
                        typeRecord = constructJSON(value);
                        dataRecord = constructJSON(value);
                        previousExpectedNumberofAttributes = expectedNumberofAttributes;

                        Iterator<String> iterator = typeRecord.get(typeName).fieldNames();
                        int attributeCount = 0;
                        while (iterator.hasNext()) {
                            attributeCount++;
                            iterator.next();
                        }

                        expectedNumberofAttributes = attributeCount;

                        if(!typeRecord.get(typeName).has(name)) {
                            JsonLocation location = parser.getCurrentLocation();
                            BDiagnosticSource source = new BDiagnosticSource(lastPackageNode.packageID , path);
                            DiagnosticPos position = new DiagnosticPos(source, location.getLineNr(), location.getLineNr(),
                                    location.getColumnNr() - (name.length() + 5), location.getColumnNr() - 3);
                            dlog.logDiagnostic(Diagnostic.Kind.ERROR, position, "Error: Type " +
                            typeName + " does not have an attribute named " + name);
                        }

                        counter--;
                    }

                    previousName = name;

                    break;
                case VALUE_STRING:
                    final String s = parser.getValueAsString();
                    if (dataRecord != null) {
                        ((ObjectNode)dataRecord.get(typeName)).put(previousName, s);
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
                    dlog.logDiagnostic(Diagnostic.Kind.ERROR, nodePosition, "Error: Unexpected JSON token value: " + jsonToken);
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

    private void extractFunctionCalls(BLangTypeDefinition typeDefinition) throws IOException {
        String name = typeDefinition.name.toString();
        Iterator<BLangFunction> iterator = ((BLangObjectTypeNode) typeDefinition.typeNode).functions.iterator();
        boolean flag = iterator.hasNext();
        while (flag) {
            BLangFunction function =  iterator.next();
            if (function.flagSet.contains(Flag.REMOTE)) {
                functions.append("{\"function\":{\"name\":\"" + function.name + "\",\"requiredParams\":[");
                Iterator<BLangSimpleVariable> iterator2 = function.requiredParams.iterator();
                while (iterator2.hasNext()) {
                    BLangSimpleVariable requiredParam = iterator2.next();
                    functions.append("{\"");
                    functions.append(requiredParam.name);
                    functions.append("\":\"");
                    String typeVariable = requiredParam.symbol.type.toString();
                    functions.append(typeVariable);
                    if(typeVariable.endsWith("?")) {
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
                            if(returnType.endsWith("?")) {
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
            }

            flag = iterator.hasNext();

            if (flag) {
                functions.append("\"}},");
            } else {
                functions.append("\"}}");
            }
        }

        String symbol = String.valueOf(((BLangTypeDefinition) typeDefinition).symbol);
        String moduleName = symbol.substring(symbol.indexOf("/") + 1);
        if (moduleName.contains(":")) {
            moduleName = moduleName.substring(0, moduleName.indexOf(":"));
        }

        String functionsJson = "{\"" + symbol.toString() + "\" : " + "[" + functions.toString() + "]}";
        functions = new StringBuilder();
        String functionsFileName = moduleName + "_" + name + "_functions.json";
        Path targetFunctionsFilePath = Paths.get("src", moduleName, "resources", functionsFileName);
        Utils.writeToFile(functionsJson, targetFunctionsFilePath);
    }
}
