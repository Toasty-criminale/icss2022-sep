package nl.han.ica.icss.parser;

import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import org.antlr.v4.runtime.ParserRuleContext;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.icss.ast.selectors.*;
import nl.han.ica.icss.ast.NotOperation;
import nl.han.ica.icss.ast.ComparisonOperation;
import nl.han.ica.icss.ast.Expression;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {

    //Accumulator attributes:
    private AST ast;

    //Use this to keep track of the parent nodes when recursively traversing the ast
    private IHANStack<ASTNode> currentContainer;

    private Map<ParserRuleContext, Expression> expressions = new IdentityHashMap<>();

    public ASTListener() {
        ast = new AST();
        currentContainer = new HANStack<>();
    }

    public AST getAST() {
        return ast;
    }

    @Override
    public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
        currentContainer.push(ast.root);
    }

    @Override
    public void exitStylesheet(ICSSParser.StylesheetContext ctx) {
        currentContainer.pop();
        expressions.clear();
    }

    @Override
    public void enterStylerule(ICSSParser.StyleruleContext ctx) {
        Stylerule rule = new Stylerule();
        ICSSParser.SelectorContext selector = ctx.selector();
        if (selector.LOWER_IDENT() != null) {
            rule.addChild(new TagSelector(selector.getText()));
        } else if (selector.ID_IDENT() != null) {
            rule.addChild(new IdSelector(selector.getText()));
        } else {
            rule.addChild(new ClassSelector(selector.getText()));
        }
        currentContainer.peek().addChild(rule);
        currentContainer.push(rule);
    }

    @Override
    public void exitStylerule(ICSSParser.StyleruleContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void exitAtom(ICSSParser.AtomContext ctx) {
        Expression value;
        String text = ctx.getText();
        if (ctx.expression() != null) value = expressions.get(ctx.expression());
        else if (ctx.PIXELSIZE() != null) value = new PixelLiteral(text);
        else if (ctx.PERCENTAGE() != null) value = new PercentageLiteral(text);
        else if (ctx.SCALAR() != null) value = new ScalarLiteral(text);
        else if (ctx.COLOR() != null) value = new ColorLiteral(text);
        else if (ctx.TRUE() != null || ctx.FALSE() != null) value = new BoolLiteral(text);
        else value = new VariableReference(text);
        expressions.put(ctx, value);
    }

    @Override
    public void exitUnary(ICSSParser.UnaryContext ctx) {
        if (ctx.NOT() != null) {
            NotOperation not = new NotOperation();
            not.addChild(expressions.get(ctx.unary()));
            expressions.put(ctx, not);
        } else {
            expressions.put(ctx, expressions.get(ctx.atom()));
        }
    }

    @Override
    public void exitTerm(ICSSParser.TermContext ctx) {
        Expression result = expressions.get(ctx.unary(0));
        for (int index = 1; index < ctx.unary().size(); index++) {
            MultiplyOperation multiply = new MultiplyOperation();
            multiply.addChild(result);
            multiply.addChild(expressions.get(ctx.unary(index)));
            result = multiply;
        }
        expressions.put(ctx, result);
    }

    @Override
    public void exitAddition(ICSSParser.AdditionContext ctx) {
        Expression result = expressions.get(ctx.term(0));
        for (int index = 1; index < ctx.term().size(); index++) {
            Operation operation = ctx.addOperator(index - 1).PLUS() != null
                    ? new AddOperation() : new SubtractOperation();
            operation.addChild(result);
            operation.addChild(expressions.get(ctx.term(index)));
            result = operation;
        }
        expressions.put(ctx, result);
    }

    @Override
    public void exitComparison(ICSSParser.ComparisonContext ctx) {
        Expression result = expressions.get(ctx.addition(0));
        if (ctx.comparisonOperator() != null) {
            String symbol = ctx.comparisonOperator().getText();
            ComparisonOperation.Operator operator;
            switch (symbol) {
                case ">":
                    operator = ComparisonOperation.Operator.GREATER;
                    break;
                case "<":
                    operator = ComparisonOperation.Operator.LESS;
                    break;
                case ">=":
                    operator = ComparisonOperation.Operator.GREATER_EQUAL;
                    break;
                case "<=":
                    operator = ComparisonOperation.Operator.LESS_EQUAL;
                    break;
                case "==":
                    operator = ComparisonOperation.Operator.EQUAL;
                    break;
                case "!=":
                    operator = ComparisonOperation.Operator.NOT_EQUAL;
                    break;
                default:
                    throw new IllegalStateException("Onbekende operator: " + symbol);
            }
            ComparisonOperation comparison = new ComparisonOperation(operator);
            comparison.addChild(result);
            comparison.addChild(expressions.get(ctx.addition(1)));
            result = comparison;
        }
        expressions.put(ctx, result);
    }

    @Override
    public void exitExpression(ICSSParser.ExpressionContext ctx) {
        expressions.put(ctx, expressions.get(ctx.comparison()));
    }

    @Override
    public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
        Declaration declaration = new Declaration(ctx.LOWER_IDENT().getText());
        declaration.addChild(expressions.get(ctx.expression()));
        currentContainer.peek().addChild(declaration);
    }

    @Override
    public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
        VariableAssignment assignment = new VariableAssignment();
        assignment.addChild(new VariableReference(ctx.CAPITAL_IDENT().getText()));
        assignment.addChild(expressions.get(ctx.expression()));
        currentContainer.peek().addChild(assignment);
    }

    @Override
    public void enterIfClause(ICSSParser.IfClauseContext ctx) {
        IfClause clause = new IfClause();
        currentContainer.peek().addChild(clause);
        currentContainer.push(clause);
    }

    @Override
    public void exitIfClause(ICSSParser.IfClauseContext ctx) {
        IfClause clause = (IfClause) currentContainer.peek();
        clause.addChild(expressions.get(ctx.expression()));
        currentContainer.pop();
    }

    @Override
    public void enterElseClause(ICSSParser.ElseClauseContext ctx) {
        ElseClause clause = new ElseClause();
        currentContainer.peek().addChild(clause);
        currentContainer.push(clause);
    }

    @Override
    public void exitElseClause(ICSSParser.ElseClauseContext ctx) {
        currentContainer.pop();
    }
}