package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.icss.ast.types.ExpressionType;
import java.util.ArrayList;
import java.util.HashMap;

public class Checker {

    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        variableTypes = new HANLinkedList<>();
        enterScope();
        checkBody(ast.root.body);
        exitScope();
    }

    private void enterScope() {
        variableTypes.addFirst(new HashMap<>());
    }

    private void exitScope() {
        variableTypes.removeFirst();
    }

    private ExpressionType getVariableType(String name, ASTNode usage) {
        for (int i = 0; i < variableTypes.getSize(); i++) {
            HashMap<String, ExpressionType> scope = variableTypes.get(i);
            if (scope.containsKey(name)) return scope.get(name);
        }
        usage.setError("Variabele niet gedefinieerd of buiten scope: " + name);
        return ExpressionType.UNDEFINED;
    }

    private void register(String name, ExpressionType type) {
        for (int i = 0; i < variableTypes.getSize(); i++) {
            HashMap<String, ExpressionType> scope = variableTypes.get(i);
            if (scope.containsKey(name)) {
                scope.put(name, type);
                return;
            }
        }
        variableTypes.getFirst().put(name, type);
    }

    private void checkBody(ArrayList<ASTNode> body) {
        for (ASTNode node : body) {
            if (node instanceof VariableAssignment) {
                VariableAssignment assignment = (VariableAssignment) node;
                ExpressionType type = typeOf(assignment.expression);
                register(assignment.name.name, type);
            } else if (node instanceof Declaration) {
                checkDeclaration((Declaration) node);
            } else if (node instanceof Stylerule) {
                Stylerule rule = (Stylerule) node;
                enterScope();
                checkBody(rule.body);
                exitScope();
            } else if (node instanceof IfClause) {
                checkIf((IfClause) node);
            }
        }
    }

    private void checkIf(IfClause clause) {
        ExpressionType condition = typeOf(clause.conditionalExpression);
        if (condition != ExpressionType.BOOL && condition != ExpressionType.UNDEFINED) {
            clause.conditionalExpression.setError("Een if-conditie moet Boolean (BOOL) zijn");
        }
        enterScope();
        checkBody(clause.body);
        exitScope();
        if (clause.elseClause != null) {
            enterScope();
            checkBody(clause.elseClause.body);
            exitScope();
        }
    }

    private void checkDeclaration(Declaration declaration) {
        ExpressionType valueType = typeOf(declaration.expression);
        if (valueType == ExpressionType.UNDEFINED) return;
        String property = declaration.property.name;
        boolean valid;
        switch (property) {
            case "color":
            case "background-color":
                valid = valueType == ExpressionType.COLOR;
                break;
            case "width":
            case "height":
                valid = valueType == ExpressionType.PIXEL || valueType == ExpressionType.PERCENTAGE;
                break;
            default:
                valid = false;
        }
        if (!valid) declaration.setError("Ongeldige type/property-combinatie: " + property);
    }

    private ExpressionType comparisonType(ComparisonOperation comparison) {
        ExpressionType left = typeOf(comparison.lhs);
        ExpressionType right = typeOf(comparison.rhs);
        if (left == ExpressionType.UNDEFINED || right == ExpressionType.UNDEFINED) {
            return ExpressionType.UNDEFINED;
        }
        if (left != right) {
            comparison.setError("Vergelijkingen vereisen gelijke types");
            return ExpressionType.UNDEFINED;
        }
        boolean numeric = left == ExpressionType.PIXEL ||
                left == ExpressionType.PERCENTAGE ||
                left == ExpressionType.SCALAR;
        switch (comparison.operator) {
            case GREATER:
            case LESS:
            case GREATER_EQUAL:
            case LESS_EQUAL:
                if (!numeric) {
                    comparison.setError("<, >, <= en >= vereisen numerieke waardes");
                    return ExpressionType.UNDEFINED;
                }
                return ExpressionType.BOOL;
            case EQUAL:
            case NOT_EQUAL:
                return ExpressionType.BOOL;
            default:
                comparison.setError("Onbekende vergelijking");
                return ExpressionType.UNDEFINED;
        }
    }

    private ExpressionType typeOf(Expression expression) {
        if (expression instanceof PixelLiteral) return ExpressionType.PIXEL;
        if (expression instanceof PercentageLiteral) return ExpressionType.PERCENTAGE;
        if (expression instanceof ColorLiteral) return ExpressionType.COLOR;
        if (expression instanceof ScalarLiteral) return ExpressionType.SCALAR;
        if (expression instanceof BoolLiteral) return ExpressionType.BOOL;
        if (expression instanceof VariableReference) {
            VariableReference reference = (VariableReference) expression;
            return getVariableType(reference.name, reference);
        }
        if (expression instanceof NotOperation) {
            NotOperation not = (NotOperation) expression;
            ExpressionType operandType = typeOf(not.operand);
            if (operandType == ExpressionType.UNDEFINED) return ExpressionType.UNDEFINED;
            if (operandType != ExpressionType.BOOL) {
                not.setError("! mag alleen voor Boolean (BOOL) worden gebruikt");
                return ExpressionType.UNDEFINED;
            }
            return ExpressionType.BOOL;
        }
        if (expression instanceof ComparisonOperation) {
            return comparisonType((ComparisonOperation) expression);
        }
        if (expression instanceof Operation) {
            Operation operation = (Operation) expression;
            ExpressionType lhs = typeOf(operation.lhs);
            ExpressionType rhs = typeOf(operation.rhs);
            if (lhs == ExpressionType.UNDEFINED || rhs == ExpressionType.UNDEFINED) {
                return ExpressionType.UNDEFINED;
            }
            if (lhs == ExpressionType.COLOR || rhs == ExpressionType.COLOR) {
                operation.setError("Kleurcodes kunnen niet worden gebruikt in berekeningen");
                return ExpressionType.UNDEFINED;
            }
            if (lhs == ExpressionType.BOOL || rhs == ExpressionType.BOOL) {
                operation.setError("Booleans kunnen niet worden gebruikt in berekeningen");
                return ExpressionType.UNDEFINED;
            }
            if (operation instanceof AddOperation || operation instanceof SubtractOperation) {
                if (lhs == rhs) return lhs;
                operation.setError("Bij plus en min moeten beide typen gelijk zijn");
                return ExpressionType.UNDEFINED;
            }
            if (operation instanceof MultiplyOperation) {
                if (lhs == ExpressionType.SCALAR) return rhs;
                if (rhs == ExpressionType.SCALAR) return lhs;
                operation.setError("Vermenigvuldigen vereist minimaal 1 SCALAR-operand");
                return ExpressionType.UNDEFINED;
            }
        }
        expression.setError("Onbekend type expressie");
        return ExpressionType.UNDEFINED;
    }
}