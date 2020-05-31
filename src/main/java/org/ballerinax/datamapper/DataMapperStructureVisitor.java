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
import org.wso2.ballerinalang.compiler.tree.expressions.BLangNumericLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeInit;
import org.wso2.ballerinalang.compiler.tree.types.BLangRecordTypeNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataMapperStructureVisitor extends BLangNodeVisitor {
    private StringBuilder structureStringBuilder;

    public DataMapperStructureVisitor(StringBuilder structure){
        structureStringBuilder = structure;
    }

    @Override
    public void visit(BLangTypeDefinition typeDefinition) {
        structureStringBuilder.append("{");
        structureStringBuilder.append("\"");
        structureStringBuilder.append(typeDefinition.typeNode.type.tsymbol.toString());
        structureStringBuilder.append("\": {");
        for (Iterator<BLangSimpleVariable> iterator = ((BLangRecordTypeNode) typeDefinition.typeNode).
            fields.iterator(); iterator.hasNext(); ) {
            BLangSimpleVariable field = iterator.next();

            if (field.getKind() == NodeKind.VARIABLE) {
                field.accept(this);
            }

            if (iterator.hasNext()) {
                structureStringBuilder.append(",");
            }
        }
        structureStringBuilder.append("}} ");
    }

    @Override
    public void visit(BLangTypeInit varRefExpr) {
        structureStringBuilder.append("{");
        List<BLangExpression> exprList = varRefExpr.argsExpr;
        ArrayList<BVarSymbol> list = (ArrayList<BVarSymbol>) ((BObjectTypeSymbol) varRefExpr.type.tsymbol).
                                    initializerFunc.symbol.params;
        int count = 0;
        for (Iterator<BLangExpression> iterator = exprList.iterator(); iterator.hasNext(); ) {
            BLangExpression expr = iterator.next();
            structureStringBuilder.append("\"");
            if (expr.getKind() == NodeKind.LITERAL) {
                structureStringBuilder.append(list.get(count));
                structureStringBuilder.append("\":");
                expr.accept(this);
            } else if (expr.getKind() == NodeKind.NAMED_ARGS_EXPR) {
                expr.accept(this);
            } else if (expr.getKind() == NodeKind.RECORD_LITERAL_EXPR) {
                structureStringBuilder.append(list.get(count));
                structureStringBuilder.append("\":");
                expr.accept(this);
            } else if (expr.getKind() == NodeKind.LIST_CONSTRUCTOR_EXPR) {
                structureStringBuilder.append(list.get(count));
                structureStringBuilder.append("\":");
                expr.accept(this);
            }
            count += 1;
        }
        structureStringBuilder.append("} ");
    }

    @Override
    public void visit(BLangNamedArgsExpression varRefExpr) {
        structureStringBuilder.append(varRefExpr.name);
        structureStringBuilder.append("\":");
        BLangExpression expr = (BLangExpression) varRefExpr.getExpression();
        if (expr.getKind() == NodeKind.LITERAL) {
            expr.accept(this);
        } else if (expr.getKind() == NodeKind.RECORD_LITERAL_EXPR) {
            expr.accept(this);
        } else if (expr.getKind() == NodeKind.LIST_CONSTRUCTOR_EXPR) {
            expr.accept(this);
        }
    }

    @Override
    public void visit(BLangSimpleVarRef varRefExpr) {
        structureStringBuilder.append("\"");
        structureStringBuilder.append(varRefExpr.type);
        structureStringBuilder.append("\"");
        structureStringBuilder.append(",");
    }

    @Override
    public void visit(BLangListConstructorExpr listConstructorExpr) {
        List<BLangExpression> exprList = listConstructorExpr.getExpressions();
        structureStringBuilder.append("\"");
        structureStringBuilder.append(listConstructorExpr.type);
        structureStringBuilder.append("\",");

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
        structureStringBuilder.append("\"");
        structureStringBuilder.append(varNode.symbol.name);
        structureStringBuilder.append("\":\"");
        structureStringBuilder.append(varNode.symbol.type);
        structureStringBuilder.append("\"");
    }

    @Override
    public void visit(BLangLiteral literalExpr) {
        structureStringBuilder.append("\"");
        structureStringBuilder.append(literalExpr.type);
        structureStringBuilder.append("\"");
        structureStringBuilder.append(",");
    }

    @Override
    public void visit(BLangNumericLiteral literalExpr) {
        structureStringBuilder.append("\"");
        structureStringBuilder.append(literalExpr.type);
        structureStringBuilder.append("\"");
        structureStringBuilder.append(",");
    }

    @Override
    public void visit(BLangRecordLiteral.BLangRecordKeyValueField recordKeyValue) {
        structureStringBuilder.append("\"");
        structureStringBuilder.append(recordKeyValue.getKey());
        structureStringBuilder.append("\":");
        if (recordKeyValue.valueExpr.getKind() == NodeKind.LITERAL) {
            ((BLangLiteral)recordKeyValue.valueExpr).accept(this);
        } else if (recordKeyValue.valueExpr.getKind() == NodeKind.RECORD_LITERAL_EXPR) {
            recordKeyValue.valueExpr.accept(this);
        } else if (recordKeyValue.valueExpr.getKind() == NodeKind.LIST_CONSTRUCTOR_EXPR) {
            recordKeyValue.valueExpr.accept(this);
        } else if (recordKeyValue.valueExpr.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
            recordKeyValue.valueExpr.accept(this);
        } else if (recordKeyValue.valueExpr.getKind() == NodeKind.NUMERIC_LITERAL) {
            recordKeyValue.valueExpr.accept(this);
        }
    }

    @Override
    public void visit(BLangRecordLiteral recordLiteral) {
        if (recordLiteral.fields.size() == 0) {
            structureStringBuilder.append("\"");
            structureStringBuilder.append(recordLiteral.type);
            structureStringBuilder.append("\",");
        } else {
            structureStringBuilder.append("{");
        }

        for (Iterator<RecordLiteralNode.RecordField> iterator = recordLiteral.fields.iterator(); iterator.hasNext(); ) {
            RecordLiteralNode.RecordField field = iterator.next();

            if (field.getKind() == NodeKind.RECORD_LITERAL_KEY_VALUE) {
                ((BLangRecordLiteral.BLangRecordKeyValueField) field).accept(this);
            } else if (field.getKind() == NodeKind.LIST_CONSTRUCTOR_EXPR) {
                ((BLangListConstructorExpr) field).accept(this);
            }
        }

        if (recordLiteral.fields.size() != 0) {
            structureStringBuilder.append("},");
        }
    }
}