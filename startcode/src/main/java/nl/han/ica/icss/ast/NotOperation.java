package nl.han.ica.icss.ast;

import java.util.ArrayList;

public class NotOperation extends Expression {
    public Expression operand;

    @Override
    public ASTNode addChild(ASTNode child) {
        if (!(child instanceof Expression)) {
            throw new IllegalArgumentException("NOT verwacht een Expression");
        }
        operand = (Expression) child;
        return this;
    }

    @Override
    public ArrayList<ASTNode> getChildren() {
        ArrayList<ASTNode> children = new ArrayList<>();
        if (operand != null) children.add(operand);
        return children;
    }

    @Override
    public String getNodeLabel() {
        return "Not";
    }
}
