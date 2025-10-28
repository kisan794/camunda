package org.example;

import org.example.dmn.builder.DmnBuilder;
import org.example.dmn.model.Rule;
import org.example.dmn.parser.NaturalLanguageRuleParser;
import org.example.dmn.validator.DmnValidator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Convert natural language business rules to DMN.
 */
public class ConvertNaturalLanguageRules {
    
    public static void main(String[] args) {
        String rulesText = """
            If customer is platinum and total amount > 3000, discount is 25% and priority shipping is true and riskLevel is "Low".
            If customer is gold and total amount between 1000 and 3000, discount is 15% and priority shipping is true and riskLevel is "Medium".
            If the order contains any electronics and total amount > 1500, discount is 10% and priority shipping is true and riskLevel is "High".
            If any item price > 2000, riskLevel is "Critical" and priority shipping is true.
            If customer membership in (silver, bronze) and total quantity >= 5, discount is 5%.
            Otherwise, discount is 0% and priority shipping is false and riskLevel is "Low".
            """;
        
        System.out.println("=== Natural Language to DMN Converter ===\n");
        System.out.println("Input Rules:");
        System.out.println(rulesText);
        System.out.println("\n--- Processing ---\n");
        
        try {
            // Parse rules
            NaturalLanguageRuleParser parser = new NaturalLanguageRuleParser();
            List<Rule> rules = parser.parseMultiple(rulesText);
            
            System.out.println("✓ Parsed " + rules.size() + " rules\n");
            
            // Display parsed rules
            System.out.println("--- Parsed Rules ---\n");
            for (int i = 0; i < rules.size(); i++) {
                Rule rule = rules.get(i);
                System.out.println("Rule " + (i + 1) + ": " + rule.getId());
                System.out.println("  Conditions:");
                rule.getConditions().forEach(c -> 
                    System.out.println("    - " + c.getVariable() + " " + 
                                     c.getOperator().getFeelForm() + " " + c.getValue())
                );
                System.out.println("  Actions:");
                rule.getActions().forEach(a -> 
                    System.out.println("    - " + a.getOutputVariable() + " = " + a.getOutputValue())
                );
                System.out.println();
            }
            
            // Generate DMN
            System.out.println("--- Generating DMN ---\n");
            DmnBuilder builder = new DmnBuilder();
            builder.withDecisionName("OrderProcessingDecision")
                   .withDecisionId("order_processing_decision");
            
            String dmnXml = builder.build(rules);
            
            System.out.println("✓ Generated DMN (" + dmnXml.length() + " characters)\n");
            
            // Validate
            System.out.println("--- Validating DMN ---\n");
            DmnValidator validator = new DmnValidator();
            DmnValidator.ValidationResult validationResult = validator.validate(dmnXml);
            
            if (validationResult.isValid()) {
                System.out.println("✓ DMN is valid!");
            } else {
                System.out.println("✗ DMN validation failed:");
                validationResult.getErrors().forEach(e -> System.out.println("  - " + e));
            }
            
            if (!validationResult.getWarnings().isEmpty()) {
                System.out.println("\nWarnings:");
                validationResult.getWarnings().forEach(w -> System.out.println("  - " + w));
            }
            
            // Save to file
            String outputFile = "order-processing-decision.dmn";
            Files.writeString(Paths.get(outputFile), dmnXml);
            
            System.out.println("\n✓ Saved to: " + outputFile);
            
            // Show sample of DMN
            System.out.println("\n--- DMN Preview (first 1000 chars) ---\n");
            System.out.println(dmnXml.substring(0, Math.min(1000, dmnXml.length())));
            System.out.println("...\n");
            
            // Show decision table structure
            System.out.println("--- Decision Table Structure ---\n");
            System.out.println("Inputs:");
            System.out.println("  - customer");
            System.out.println("  - total_amount");
            System.out.println("  - the_order");
            System.out.println("  - any_item_price");
            System.out.println("  - customer_membership");
            System.out.println("  - total_quantity");
            System.out.println("\nOutputs:");
            System.out.println("  - discount");
            System.out.println("  - priority_shipping");
            System.out.println("  - risklevel");
            System.out.println("\nRules: " + rules.size());
            System.out.println("Hit Policy: FIRST (first matching rule wins)");
            
            System.out.println("\n=== Conversion Complete ===");
            
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

