package org.example.ai;

import org.example.batch.BatchRuleEngine;
import org.example.dmn.builder.DmnBuilder;
import org.example.dmn.model.Rule;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * AI-enhanced batch processor that can handle:
 * 1. Structured rules (existing functionality)
 * 2. Unstructured text (AI converts to rules)
 * 3. Mixed content (auto-detect and process accordingly)
 */
public class AiEnhancedBatchProcessor {
    
    private final AiRuleGenerator aiGenerator;
    private final BatchRuleEngine batchEngine;
    private final boolean useAiForUnstructured;
    private final boolean improveExistingRules;
    
    private AiEnhancedBatchProcessor(Builder builder) {
        this.aiGenerator = builder.aiGenerator;
        this.batchEngine = builder.batchEngine;
        this.useAiForUnstructured = builder.useAiForUnstructured;
        this.improveExistingRules = builder.improveExistingRules;
    }
    
    /**
     * Process directory with AI enhancement.
     */
    public ProcessingResult processDirectory(Path inputDirectory) throws IOException {
        Instant start = Instant.now();
        
        System.out.println("🤖 AI-Enhanced Batch Processor Starting...");
        System.out.println("Input Directory: " + inputDirectory);
        System.out.println("AI Enhancement: " + (useAiForUnstructured ? "Enabled" : "Disabled"));
        System.out.println("Rule Improvement: " + (improveExistingRules ? "Enabled" : "Disabled"));
        System.out.println();
        
        // Find all text files
        List<Path> inputFiles;
        try (Stream<Path> paths = Files.walk(inputDirectory)) {
            inputFiles = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".txt"))
                .toList();
        }
        
        System.out.println("📁 Found " + inputFiles.size() + " input files");
        System.out.println();
        
        // Process files
        List<FileProcessingResult> results = new ArrayList<>();
        int structuredCount = 0;
        int unstructuredCount = 0;
        int improvedCount = 0;
        
        for (Path file : inputFiles) {
            try {
                FileProcessingResult result = processFile(file);
                results.add(result);
                
                switch (result.getProcessingType()) {
                    case STRUCTURED -> structuredCount++;
                    case AI_GENERATED -> unstructuredCount++;
                    case AI_IMPROVED -> improvedCount++;
                }
                
                System.out.println("✓ " + file.getFileName() + " - " + result.getProcessingType());
                
            } catch (Exception e) {
                System.err.println("✗ " + file.getFileName() + " - " + e.getMessage());
            }
        }
        
        Duration duration = Duration.between(start, Instant.now());
        
        return new ProcessingResult(
            inputFiles.size(),
            structuredCount,
            unstructuredCount,
            improvedCount,
            duration,
            results
        );
    }
    
    /**
     * Process a single file with AI enhancement.
     */
    private FileProcessingResult processFile(Path inputFile) throws IOException, InterruptedException {
        String content = Files.readString(inputFile);
        
        // Detect if content is structured or unstructured
        if (isStructuredRules(content)) {
            // Structured rules - optionally improve with AI
            if (improveExistingRules && aiGenerator != null) {
                String improved = aiGenerator.improveRules(content);
                return new FileProcessingResult(
                    inputFile.getFileName().toString(),
                    ProcessingType.AI_IMPROVED,
                    improved
                );
            } else {
                return new FileProcessingResult(
                    inputFile.getFileName().toString(),
                    ProcessingType.STRUCTURED,
                    content
                );
            }
        } else {
            // Unstructured text - use AI to generate rules
            if (useAiForUnstructured && aiGenerator != null) {
                List<Rule> rules = aiGenerator.generateRulesFromText(content);
                String structuredRules = rulesToText(rules);
                
                return new FileProcessingResult(
                    inputFile.getFileName().toString(),
                    ProcessingType.AI_GENERATED,
                    structuredRules
                );
            } else {
                throw new IOException("Unstructured content requires AI enhancement");
            }
        }
    }
    
    /**
     * Detect if content contains structured rules.
     */
    private boolean isStructuredRules(String content) {
        String lower = content.toLowerCase();
        return lower.contains("if ") && 
               (lower.contains(" is ") || lower.contains(" > ") || lower.contains(" < "));
    }
    
    /**
     * Convert rules to text format.
     */
    private String rulesToText(List<Rule> rules) {
        StringBuilder sb = new StringBuilder();
        for (Rule rule : rules) {
            sb.append(rule.getRawText()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Processing type enum.
     */
    public enum ProcessingType {
        STRUCTURED,      // Already structured, no AI needed
        AI_GENERATED,    // Unstructured text converted by AI
        AI_IMPROVED      // Structured rules improved by AI
    }
    
    /**
     * Result of processing a single file.
     */
    public static class FileProcessingResult {
        private final String fileName;
        private final ProcessingType processingType;
        private final String processedContent;
        
        public FileProcessingResult(String fileName, ProcessingType processingType, String processedContent) {
            this.fileName = fileName;
            this.processingType = processingType;
            this.processedContent = processedContent;
        }
        
        public String getFileName() { return fileName; }
        public ProcessingType getProcessingType() { return processingType; }
        public String getProcessedContent() { return processedContent; }
    }
    
    /**
     * Overall processing result.
     */
    public static class ProcessingResult {
        private final int totalFiles;
        private final int structuredFiles;
        private final int aiGeneratedFiles;
        private final int aiImprovedFiles;
        private final Duration totalTime;
        private final List<FileProcessingResult> fileResults;
        
        public ProcessingResult(int totalFiles, int structuredFiles, int aiGeneratedFiles,
                               int aiImprovedFiles, Duration totalTime, List<FileProcessingResult> fileResults) {
            this.totalFiles = totalFiles;
            this.structuredFiles = structuredFiles;
            this.aiGeneratedFiles = aiGeneratedFiles;
            this.aiImprovedFiles = aiImprovedFiles;
            this.totalTime = totalTime;
            this.fileResults = fileResults;
        }
        
        public void printSummary() {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🤖 AI-ENHANCED PROCESSING SUMMARY");
            System.out.println("=".repeat(60));
            System.out.println("Total Files:              " + totalFiles);
            System.out.println("Structured Rules:         " + structuredFiles);
            System.out.println("AI Generated:             " + aiGeneratedFiles + " 🤖");
            System.out.println("AI Improved:              " + aiImprovedFiles + " ✨");
            System.out.println("Total Processing Time:    " + formatDuration(totalTime));
            System.out.println("=".repeat(60));
        }
        
        private String formatDuration(Duration duration) {
            long seconds = duration.getSeconds();
            long millis = duration.toMillis() % 1000;
            if (seconds > 0) {
                return seconds + "." + String.format("%03d", millis) + "s";
            } else {
                return millis + "ms";
            }
        }
    }
    
    /**
     * Builder for AiEnhancedBatchProcessor.
     */
    public static class Builder {
        private AiRuleGenerator aiGenerator;
        private BatchRuleEngine batchEngine;
        private boolean useAiForUnstructured = true;
        private boolean improveExistingRules = false;
        
        public Builder aiGenerator(AiRuleGenerator aiGenerator) {
            this.aiGenerator = aiGenerator;
            return this;
        }
        
        public Builder batchEngine(BatchRuleEngine batchEngine) {
            this.batchEngine = batchEngine;
            return this;
        }
        
        public Builder useAiForUnstructured(boolean useAi) {
            this.useAiForUnstructured = useAi;
            return this;
        }
        
        public Builder improveExistingRules(boolean improve) {
            this.improveExistingRules = improve;
            return this;
        }
        
        public AiEnhancedBatchProcessor build() {
            return new AiEnhancedBatchProcessor(this);
        }
    }
}

