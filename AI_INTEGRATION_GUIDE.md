# 🤖 AI/ML Integration Guide

## Overview

Your DMN rule engine now supports **AI-powered rule generation** using Large Language Models (LLMs). This allows you to:

✅ **Convert unstructured text** to structured business rules  
✅ **Extract rules from documents** (PDFs, Word docs, emails)  
✅ **Improve existing rules** (fix grammar, optimize logic)  
✅ **Generate DMN directly** from natural language  
✅ **Handle mixed content** (structured + unstructured)  

---

## 🚀 Quick Start

### 1. Set Up API Key

```bash
# OpenAI
export OPENAI_API_KEY="sk-..."

# Or Anthropic Claude
export ANTHROPIC_API_KEY="sk-ant-..."

# Or Azure OpenAI
export AZURE_OPENAI_KEY="..."
export AZURE_OPENAI_ENDPOINT="https://your-resource.openai.azure.com"
```

### 2. Run Demo

```bash
./gradlew compileJava -q
java -cp build/classes/java/main org.example.AiRuleGeneratorDemo
```

### 3. Use in Your Code

```java
import org.example.ai.AiRuleGenerator;
import org.example.dmn.model.Rule;
import java.util.List;

// Create AI generator
AiRuleGenerator generator = new AiRuleGenerator.Builder()
    .useOpenAI(System.getenv("OPENAI_API_KEY"))
    .build();

// Convert unstructured text to rules
String text = "Platinum customers get 25% off when spending over $3000";
List<Rule> rules = generator.generateRulesFromText(text);

// Generate DMN directly
String dmn = generator.generateDmnFromText(text, "Customer Discount");
```

---

## 📋 Use Cases

### Use Case 1: Unstructured Text → Structured Rules

**Input (natural language):**
```
Our company policy is to give platinum customers a 25% discount 
when they spend more than $3000. Gold customers get 15% off for 
orders between $1000 and $3000.
```

**AI Output (structured rules):**
```
If customer is platinum and total_amount > 3000, discount is 25%.
If customer is gold and total_amount between 1000 and 3000, discount is 15%.
```

**Code:**
```java
AiRuleGenerator generator = new AiRuleGenerator.Builder()
    .useOpenAI(apiKey)
    .build();

List<Rule> rules = generator.generateRulesFromText(unstructuredText);
```

---

### Use Case 2: Extract Rules from Documents

**Input (business document):**
```
LOAN APPROVAL POLICY

Section 3.2: Credit Score Requirements
Applicants with a credit score above 750 are automatically approved 
for loans up to $500,000. Those with scores between 650 and 750 
require manual review and are limited to $250,000.
```

**AI Output:**
```
If credit_score > 750, approval_status is approved and max_loan is 500000.
If credit_score between 650 and 750, approval_status is manual_review and max_loan is 250000.
If credit_score < 650, approval_status is rejected.
```

**Code:**
```java
String document = Files.readString(Paths.get("policy.txt"));
List<Rule> rules = generator.generateRulesFromDocument(
    document, 
    "Loan Approval Policy"
);
```

---

### Use Case 3: Improve Existing Rules

**Input (poor quality):**
```
If cust is plat and amt > 3000, disc is 25.
If cust is gold and amt > 1000 and amt < 3000, disc is 15.
```

**AI Output (improved):**
```
If customer is platinum and total_amount > 3000, discount is 25%.
If customer is gold and total_amount between 1000 and 3000, discount is 15%.
```

**Code:**
```java
String improved = generator.improveRules(existingRules);
```

---

### Use Case 4: Batch Processing with AI

Process 100+ files containing **mixed structured and unstructured content**:

```java
import org.example.ai.AiEnhancedBatchProcessor;
import org.example.batch.BatchRuleEngine;

// Build AI generator
AiRuleGenerator aiGen = new AiRuleGenerator.Builder()
    .useOpenAI(apiKey)
    .build();

// Build batch engine
BatchRuleEngine batchEngine = new BatchRuleEngine.Builder()
    .outputDirectory(Paths.get("dmn-output"))
    .maxThreads(8)
    .build();

// Build AI-enhanced processor
AiEnhancedBatchProcessor processor = new AiEnhancedBatchProcessor.Builder()
    .aiGenerator(aiGen)
    .batchEngine(batchEngine)
    .useAiForUnstructured(true)
    .improveExistingRules(true)
    .build();

// Process directory
var result = processor.processDirectory(Paths.get("rules-input"));
result.printSummary();
```

**Output:**
```
🤖 AI-ENHANCED PROCESSING SUMMARY
============================================================
Total Files:              100
Structured Rules:         60
AI Generated:             30 🤖
AI Improved:              10 ✨
Total Processing Time:    45.2s
============================================================
```

---

## 🔧 Configuration

### Supported AI Providers

#### 1. OpenAI (GPT-4, GPT-3.5)
```java
AiRuleGenerator generator = new AiRuleGenerator.Builder()
    .useOpenAI("sk-...")
    .build();
```

#### 2. Anthropic Claude
```java
AiRuleGenerator generator = new AiRuleGenerator.Builder()
    .useClaude("sk-ant-...")
    .build();
```

#### 3. Azure OpenAI
```java
AiRuleGenerator generator = new AiRuleGenerator.Builder()
    .useAzureOpenAI(
        "your-api-key",
        "https://your-resource.openai.azure.com/openai/deployments/gpt-4/chat/completions?api-version=2023-05-15"
    )
    .build();
```

#### 4. Local LLM (Ollama, LM Studio)
```java
AiRuleGenerator generator = new AiRuleGenerator.Builder()
    .useLocalLLM(
        "http://localhost:11434/api/chat",
        "llama2"
    )
    .build();
```

---

## 📊 Performance & Cost

### OpenAI GPT-4

| Operation | Tokens | Cost | Time |
|-----------|--------|------|------|
| Simple rule generation | ~500 | $0.015 | 2-3s |
| Document extraction | ~2000 | $0.06 | 5-8s |
| Rule improvement | ~800 | $0.024 | 3-4s |

**Batch Processing (100 files):**
- Total cost: ~$3-5
- Total time: ~5-10 minutes
- Throughput: 10-20 files/minute

### Local LLM (Free)

| Model | Speed | Quality |
|-------|-------|---------|
| Llama 2 7B | Fast (1-2s) | Good |
| Llama 2 13B | Medium (3-5s) | Better |
| Mistral 7B | Fast (1-2s) | Good |
| CodeLlama | Fast (1-2s) | Best for rules |

**Recommendation:** Use local LLM for development/testing, cloud LLM for production.

---

## 🎯 Advanced Features

### 1. Custom Prompts

```java
public class CustomAiGenerator extends AiRuleGenerator {
    @Override
    protected String buildPrompt(String text) {
        return """
            You are an expert in insurance underwriting rules.
            Convert the following policy text to decision rules:
            
            %s
            
            Use insurance-specific terminology and include risk factors.
            """.formatted(text);
    }
}
```

### 2. Multi-Model Ensemble

```java
// Use multiple models and vote on best result
AiRuleGenerator gpt4 = new AiRuleGenerator.Builder().useOpenAI(key1).build();
AiRuleGenerator claude = new AiRuleGenerator.Builder().useClaude(key2).build();

List<Rule> rules1 = gpt4.generateRulesFromText(text);
List<Rule> rules2 = claude.generateRulesFromText(text);

// Compare and merge results
List<Rule> bestRules = selectBestRules(rules1, rules2);
```

### 3. Streaming for Large Documents

```java
// Process large documents in chunks
String largeDoc = Files.readString(Paths.get("large-policy.txt"));
List<String> chunks = splitIntoChunks(largeDoc, 2000);

List<Rule> allRules = new ArrayList<>();
for (String chunk : chunks) {
    List<Rule> chunkRules = generator.generateRulesFromText(chunk);
    allRules.addAll(chunkRules);
}
```

### 4. Validation & Quality Control

```java
// Generate rules with AI
List<Rule> aiRules = generator.generateRulesFromText(text);

// Validate with traditional parser
NaturalLanguageRuleParser parser = new NaturalLanguageRuleParser();
for (Rule rule : aiRules) {
    try {
        parser.parse(rule.getRawText());
    } catch (Exception e) {
        System.err.println("Invalid rule: " + rule.getRawText());
    }
}
```

---

## 🧪 Testing

### Unit Tests

```java
@Test
public void testAiRuleGeneration() throws Exception {
    AiRuleGenerator generator = new AiRuleGenerator.Builder()
        .useOpenAI(System.getenv("OPENAI_API_KEY"))
        .build();
    
    String text = "Platinum customers get 25% off over $3000";
    List<Rule> rules = generator.generateRulesFromText(text);
    
    assertFalse(rules.isEmpty());
    assertTrue(rules.get(0).getRawText().contains("platinum"));
    assertTrue(rules.get(0).getRawText().contains("25"));
}
```

### Integration Tests

```bash
# Test with sample data
echo "Platinum customers get 25% off over \$3000" > test-input.txt

java -cp build/classes/java/main org.example.AiRuleGeneratorDemo

# Verify output
cat output.dmn | grep "platinum"
```

---

## 🔒 Security & Privacy

### Best Practices

1. **API Key Management**
   ```bash
   # Use environment variables
   export OPENAI_API_KEY="sk-..."
   
   # Or use secrets manager
   aws secretsmanager get-secret-value --secret-id openai-key
   ```

2. **Data Privacy**
   - Don't send sensitive data to cloud APIs
   - Use local LLMs for confidential rules
   - Implement data masking for PII

3. **Rate Limiting**
   ```java
   // Add rate limiting
   RateLimiter limiter = RateLimiter.create(10.0); // 10 requests/sec
   
   for (String text : texts) {
       limiter.acquire();
       generator.generateRulesFromText(text);
   }
   ```

4. **Error Handling**
   ```java
   try {
       List<Rule> rules = generator.generateRulesFromText(text);
   } catch (IOException e) {
       // Fallback to manual processing
       logger.warn("AI generation failed, using fallback");
       rules = manualParser.parse(text);
   }
   ```

---

## 📈 Monitoring & Logging

### Track AI Usage

```java
public class MonitoredAiGenerator extends AiRuleGenerator {
    private final Metrics metrics;
    
    @Override
    public List<Rule> generateRulesFromText(String text) {
        long start = System.currentTimeMillis();
        
        try {
            List<Rule> rules = super.generateRulesFromText(text);
            
            metrics.recordSuccess(System.currentTimeMillis() - start);
            metrics.recordTokens(estimateTokens(text));
            
            return rules;
        } catch (Exception e) {
            metrics.recordFailure(e);
            throw e;
        }
    }
}
```

---

## 💡 Tips & Tricks

### 1. Optimize Prompts
- Be specific about output format
- Provide examples in the prompt
- Use temperature=0.3 for consistency

### 2. Cache Results
```java
Map<String, List<Rule>> cache = new ConcurrentHashMap<>();

List<Rule> getCachedRules(String text) {
    return cache.computeIfAbsent(text, 
        t -> generator.generateRulesFromText(t));
}
```

### 3. Batch API Calls
```java
// Process multiple texts in one API call
String combinedPrompt = texts.stream()
    .map(t -> "Text " + i++ + ": " + t)
    .collect(Collectors.joining("\n\n"));

String response = generator.callAiApi(combinedPrompt);
```

---

## 🎓 Next Steps

1. **Try the demo**: `java org.example.AiRuleGeneratorDemo`
2. **Test with your data**: Use real business documents
3. **Integrate with batch processor**: Process 100+ files
4. **Deploy to production**: Use Azure/AWS for scalability
5. **Monitor and optimize**: Track costs and performance

---

## 📚 Resources

- **OpenAI API**: https://platform.openai.com/docs
- **Anthropic Claude**: https://docs.anthropic.com
- **Ollama (Local LLM)**: https://ollama.ai
- **LM Studio**: https://lmstudio.ai

---

## 🏆 Summary

You now have **AI-powered rule generation** that can:

✅ Convert **unstructured text** to structured rules  
✅ Extract rules from **business documents**  
✅ **Improve existing rules** automatically  
✅ Process **100+ files** with mixed content  
✅ Support **multiple AI providers** (OpenAI, Claude, local)  
✅ Generate **valid DMN** directly from natural language  

**Ready to use AI in your rule engine!** 🚀

