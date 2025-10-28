package org.example.dmn.parser;

import org.example.dmn.model.Action;
import org.example.dmn.model.Condition;
import org.example.dmn.model.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced parser for natural language business rules.
 * 
 * Supports patterns like:
 * - "If customer is platinum and total amount > 3000, discount is 25%"
 * - "If total amount between 1000 and 3000, discount is 15%"
 * - "If customer membership in (silver, bronze), discount is 5%"
 * - "Otherwise, discount is 0%"
 */
public class NaturalLanguageRuleParser {
    
    // Pattern: If <conditions>, <actions>
    private static final Pattern IF_THEN_PATTERN = Pattern.compile(
        "If\\s+(.+?),\\s*(.+)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern: Otherwise, <actions>
    private static final Pattern OTHERWISE_PATTERN = Pattern.compile(
        "Otherwise,\\s*(.+)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern: variable is value
    private static final Pattern IS_PATTERN = Pattern.compile(
        "([a-zA-Z_][a-zA-Z0-9_\\s]*)\\s+is\\s+(.+)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern: variable between X and Y
    private static final Pattern BETWEEN_PATTERN = Pattern.compile(
        "([a-zA-Z_][a-zA-Z0-9_\\s]*)\\s+between\\s+([0-9.]+)\\s+and\\s+([0-9.]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern: variable in (list)
    private static final Pattern IN_LIST_PATTERN = Pattern.compile(
        "([a-zA-Z_][a-zA-Z0-9_\\s]*)\\s+in\\s+\\(([^)]+)\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern: variable contains any value
    private static final Pattern CONTAINS_ANY_PATTERN = Pattern.compile(
        "([a-zA-Z_][a-zA-Z0-9_\\s]*)\\s+contains\\s+any\\s+(.+)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern: variable operator value (standard comparison)
    private static final Pattern COMPARISON_PATTERN = Pattern.compile(
        "([a-zA-Z_][a-zA-Z0-9_\\s]*)\\s+(>=|<=|>|<|!=|=)\\s+(.+)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Parse a single natural language rule.
     */
    public Rule parse(String ruleText) {
        if (ruleText == null || ruleText.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule text cannot be empty");
        }

        String normalized = ruleText.trim();

        // Check for "Otherwise" pattern (default rule)
        Matcher otherwiseMatcher = OTHERWISE_PATTERN.matcher(normalized);
        if (otherwiseMatcher.matches()) {
            return parseOtherwiseRule(otherwiseMatcher.group(1), ruleText);
        }

        // Smart split on comma (avoiding commas inside parentheses)
        if (!normalized.toLowerCase().startsWith("if ")) {
            throw new IllegalArgumentException(
                "Rule must start with 'If'. Got: " + ruleText
            );
        }

        // Find the comma that separates conditions from actions
        // (skip commas inside parentheses)
        int commaPos = findSeparatorComma(normalized);
        if (commaPos == -1) {
            throw new IllegalArgumentException(
                "Rule must have a comma separating conditions from actions. Got: " + ruleText
            );
        }

        String conditionsPart = normalized.substring(3, commaPos).trim(); // Skip "If "
        String actionsPart = normalized.substring(commaPos + 1).trim();

        // Parse conditions
        List<Condition> conditions = parseConditions(conditionsPart);

        // Parse actions
        List<Action> actions = parseActions(actionsPart);

        String ruleId = "rule_" + UUID.randomUUID().toString().substring(0, 8);

        return new Rule(ruleId, conditions, actions, ruleText);
    }

    /**
     * Find the comma that separates conditions from actions.
     * Skips commas inside parentheses.
     */
    private int findSeparatorComma(String text) {
        int parenDepth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                parenDepth++;
            } else if (c == ')') {
                parenDepth--;
            } else if (c == ',' && parenDepth == 0) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Parse "Otherwise" rule (default/fallback with no conditions).
     */
    private Rule parseOtherwiseRule(String actionsPart, String rawText) {
        // Create a dummy condition that always matches
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("1", Condition.Operator.EQUALS, "1")); // Always true
        
        List<Action> actions = parseActions(actionsPart);
        
        String ruleId = "rule_otherwise_" + UUID.randomUUID().toString().substring(0, 8);
        
        return new Rule(ruleId, conditions, actions, rawText);
    }
    
    /**
     * Parse conditions from natural language.
     * Smart splitting that handles "between X and Y" and "in (list)" patterns.
     */
    private List<Condition> parseConditions(String conditionsText) {
        List<Condition> conditions = new ArrayList<>();

        // Smart split: don't split "and" inside "between X and Y" or "in (...)"
        List<String> parts = smartSplitByAnd(conditionsText);

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            Condition condition = parseCondition(trimmed);
            if (condition != null) {
                conditions.add(condition);
            }
        }

        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("At least one condition is required");
        }

        return conditions;
    }

    /**
     * Smart split by "and" that preserves "between X and Y" and "in (...)" patterns.
     */
    private List<String> smartSplitByAnd(String text) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        boolean inBetween = false;

        // Process character by character to handle parentheses correctly
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);

            // Track parentheses
            if (c == '(') {
                parenDepth++;
                current.append(c);
                i++;
                continue;
            } else if (c == ')') {
                parenDepth--;
                current.append(c);
                i++;
                continue;
            }

            // Check for "between" keyword
            if (i + 7 <= text.length() && text.substring(i, i + 7).equalsIgnoreCase("between")) {
                inBetween = true;
                current.append(text.substring(i, i + 7));
                i += 7;
                continue;
            }

            // Check for " and " separator (with spaces)
            if (i + 5 <= text.length() &&
                text.substring(i, i + 5).matches("(?i)\\s+and\\s+") &&
                parenDepth == 0 && !inBetween) {
                // This is a separator "and"
                if (current.length() > 0) {
                    parts.add(current.toString().trim());
                    current = new StringBuilder();
                }
                i += 5; // Skip " and "
                continue;
            }

            // Check if we just finished "between X and Y" pattern
            if (inBetween && i + 5 <= text.length() &&
                text.substring(i, i + 5).matches("(?i)\\s+and\\s+")) {
                // Add " and " to current (part of between pattern)
                current.append(text.substring(i, i + 5));
                i += 5;
                // Look ahead to see if next token is a number
                int j = i;
                while (j < text.length() && Character.isWhitespace(text.charAt(j))) j++;
                StringBuilder nextToken = new StringBuilder();
                while (j < text.length() && !Character.isWhitespace(text.charAt(j))) {
                    nextToken.append(text.charAt(j));
                    j++;
                }
                if (isNumeric(nextToken.toString())) {
                    inBetween = false; // Reset after "between X and Y"
                }
                continue;
            }

            // Regular character
            current.append(c);
            i++;
        }

        // Add the last part
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }

        return parts;
    }
    
    /**
     * Parse a single condition.
     */
    private Condition parseCondition(String conditionText) {
        // Try "between" pattern first
        Matcher betweenMatcher = BETWEEN_PATTERN.matcher(conditionText);
        if (betweenMatcher.matches()) {
            String variable = normalizeVariableName(betweenMatcher.group(1));
            String min = betweenMatcher.group(2);
            String max = betweenMatcher.group(3);
            // Convert to: variable >= min and variable <= max
            // For now, we'll use a FEEL range expression
            return new Condition(variable, Condition.Operator.IN, "[" + min + ".." + max + "]");
        }
        
        // Try "in (list)" pattern
        Matcher inListMatcher = IN_LIST_PATTERN.matcher(conditionText);
        if (inListMatcher.matches()) {
            String variable = normalizeVariableName(inListMatcher.group(1));
            String listItems = inListMatcher.group(2).trim();
            // Convert to FEEL list format
            String[] items = listItems.split(",");
            StringBuilder feelList = new StringBuilder("[");
            for (int i = 0; i < items.length; i++) {
                if (i > 0) feelList.append(", ");
                String item = items[i].trim();
                feelList.append("\"").append(item).append("\"");
            }
            feelList.append("]");
            return new Condition(variable, Condition.Operator.IN, feelList.toString());
        }
        
        // Try "contains any" pattern
        Matcher containsMatcher = CONTAINS_ANY_PATTERN.matcher(conditionText);
        if (containsMatcher.matches()) {
            String variable = normalizeVariableName(containsMatcher.group(1));
            String value = containsMatcher.group(2).trim();
            return new Condition(variable, Condition.Operator.CONTAINS, value);
        }
        
        // Try "is" pattern (equality)
        Matcher isMatcher = IS_PATTERN.matcher(conditionText);
        if (isMatcher.matches()) {
            String variable = normalizeVariableName(isMatcher.group(1));
            String value = isMatcher.group(2).trim();
            return new Condition(variable, Condition.Operator.EQUALS, value);
        }
        
        // Try standard comparison operators
        Matcher comparisonMatcher = COMPARISON_PATTERN.matcher(conditionText);
        if (comparisonMatcher.matches()) {
            String variable = normalizeVariableName(comparisonMatcher.group(1));
            String operatorSymbol = comparisonMatcher.group(2);
            String value = comparisonMatcher.group(3).trim();
            
            Condition.Operator operator = mapOperatorSymbol(operatorSymbol);
            return new Condition(variable, operator, value);
        }
        
        throw new IllegalArgumentException("Could not parse condition: " + conditionText);
    }
    
    /**
     * Parse actions from natural language.
     */
    private List<Action> parseActions(String actionsText) {
        List<Action> actions = new ArrayList<>();
        
        // Split by "and"
        String[] parts = actionsText.split("\\s+and\\s+", -1);
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            
            // Remove trailing period if present
            if (trimmed.endsWith(".")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
            }
            
            Action action = parseAction(trimmed);
            if (action != null) {
                actions.add(action);
            }
        }
        
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("At least one action is required");
        }
        
        return actions;
    }
    
    /**
     * Parse a single action.
     */
    private Action parseAction(String actionText) {
        // Pattern: variable is value
        Matcher isMatcher = IS_PATTERN.matcher(actionText);
        if (isMatcher.matches()) {
            String variable = normalizeVariableName(isMatcher.group(1));
            String value = isMatcher.group(2).trim();
            return new Action(variable, value);
        }
        
        throw new IllegalArgumentException("Could not parse action: " + actionText);
    }
    
    /**
     * Normalize variable names (remove spaces, convert to snake_case).
     */
    private String normalizeVariableName(String name) {
        return name.trim()
            .toLowerCase()
            .replaceAll("\\s+", "_")
            .replaceAll("[^a-z0-9_]", "");
    }

    /**
     * Check if a string is numeric.
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Map operator symbols to Condition.Operator enum.
     */
    private Condition.Operator mapOperatorSymbol(String symbol) {
        switch (symbol) {
            case ">": return Condition.Operator.GREATER_THAN;
            case "<": return Condition.Operator.LESS_THAN;
            case ">=": return Condition.Operator.GREATER_THAN_OR_EQUAL;
            case "<=": return Condition.Operator.LESS_THAN_OR_EQUAL;
            case "=": return Condition.Operator.EQUALS;
            case "!=": return Condition.Operator.NOT_EQUALS;
            default: throw new IllegalArgumentException("Unknown operator: " + symbol);
        }
    }
    
    /**
     * Parse multiple rules from text.
     */
    public List<Rule> parseMultiple(String rulesText) {
        List<Rule> rules = new ArrayList<>();
        
        String[] lines = rulesText.split("\\n");
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("//")) {
                try {
                    rules.add(parse(trimmed));
                } catch (Exception e) {
                    System.err.println("Warning: Failed to parse rule: " + trimmed);
                    System.err.println("  Error: " + e.getMessage());
                }
            }
        }
        
        return rules;
    }
}

