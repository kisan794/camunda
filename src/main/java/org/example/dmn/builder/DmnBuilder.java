package org.example.dmn.builder;

import org.example.dmn.model.Action;
import org.example.dmn.model.Condition;
import org.example.dmn.model.Rule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds DMN 1.3 XML from parsed rules.
 */
public class DmnBuilder {
    private String decisionName = "Decision";
    private String decisionId = "decision_1";
    private String namespace = "http://camunda.org/schema/1.0/dmn";
    
    public DmnBuilder withDecisionName(String name) {
        this.decisionName = name;
        return this;
    }
    
    public DmnBuilder withDecisionId(String id) {
        this.decisionId = id;
        return this;
    }
    
    public String build(List<Rule> rules) {
        // Collect all unique input and output variables
        Set<String> inputVars = new HashSet<>();
        Set<String> outputVars = new HashSet<>();
        
        for (Rule rule : rules) {
            for (Condition cond : rule.getConditions()) {
                inputVars.add(cond.getVariable());
            }
            for (Action action : rule.getActions()) {
                outputVars.add(action.getOutputVariable());
            }
        }
        
        StringBuilder xml = new StringBuilder();
        
        // XML Header
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<definitions xmlns=\"https://www.omg.org/spec/DMN/20191111/MODEL/\"\n");
        xml.append("             xmlns:dmndi=\"https://www.omg.org/spec/DMN/20191111/DMNDI/\"\n");
        xml.append("             xmlns:dc=\"http://www.omg.org/spec/DMN/20180521/DC/\"\n");
        xml.append("             xmlns:camunda=\"http://camunda.org/schema/1.0/dmn\"\n");
        xml.append("             id=\"definitions_").append(decisionId).append("\"\n");
        xml.append("             name=\"").append(escapeXml(decisionName)).append("\"\n");
        xml.append("             namespace=\"").append(namespace).append("\">\n\n");
        
        // Decision element
        xml.append("  <decision id=\"").append(decisionId).append("\" name=\"").append(escapeXml(decisionName)).append("\">\n");
        xml.append("    <decisionTable id=\"decisionTable_").append(decisionId).append("\" hitPolicy=\"FIRST\">\n");
        
        // Input clauses
        for (String inputVar : inputVars) {
            xml.append("      <input id=\"input_").append(inputVar).append("\" label=\"").append(inputVar).append("\">\n");
            xml.append("        <inputExpression id=\"inputExpression_").append(inputVar).append("\" typeRef=\"string\">\n");
            xml.append("          <text>").append(inputVar).append("</text>\n");
            xml.append("        </inputExpression>\n");
            xml.append("      </input>\n");
        }
        
        // Output clauses
        for (String outputVar : outputVars) {
            xml.append("      <output id=\"output_").append(outputVar).append("\" label=\"").append(outputVar).append("\" name=\"").append(outputVar).append("\" typeRef=\"string\" />\n");
        }
        
        // Rules
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            xml.append("      <rule id=\"").append(rule.getId()).append("\">\n");
            
            // Input entries (conditions)
            for (String inputVar : inputVars) {
                String expression = getInputExpression(rule, inputVar);
                xml.append("        <inputEntry id=\"inputEntry_").append(rule.getId()).append("_").append(inputVar).append("\">\n");
                xml.append("          <text>").append(escapeXml(expression)).append("</text>\n");
                xml.append("        </inputEntry>\n");
            }
            
            // Output entries (actions)
            for (String outputVar : outputVars) {
                String value = getOutputValue(rule, outputVar);
                xml.append("        <outputEntry id=\"outputEntry_").append(rule.getId()).append("_").append(outputVar).append("\">\n");
                xml.append("          <text>").append(escapeXml(value)).append("</text>\n");
                xml.append("        </outputEntry>\n");
            }
            
            xml.append("      </rule>\n");
        }
        
        xml.append("    </decisionTable>\n");
        xml.append("  </decision>\n");
        xml.append("</definitions>\n");
        
        return xml.toString();
    }
    
    private String getInputExpression(Rule rule, String inputVar) {
        for (Condition cond : rule.getConditions()) {
            if (cond.getVariable().equals(inputVar)) {
                return buildFeelExpression(cond);
            }
        }
        return "-"; // No condition for this input (matches any)
    }
    
    private String buildFeelExpression(Condition cond) {
        String operator = cond.getOperator().getFeelForm();
        String value = cond.getValue();
        
        // Handle special cases
        if (cond.getOperator() == Condition.Operator.IN) {
            // Value already contains the list/range
            return value;
        } else if (cond.getOperator() == Condition.Operator.CONTAINS) {
            return "contains(., " + normalizeValue(value) + ")";
        } else if (cond.getOperator() == Condition.Operator.EQUALS) {
            return normalizeValue(value);
        } else {
            return operator + " " + normalizeValue(value);
        }
    }
    
    private String normalizeValue(String value) {
        // If it's already quoted or a number, return as-is
        if (value.startsWith("\"") || value.startsWith("[") || isNumeric(value) || 
            value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return value;
        }
        // Otherwise, quote it
        return "\"" + value + "\"";
    }
    
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private String getOutputValue(Rule rule, String outputVar) {
        for (Action action : rule.getActions()) {
            if (action.getOutputVariable().equals(outputVar)) {
                return normalizeValue(action.getOutputValue());
            }
        }
        return "null"; // No output for this variable
    }
    
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}

