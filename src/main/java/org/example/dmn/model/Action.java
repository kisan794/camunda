package org.example.dmn.model;

/**
 * Represents an action (output assignment) in a business rule.
 */
public class Action {
    private final String outputVariable;
    private final String outputValue;
    
    public Action(String outputVariable, String outputValue) {
        this.outputVariable = outputVariable;
        this.outputValue = outputValue;
    }
    
    public String getOutputVariable() {
        return outputVariable;
    }
    
    public String getOutputValue() {
        return outputValue;
    }
    
    @Override
    public String toString() {
        return outputVariable + " = " + outputValue;
    }
}

