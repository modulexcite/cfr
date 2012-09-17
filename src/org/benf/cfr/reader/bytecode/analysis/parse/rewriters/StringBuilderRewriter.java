package org.benf.cfr.reader.bytecode.analysis.parse.rewriters;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.*;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.util.ListFactory;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: lee
 * Date: 17/09/2012
 * Time: 06:43
 */
public class StringBuilderRewriter implements ExpressionRewriter {
    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        if (expression instanceof MemberFunctionInvokation) {
            MemberFunctionInvokation memberFunctionInvokation = (MemberFunctionInvokation) expression;
            if (memberFunctionInvokation.getName().equals("toString")) {
                Expression lhs = memberFunctionInvokation.getObject();
                Expression result = testAppendChain(lhs);
                if (result != null) return result;
            }
        }
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer);
    }

    @Override
    public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer);
        return (ConditionalExpression) res;
    }

    @Override
    public AbstractAssignmentExpression rewriteExpression(AbstractAssignmentExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer);
        return (AbstractAssignmentExpression) res;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        return lValue;
    }

    private Expression testAppendChain(Expression lhs) {
        List<Expression> reverseAppendChain = ListFactory.newList();
        do {
            if (lhs instanceof MemberFunctionInvokation) {
                MemberFunctionInvokation memberFunctionInvokation = (MemberFunctionInvokation) lhs;
                if (memberFunctionInvokation.getName().equals("append") &&
                        memberFunctionInvokation.getArgs().size() == 1) {
                    lhs = memberFunctionInvokation.getObject();
                    reverseAppendChain.add(memberFunctionInvokation.getArgs().get(0));
                } else {
                    return null;
                }
            } else if (lhs instanceof ConstructorInvokation) {
                ConstructorInvokation newObject = (ConstructorInvokation) lhs;
                String rawName = newObject.getTypeInstance().getRawName();
                if (rawName.equals("java.lang.StringBuilder")) {
                    return genStringConcat(reverseAppendChain);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } while (lhs != null);
        return null;
    }

    private Expression genStringConcat(List<Expression> revList) {
        int x = revList.size() - 1;
        if (x < 0) return null;
        Expression head = revList.get(x);
        for (--x; x >= 0; --x) {
            head = new ArithmeticOperation(head, revList.get(x), ArithOp.PLUS);
        }
        return head;
    }
}