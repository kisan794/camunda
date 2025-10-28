package org.example.ai;

import org.example.dmn.builder.DmnBuilder;
import org.example.dmn.model.Rule;
import org.example.dmn.parser.NaturalLanguageRuleParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * AI-powered rule generator using LLMs (OpenAI, Claude, etc.)
 * Converts unstructured text, documents, or requirements into structured business rules.
 */
public class AiRuleGenerator {
    
    private final String apiKey;
    private final String apiEndpoint;
    private final String model;
    private final HttpClient httpClient;
    private final NaturalLanguageRuleParser parser;
    
    public AiRuleGenerator(Builder builder) {
        this.apiKey = builder.apiKey;
        this.apiEndpoint = builder.apiEndpoint;
        this.model = builder.model;
        this.httpClient = HttpClient.newHttpClient();
        this.parser = new NaturalLanguageRuleParser();
    }
    
    /**
     * Generate rules from unstructured text using AI.
     * 
     * Example input:
     * "We give platinum customers 25% discount when they spend over $3000.
     *  Gold customers get 15% off for orders between $1000 and $3000."
     */
    public List<Rule> generateRulesFromText(String unstructuredText) throws IOException, InterruptedException {
        String prompt = buildPrompt(unstructuredText);
        String aiResponse = callAiApi(prompt);
        String structuredRules = extractRulesFromResponse(aiResponse);
        
        return parser.parseMultiple(structuredRules);
    }
    
    /**
     * Generate rules from business requirements document.
     */
    public List<Rule> generateRulesFromDocument(String documentContent, String context) 
            throws IOException, InterruptedException {
        String prompt = buildDocumentPrompt(documentContent, context);
        String aiResponse = callAiApi(prompt);
        String structuredRules = extractRulesFromResponse(aiResponse);
        
        return parser.parseMultiple(structuredRules);
    }
    
    /**
     * Improve existing rules using AI (fix grammar, optimize logic, suggest alternatives).
     */
    public String improveRules(String existingRules) throws IOException, InterruptedException {
        String prompt = buildImprovementPrompt(existingRules);
        String aiResponse = callAiApi(prompt);
        
        return extractRulesFromResponse(aiResponse);
    }
    
    /**
     * Generate DMN directly from unstructured text.
     */
    public String generateDmnFromText(String unstructuredText, String decisionName) 
            throws IOException, InterruptedException {
        List<Rule> rules = generateRulesFromText(unstructuredText);
        
        DmnBuilder builder = new DmnBuilder();
        builder.withDecisionName(decisionName)
               .withDecisionId(decisionName.toLowerCase().replace(" ", "_"));
        
        return builder.build(rules);
    }
    
    /**
     * Build prompt for AI to convert unstructured text to structured rules.
     */
    private String buildPrompt(String unstructuredText) {
        return """
            You are a business rules expert. Convert the following unstructured business requirements 
            into structured IF-THEN rules using this exact format:
            
            Format:
            If <condition> and <condition>, <output> is <value> and <output> is <value>.
            
            Supported patterns:
            - Equality: "customer is platinum"
            - Comparison: "amount > 1000", "age >= 18"
            - Range: "amount between 100 and 500"
            - List: "status in (active, pending, approved)"
            - Contains: "order contains electronics"
            - Default: "Otherwise, status is default."
            
            Requirements:
            %s
            
            Generate ONLY the structured rules, one per line. Do not include explanations.
            """.formatted(unstructuredText);
    }
    
    /**
     * Build prompt for document-based rule generation.
     */
    private String buildDocumentPrompt(String documentContent, String context) {
        return """
            You are a business analyst extracting decision rules from a requirements document.
            
            Context: %s
            
            Document:
            %s
            
            Extract all business rules and convert them to this format:
            If <condition> and <condition>, <output> is <value>.
            
            Focus on:
            - Decision logic
            - Approval criteria
            - Pricing rules
            - Eligibility conditions
            - Risk assessments
            
            Generate ONLY the structured rules, one per line.
            """.formatted(context, documentContent);
    }
    
    /**
     * Build prompt for rule improvement.
     */
    private String buildImprovementPrompt(String existingRules) {
        return """
            You are a business rules optimization expert. Improve the following rules by:
            1. Fixing grammar and clarity
            2. Removing redundancy
            3. Optimizing logic
            4. Ensuring consistency
            5. Suggesting better variable names
            
            Existing rules:
            %s
            
            Return the improved rules in the same format, one per line.
            """.formatted(existingRules);
    }
    
    /**
     * Call AI API (OpenAI, Claude, etc.)
     */
    private String callAiApi(String prompt) throws IOException, InterruptedException {
        // OpenAI API example
        String requestBody = """
            {
                "model": "%s",
                "messages": [
                    {
                        "role": "system",
                        "content": "You are a business rules expert that converts requirements into structured decision rules."
                    },
                    {
                        "role": "user",
                        "content": %s
                    }
                ],
                "temperature": 0.3,
                "max_tokens": 2000
            }
            """.formatted(model, escapeJson(prompt));
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiEndpoint))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("AI API error: " + response.statusCode() + " - " + response.body());
        }
        
        return response.body();
    }
    
    /**
     * Extract rules from AI response (parse JSON).
     */
    private String extractRulesFromResponse(String jsonResponse) {
        // Simple JSON parsing - in production use Jackson or Gson
        int contentStart = jsonResponse.indexOf("\"content\":") + 11;
        int contentEnd = jsonResponse.indexOf("\"", contentStart + 1);
        
        if (contentStart > 10 && contentEnd > contentStart) {
            String content = jsonResponse.substring(contentStart, contentEnd);
            return content.replace("\\n", "\n").replace("\\\"", "\"");
        }
        
        return "";
    }
    
    /**
     * Escape JSON string.
     */
    private String escapeJson(String text) {
        return "\"" + text.replace("\\", "\\\\")
                          .replace("\"", "\\\"")
                          .replace("\n", "\\n")
                          .replace("\r", "\\r")
                          .replace("\t", "\\t") + "\"";
    }
    
    /**
     * Builder for AiRuleGenerator.
     */
    public static class Builder {
        private String apiKey;
        private String apiEndpoint = "https://api.openai.com/v1/chat/completions";
        private String model = "gpt-4";
        
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }
        
        public Builder apiEndpoint(String apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        /**
         * Use OpenAI GPT-4.
         */
        public Builder useOpenAI(String apiKey) {
            this.apiKey = apiKey;
            this.apiEndpoint = "https://api.openai.com/v1/chat/completions";
            this.model = "gpt-4";
            return this;
        }
        
        /**
         * Use Anthropic Claude.
         */
        public Builder useClaude(String apiKey) {
            this.apiKey = apiKey;
            this.apiEndpoint = "https://api.anthropic.com/v1/messages";
            this.model = "claude-3-sonnet-20240229";
            return this;
        }
        
        /**
         * Use Azure OpenAI.
         */
        public Builder useAzureOpenAI(String apiKey, String endpoint) {
            this.apiKey = apiKey;
            this.apiEndpoint = endpoint;
            this.model = "gpt-4";
            return this;
        }
        
        /**
         * Use local LLM (Ollama, LM Studio, etc.)
         */
        public Builder useLocalLLM(String endpoint, String model) {
            this.apiKey = "not-needed";
            this.apiEndpoint = endpoint;
            this.model = model;
            return this;
        }
        
        public AiRuleGenerator build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalStateException("API key is required");
            }
            return new AiRuleGenerator(this);
        }
    }
}

