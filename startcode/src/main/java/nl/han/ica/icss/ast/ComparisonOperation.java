package nl.han.ica.icss.ast;

public class ComparisonOperation extends Operation {
    public enum Operator {
        GREATER, LESS, GREATER_EQUAL, LESS_EQUAL, EQUAL, NOT_EQUAL
    }

    public final Operator operator;

    public ComparisonOperation(Operator operator) {
        this.operator = operator;
    }

    @Override
    public String getNodeLabel() {
        return "Comparison (" + operator + ")";
    }
}
