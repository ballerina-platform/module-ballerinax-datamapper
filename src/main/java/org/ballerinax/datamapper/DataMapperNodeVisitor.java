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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.impl.symbols.BallerinaClassSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleCompilation;
import org.ballerinax.datamapper.exceptions.DataMapperException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Visitor to extract Record Type Structure information.
 */
public class DataMapperNodeVisitor extends NodeVisitor {
    private final HashMap<String, String> recordTypes;
    private final HashMap<String, Map<String, FunctionRecord>> clientMap;
    private SemanticModel model;

    public DataMapperNodeVisitor() {
        this.recordTypes = new HashMap<String, String>();
        this.clientMap = new HashMap<>();
    }

    public HashMap<String, String> getRecordTypes() {
        return recordTypes;
    }

    public HashMap<String, Map<String, FunctionRecord>> getClientMap() {
        return clientMap;
    }

    public void setModule(Module module) {
        ModuleCompilation compilation = module.getCompilation();
        this.model = compilation.getSemanticModel();
    }

    private String getFieldTypes(Map<String, RecordFieldSymbol> fieldSymbolMap) {
        Iterator<String> iterator = fieldSymbolMap.keySet().iterator();
        Map<String, String> fieldSymbols = new HashMap<>();
        String serialized = null;
        while (iterator.hasNext()) {
            String fieldName = iterator.next();
            String fieldType = fieldSymbolMap.get(fieldName).typeDescriptor().signature();
            fieldSymbols.put(fieldName, fieldType);
        }
        try {
            serialized = new ObjectMapper().writeValueAsString(fieldSymbols);
        } catch (JsonProcessingException e) {
            throw new DataMapperException(e);
        }
        return serialized;
    }

    @Override
    public void visit(TypeDefinitionNode typeDefinitionNode) {
        if (typeDefinitionNode.typeDescriptor().kind() == SyntaxKind.RECORD_TYPE_DESC) {
            Optional<Symbol> recordSymbolOpt = this.model.symbol(typeDefinitionNode);
            if (recordSymbolOpt.isPresent()) {
                Symbol recordSymbol = recordSymbolOpt.get();
                Optional<String> recordNameOpt = recordSymbol.getName();
                if (recordNameOpt.isPresent()) {
                    String recordName = recordNameOpt.get();
                    String recordSignature = recordSymbol.getModule().get().id().toString();
                    recordName = recordSignature + ":" + recordName;
                    Map<String, RecordFieldSymbol> fieldSymbolMap = ((RecordTypeSymbol) ((TypeDefinitionSymbol)
                            recordSymbol).typeDescriptor()).fieldDescriptors();
                    String serialized = getFieldTypes(fieldSymbolMap);
                    serialized = "{\"" + recordName + "\":" + serialized + "}";
                    this.recordTypes.put(recordName, serialized);
                }
            }
        }
    }

    @Override
    public void visit(ClassDefinitionNode classDefinitionNode) {
        NodeList<Token> classTypeQualifiers = classDefinitionNode.classTypeQualifiers();
        for (Token classTypeQualifier : classTypeQualifiers) {
            if (classTypeQualifier.text().equals("client")) {
                Optional<Symbol> classSymbolOpt = model.symbol(classDefinitionNode);
                if (classSymbolOpt.isPresent()) {
                    Symbol classSymbol = classSymbolOpt.get();
                    if (classSymbol.getName().isEmpty()) {
                        continue;
                    }
                    String clientName = classSymbol.getName().get();
                    Collection<MethodSymbol> methods = ((BallerinaClassSymbol) classSymbol).methods().values();
                    Map<String, FunctionRecord> functionMap = new HashMap<>();
                    for (MethodSymbol method : methods) {
                        if (!method.qualifiers().contains(Qualifier.REMOTE)) {
                            continue;
                        }
                        String functionName = method.getName().get();
                        List<String> paraType = new ArrayList<>();
                        FunctionRecord functionRecord = new FunctionRecord();
                        for (ParameterSymbol parameter : method.typeDescriptor().parameters()) {
                            if (parameter.getName().isPresent()) {
                                String parameterName = parameter.getName().get();
                                if (parameter.typeDescriptor().typeKind() == TypeDescKind.UNION) {
                                    List<TypeSymbol> paraList = ((UnionTypeSymbol) parameter.typeDescriptor()).
                                            memberTypeDescriptors();
                                    for (TypeSymbol typeSymbol : paraList) {
                                        if (typeSymbol.typeKind() == TypeDescKind.ERROR ||
                                                typeSymbol.typeKind() == TypeDescKind.NIL) {
                                            continue;
                                        } else {
                                            paraType.add(typeSymbol.signature());
                                        }
                                    }
                                } else {
                                    paraType.add(parameter.typeDescriptor().signature());
                                }
                                functionRecord.addParameter(parameterName, paraType);
                                paraType = new ArrayList<>();
                            }
                        }
                        TypeSymbol returnTypeSymbol = method.typeDescriptor().returnTypeDescriptor().get();
                        if (returnTypeSymbol.typeKind() == TypeDescKind.UNION) {
                            List<TypeSymbol> returnList = ((UnionTypeSymbol) returnTypeSymbol).
                                    memberTypeDescriptors();
                            for (TypeSymbol typeSymbol : returnList) {
                                if (typeSymbol.typeKind() == TypeDescKind.ERROR) {
                                    continue;
                                } else {
                                    functionRecord.addReturnType(typeSymbol.signature());
                                }
                            }
                        } else {
                            functionRecord.addReturnType(returnTypeSymbol.signature());
                        }
                        functionMap.put(functionName, functionRecord);
                    }
                    if (!functionMap.isEmpty()) {
                        this.clientMap.put(clientName, functionMap);
                    }
                }
            }
        }
    }
}
