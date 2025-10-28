package org.example.dmn.validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates DMN XML structure.
 */
public class DmnValidator {
    
    public ValidationResult validate(String dmnXml) {
        ValidationResult result = new ValidationResult();
        
        if (dmnXml == null || dmnXml.trim().isEmpty()) {
            result.addError("DMN XML is empty");
            return result;
        }
        
        // Check for XML declaration
        if (!dmnXml.trim().startsWith("<?xml")) {
            result.addWarning("Missing XML declaration");
        }
        
        // Check for required DMN elements
        if (!dmnXml.contains("<definitions")) {
            result.addError("Missing <definitions> root element");
        }
        
        if (!dmnXml.contains("<decision")) {
            result.addError("Missing <decision> element");
        }
        
        if (!dmnXml.contains("<decisionTable")) {
            result.addError("Missing <decisionTable> element");
        }
        
        // Check for balanced tags
        int openDefs = countOccurrences(dmnXml, "<definitions");
        int closeDefs = countOccurrences(dmnXml, "</definitions>");
        if (openDefs != closeDefs) {
            result.addError("Unbalanced <definitions> tags");
        }

        int openDecision = countOccurrences(dmnXml, "<decision ");
        int closeDecision = countOccurrences(dmnXml, "</decision>");
        if (openDecision != closeDecision) {
            result.addError("Unbalanced <decision> tags");
        }
        
        // Check for DMN namespace
        if (!dmnXml.contains("https://www.omg.org/spec/DMN")) {
            result.addWarning("Missing OMG DMN namespace");
        }
        
        // Check for hit policy
        if (!dmnXml.contains("hitPolicy=")) {
            result.addWarning("Missing hit policy attribute");
        }
        
        return result;
    }
    
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
    
    /**
     * Validation result containing errors and warnings.
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }
    }
}

