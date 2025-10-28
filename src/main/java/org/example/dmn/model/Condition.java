package org.example.dmn.model;

/**
 * Represents a condition in a business rule.
 */
public class Condition {
    private final String variable;
    private final Operator operator;
    private final String value;
    
    public Condition(String variable, Operator operator, String value) {
        this.variable = variable;
        this.operator = operator;
        this.value = value;
    }
    
    public String getVariable() {
        return variable;
    }
    
    public Operator getOperator() {
        return operator;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return variable + " " + operator.getFeelForm() + " " + value;
    }
    
    /**
     * Supported operators with their FEEL equivalents.
     */
    public enum Operator {
        EQUALS("=", "="),
        NOT_EQUALS("not equals", "!="),
        GREATER_THAN(">", ">"),
        LESS_THAN("<", "<"),
        GREATER_THAN_OR_EQUAL(">=", ">="),
        LESS_THAN_OR_EQUAL("<=", "<="),
        CONTAINS("contains", "contains"),
        IN("in", "in");
        
        private final String englishForm;
        private final String feelForm;
        
        Operator(String englishForm, String feelForm) {
            this.englishForm = englishForm;
            this.feelForm = feelForm;
        }
        
        public String getEnglishForm() {
            return englishForm;
        }
        
        public String getFeelForm() {
            return feelForm;
        }
        
        public static Operator fromEnglish(String english) {
            for (Operator op : values()) {
                if (op.englishForm.equalsIgnoreCase(english)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unknown operator: " + english);
        }
    }
}

