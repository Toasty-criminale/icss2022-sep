package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import java.util.ArrayList;
import java.util.HashMap;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;

public class Evaluator implements Transform {

    private IHANLinkedList<HashMap<String, Literal>> variableValues;

    public Evaluator() {
        variableValues = new HANLinkedList<>();
    }

    @Override
    public void apply(AST ast) {
        variableValues = new HANLinkedList<>();
        enterScope();
        process(ast.root.body);
        exitScope();
    }

    private void enterScope() {
        variableValues.addFirst(new HashMap<>());
    }

    private void exitScope() {
        variableValues.removeFirst();
    }

    private Literal lookup(String name) {
        for (int i = 0; i < variableValues.getSize(); i++) {
            HashMap<String, Literal> scope = variableValues.get(i);
            if (scope.containsKey(name)) return scope.get(name);
        }
        throw new IllegalStateException("Niet bestaande variabele: " + name);
    }

    private void assign(String name, Literal value) {
        for (int i = 0; i < variableValues.getSize(); i++) {
            HashMap<String, Literal> scope = variableValues.get(i);
            if (scope.containsKey(name)) {
                scope.put(name, value);
                return;
            }
        }
        variableValues.getFirst().put(name, value);
    }

    private void process(ArrayList<ASTNode> body) {
        ArrayList<ASTNode> transformed = new ArrayList<>();
        for (ASTNode node : body) {
            if (node instanceof VariableAssignment) {
                VariableAssignment assignment = (VariableAssignment) node;
                Literal value = calculate(assignment.expression);
                assignment.expression = value;
                assign(assignment.name.name, value);
                transformed.add(assignment);
            } else if (node instanceof Declaration) {
                Declaration declaration = (Declaration) node;
                declaration.expression = calculate(declaration.expression);
                transformed.add(declaration);
            } else if (node instanceof Stylerule) {
                Stylerule rule = (Stylerule) node;
                enterScope();
                process(rule.body);
                exitScope();
                transformed.add(rule);
            } else if (node instanceof IfClause) {
                IfClause clause = (IfClause) node;
                Literal literal = calculate(clause.conditionalExpression);
                if (!(literal instanceof BoolLiteral)) {
                    throw new IllegalStateException("If-conditie is geen boolean");
                }
                boolean condition = ((BoolLiteral) literal).value;
                ArrayList<ASTNode> selected = new ArrayList<>();
                if (condition) selected.addAll(clause.body);
                else if (clause.elseClause != null) selected.addAll(clause.elseClause.body);
                enterScope();
                process(selected);
                exitScope();
                transformed.addAll(selected);
            } else {
                transformed.add(node);
            }
        }
        body.clear();
        body.addAll(transformed);
    }

    private Literal calculate(Expression expression) {
        if (expression instanceof Literal) return (Literal) expression;
        if (expression instanceof VariableReference) {
            return lookup(((VariableReference) expression).name);
        }
        if (expression instanceof NotOperation) {
            Literal operand = calculate(((NotOperation) expression).operand);
            if (!(operand instanceof BoolLiteral)) {
                throw new IllegalStateException("! vereist een boolean");
            }
            return new BoolLiteral(!((BoolLiteral) operand).value);
        }
        if (expression instanceof ComparisonOperation) {
            return calculateComparison((ComparisonOperation) expression);
        }
        if (!(expression instanceof Operation)) {
            throw new IllegalStateException("Niet ondersteunde expressie: " + expression.getNodeLabel());
        }
        Operation operation = (Operation) expression;
        Literal left = calculate(operation.lhs);
        Literal right = calculate(operation.rhs);
        int a = number(left);
        int b = number(right);
        if (operation instanceof AddOperation) return sameUnit(left, Math.addExact(a, b));
        if (operation instanceof SubtractOperation) return sameUnit(left, Math.subtractExact(a, b));
        if (operation instanceof MultiplyOperation) {
            int product = Math.multiplyExact(a, b);
            if (left instanceof ScalarLiteral) return sameUnit(right, product);
            if (right instanceof ScalarLiteral) return sameUnit(left, product);
        }
        throw new IllegalStateException("Ongeldige operatie; eerst Checker uitvoeren");
    }

    private BoolLiteral calculateComparison(ComparisonOperation comparison) {
        Literal left = calculate(comparison.lhs);
        Literal right = calculate(comparison.rhs);
        boolean equal;
        int order = 0;

        if (left instanceof BoolLiteral && right instanceof BoolLiteral) {
            equal = ((BoolLiteral) left).value == ((BoolLiteral) right).value;
        } else if (left instanceof ColorLiteral && right instanceof ColorLiteral) {
            equal = ((ColorLiteral) left).value.equals(((ColorLiteral) right).value);
        } else {
            int a = number(left);
            int b = number(right);
            equal = a == b;
            order = Integer.compare(a, b);
        }

        boolean result;
        switch (comparison.operator) {
            case GREATER:
                result = order > 0;
                break;
            case LESS:
                result = order < 0;
                break;
            case GREATER_EQUAL:
                result = order >= 0;
                break;
            case LESS_EQUAL:
                result = order <= 0;
                break;
            case EQUAL:
                result = equal;
                break;
            case NOT_EQUAL:
                result = !equal;
                break;
            default:
                throw new IllegalStateException("Onbekende vergelijking");
        }
        return new BoolLiteral(result);
    }

    private int number(Literal literal) {
        if (literal instanceof PixelLiteral) return ((PixelLiteral) literal).value;
        if (literal instanceof PercentageLiteral) return ((PercentageLiteral) literal).value;
        if (literal instanceof ScalarLiteral) return ((ScalarLiteral) literal).value;
        throw new IllegalStateException("Een kleur/boolean is niet numeriek");
    }

    private Literal sameUnit(Literal template, int value) {
        if (template instanceof PixelLiteral) return new PixelLiteral(value);
        if (template instanceof PercentageLiteral) return new PercentageLiteral(value);
        if (template instanceof ScalarLiteral) return new ScalarLiteral(value);
        throw new IllegalStateException("Ongeldige numerieke eenheid");
    }
}
