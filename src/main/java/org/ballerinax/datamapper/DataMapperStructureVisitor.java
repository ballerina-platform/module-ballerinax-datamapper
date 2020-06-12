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

import org.ballerinalang.model.tree.NodeKind;
import org.ballerinalang.model.tree.expressions.RecordLiteralNode;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BObjectTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangNamedArgsExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeInit;
import org.wso2.ballerinalang.compiler.tree.types.BLangRecordTypeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataMapperStructureVisitor extends BLangNodeVisitor {
    private Map<String, String> structureMap;

    public DataMapperStructureVisitor(Map structure){
        structureMap = structure;
    }

    @Override
    public void visit(BLangTypeDefinition typeDefinition) {
        for (BLangSimpleVariable field: ((BLangRecordTypeNode) typeDefinition.typeNode).fields) {
            if (field.getKind() == NodeKind.VARIABLE) {
                field.accept(this);
            }
        }
    }

    @Override
    public void visit(BLangTypeInit varRefExpr) {
        List<BLangExpression> exprList = varRefExpr.argsExpr;
        ArrayList<BVarSymbol> list = (ArrayList<BVarSymbol>) ((BObjectTypeSymbol) varRefExpr.type.tsymbol).
                                    initializerFunc.symbol.params;
        for(BLangExpression expr : exprList) {
            switch(expr.getKind()) {
                case LITERAL:
                case NAMED_ARGS_EXPR:
                case RECORD_LITERAL_EXPR:
                case LIST_CONSTRUCTOR_EXPR:
                    expr.accept(this);
                    break;
            }
        }
    }

    @Override
    public void visit(BLangNamedArgsExpression varRefExpr) {
        BLangExpression expr = (BLangExpression) varRefExpr.getExpression();
        switch (expr.getKind()){
            case LITERAL:
            case RECORD_LITERAL_EXPR:
            case LIST_CONSTRUCTOR_EXPR:
                expr.accept(this);
                break;
        }
    }

    @Override
    public void visit(BLangListConstructorExpr listConstructorExpr) {
        List<BLangExpression> exprList = listConstructorExpr.getExpressions();

        for (BLangExpression expr : exprList) {
            if (expr.getKind() == NodeKind.RECORD_LITERAL_EXPR) {
                expr.accept(this);
            } else if (expr.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
                //Note: Here we do not need to travers further because we know that this is an array
                //and the type is known in advance in this method using listConstructorExpr.type
            }
        }
    }

    @Override
    public void visit(BLangSimpleVariable varNode) {
        structureMap.put(varNode.symbol.name.toString(), varNode.symbol.type.toString());
    }

    @Override
    public void visit(BLangRecordLiteral.BLangRecordKeyValueField recordKeyValue) {
        switch(recordKeyValue.valueExpr.getKind()) {
            case LITERAL:
                ((BLangLiteral)recordKeyValue.valueExpr).accept(this);
                break;
            case RECORD_LITERAL_EXPR:
            case LIST_CONSTRUCTOR_EXPR:
            case SIMPLE_VARIABLE_REF:
            case NUMERIC_LITERAL:
                recordKeyValue.valueExpr.accept(this);
                break;
        }
    }

    @Override
    public void visit(BLangRecordLiteral recordLiteral) {
        for(RecordLiteralNode.RecordField field : recordLiteral.fields) {
            if (field.getKind() == NodeKind.RECORD_LITERAL_KEY_VALUE) {
                ((BLangRecordLiteral.BLangRecordKeyValueField) field).accept(this);
            } else if (field.getKind() == NodeKind.LIST_CONSTRUCTOR_EXPR) {
                ((BLangListConstructorExpr) field).accept(this);
            }
        }
    }
}