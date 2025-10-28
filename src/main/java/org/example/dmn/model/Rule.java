package org.example.dmn.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete business rule with conditions and actions.
 */
public class Rule {
    private final String id;
    private final List<Condition> conditions;
    private final List<Action> actions;
    private final String rawText;
    
    public Rule(String id, List<Condition> conditions, List<Action> actions, String rawText) {
        this.id = id;
        this.conditions = new ArrayList<>(conditions);
        this.actions = new ArrayList<>(actions);
        this.rawText = rawText;
    }
    
    public String getId() {
        return id;
    }
    
    public List<Condition> getConditions() {
        return new ArrayList<>(conditions);
    }
    
    public List<Action> getActions() {
        return new ArrayList<>(actions);
    }
    
    public String getRawText() {
        return rawText;
    }
    
    @Override
    public String toString() {
        return "Rule{id='" + id + "', conditions=" + conditions.size() + 
               ", actions=" + actions.size() + "}";
    }
}

