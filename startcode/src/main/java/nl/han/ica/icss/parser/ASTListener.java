package nl.han.ica.icss.parser;

import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;
import org.antlr.v4.runtime.ParserRuleContext;

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
        if (ctx.PIXELSIZE() != null) value = new PixelLiteral(text);
        else if (ctx.PERCENTAGE() != null) value = new PercentageLiteral(text);
        else if (ctx.SCALAR() != null) value = new ScalarLiteral(text);
        else if (ctx.COLOR() != null) value = new ColorLiteral(text);
        else if (ctx.TRUE() != null || ctx.FALSE() != null) value = new BoolLiteral(text);
        else value = new VariableReference(text);
        expressions.put(ctx, value);
    }

    @Override
    public void exitTerm(ICSSParser.TermContext ctx) {
        Expression result = expressions.get(ctx.atom(0));
        for (int index = 1; index < ctx.atom().size(); index++) {
            MultiplyOperation multiply = new MultiplyOperation();
            multiply.addChild(result);
            multiply.addChild(expressions.get(ctx.atom(index)));
            result = multiply;
        }
        expressions.put(ctx, result);
    }

    @Override
    public void exitExpression(ICSSParser.ExpressionContext ctx) {
        Expression result = expressions.get(ctx.term(0));
        for (int index = 1; index < ctx.term().size(); index++) {
            Operation operation;
            if (ctx.addOperator(index - 1).PLUS() != null) operation = new AddOperation();
            else operation = new SubtractOperation();
            operation.addChild(result);
            operation.addChild(expressions.get(ctx.term(index)));
            result = operation;
        }
        expressions.put(ctx, result);
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