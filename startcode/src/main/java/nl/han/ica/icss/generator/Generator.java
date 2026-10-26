package nl.han.ica.icss.generator;


import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;

public class Generator {

    public String generate(AST ast) {
        StringBuilder css = new StringBuilder();
        for (ASTNode node : ast.root.body) {
            if (node instanceof Stylerule) writeRule((Stylerule) node, css);
        }
        return css.toString();
    }

    private void writeRule(Stylerule rule, StringBuilder css) {
        for (int i = 0; i < rule.selectors.size(); i++) {
            if (i > 0) css.append(", ");
            css.append(rule.selectors.get(i).toString());
        }
        css.append(" {\n");
        for (ASTNode node : rule.body) {
            if (node instanceof Declaration) {
                Declaration declaration = (Declaration) node;
                css.append("  ").append(declaration.property.name).append(": ");
                css.append(cssValue(declaration.expression)).append(";\n");
            } else if (node instanceof IfClause) {
                throw new IllegalStateException("Evaluator moet draaien voor Generator.");
            }
        }
        css.append("}\n");
    }

    private String cssValue(Expression expression) {
        if (expression instanceof PixelLiteral) return ((PixelLiteral) expression).value + "px";
        if (expression instanceof PercentageLiteral) return ((PercentageLiteral) expression).value + "%";
        if (expression instanceof ColorLiteral) return ((ColorLiteral) expression).value;
        throw new IllegalStateException("Verwacht CSS-literal, gekregen: " + expression.getNodeLabel());
    }


}
