package org.example;

import org.example.ai.AiRuleGenerator;
import org.example.dmn.model.Rule;

import java.util.List;

/**
 * Demo of AI-powered rule generation.
 * 
 * IMPORTANT: Set your API key as environment variable:
 *   export OPENAI_API_KEY="sk-..."
 *   export ANTHROPIC_API_KEY="sk-ant-..."
 */
public class AiRuleGeneratorDemo {
    
    public static void main(String[] args) {
        // Check for API key
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("⚠️  No API key found. Set OPENAI_API_KEY environment variable.");
            System.out.println();
            System.out.println("Example usage:");
            System.out.println("  export OPENAI_API_KEY=\"sk-...\"");
            System.out.println("  java org.example.AiRuleGeneratorDemo");
            System.out.println();
            System.out.println("Running in DEMO MODE (showing examples without API calls)...");
            System.out.println();
            runDemoMode();
            return;
        }
        
        runWithAi(apiKey);
    }
    
    /**
     * Run with actual AI API.
     */
    private static void runWithAi(String apiKey) {
        System.out.println("🤖 AI Rule Generator Demo");
        System.out.println("=".repeat(60));
        System.out.println();
        
        // Build AI generator
        AiRuleGenerator generator = new AiRuleGenerator.Builder()
            .useOpenAI(apiKey)
            .build();
        
        // Example 1: Convert unstructured text to rules
        System.out.println("📝 Example 1: Unstructured Text → Structured Rules");
        System.out.println("-".repeat(60));
        
        String unstructuredText = """
            Our company policy is to give platinum customers a 25% discount 
            when they spend more than $3000. Gold customers get 15% off for 
            orders between $1000 and $3000. Silver and bronze members get 5% 
            discount if they buy 5 or more items. Everyone else pays full price.
            """;
        
        System.out.println("Input (unstructured):");
        System.out.println(unstructuredText);
        System.out.println();
        
        try {
            List<Rule> rules = generator.generateRulesFromText(unstructuredText);
            
            System.out.println("Output (structured rules):");
            for (Rule rule : rules) {
                System.out.println("  " + rule.getRawText());
            }
            System.out.println();
            
            // Generate DMN
            String dmn = generator.generateDmnFromText(unstructuredText, "Customer Discount");
            System.out.println("✅ Generated DMN (" + dmn.length() + " characters)");
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        // Example 2: Improve existing rules
        System.out.println("✨ Example 2: Improve Existing Rules");
        System.out.println("-".repeat(60));
        
        String existingRules = """
            If cust is plat and amt > 3000, disc is 25.
            If cust is gold and amt > 1000 and amt < 3000, disc is 15.
            If cust is silver or bronze and qty >= 5, disc is 5.
            """;
        
        System.out.println("Input (poor quality):");
        System.out.println(existingRules);
        System.out.println();
        
        try {
            String improved = generator.improveRules(existingRules);
            
            System.out.println("Output (improved):");
            System.out.println(improved);
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        // Example 3: Extract rules from document
        System.out.println("📄 Example 3: Extract Rules from Document");
        System.out.println("-".repeat(60));
        
        String document = """
            LOAN APPROVAL POLICY
            
            Section 3.2: Credit Score Requirements
            Applicants with a credit score above 750 are automatically approved 
            for loans up to $500,000. Those with scores between 650 and 750 
            require manual review and are limited to $250,000. Scores below 650 
            are automatically rejected unless they have a co-signer.
            
            Section 3.3: Income Requirements
            Annual income must be at least 3x the loan amount. Self-employed 
            applicants need 2 years of tax returns.
            """;
        
        System.out.println("Input (document):");
        System.out.println(document.substring(0, 200) + "...");
        System.out.println();
        
        try {
            List<Rule> rules = generator.generateRulesFromDocument(document, "Loan Approval");
            
            System.out.println("Output (extracted rules):");
            for (Rule rule : rules) {
                System.out.println("  " + rule.getRawText());
            }
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Demo mode without API calls.
     */
    private static void runDemoMode() {
        System.out.println("🤖 AI Rule Generator - Demo Mode");
        System.out.println("=".repeat(60));
        System.out.println();
        
        System.out.println("📝 Example 1: Unstructured Text → Structured Rules");
        System.out.println("-".repeat(60));
        System.out.println("Input:");
        System.out.println("  'Platinum customers get 25% off when spending over $3000'");
        System.out.println();
        System.out.println("AI would generate:");
        System.out.println("  If customer is platinum and total_amount > 3000, discount is 25%.");
        System.out.println();
        
        System.out.println("✨ Example 2: Improve Existing Rules");
        System.out.println("-".repeat(60));
        System.out.println("Input:");
        System.out.println("  'If cust is plat and amt > 3000, disc is 25.'");
        System.out.println();
        System.out.println("AI would improve to:");
        System.out.println("  If customer is platinum and total_amount > 3000, discount is 25%.");
        System.out.println();
        
        System.out.println("📄 Example 3: Extract from Document");
        System.out.println("-".repeat(60));
        System.out.println("Input:");
        System.out.println("  'Credit scores above 750 are auto-approved for $500k loans'");
        System.out.println();
        System.out.println("AI would extract:");
        System.out.println("  If credit_score > 750, approval_status is approved and max_loan is 500000.");
        System.out.println();
        
        System.out.println("=".repeat(60));
        System.out.println("💡 To use real AI, set your API key:");
        System.out.println("   export OPENAI_API_KEY=\"sk-...\"");
        System.out.println("   export ANTHROPIC_API_KEY=\"sk-ant-...\"");
        System.out.println();
        System.out.println("Supported AI providers:");
        System.out.println("  ✓ OpenAI (GPT-4, GPT-3.5)");
        System.out.println("  ✓ Anthropic (Claude)");
        System.out.println("  ✓ Azure OpenAI");
        System.out.println("  ✓ Local LLMs (Ollama, LM Studio)");
        System.out.println();
    }
}

