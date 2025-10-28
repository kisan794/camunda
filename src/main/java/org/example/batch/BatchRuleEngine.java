package org.example.batch;

import org.example.dmn.builder.DmnBuilder;
import org.example.dmn.model.Rule;
import org.example.dmn.parser.NaturalLanguageRuleParser;
import org.example.dmn.validator.DmnValidator;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Batch processing engine for converting multiple rule files to DMN.
 * Supports parallel processing, partitioning, and error handling.
 */
public class BatchRuleEngine {
    
    private final NaturalLanguageRuleParser parser;
    private final int maxThreads;
    private final int maxRulesPerDmn;
    private final boolean validateOutput;
    private final Path outputDirectory;
    private final ExecutorService executorService;
    
    private BatchRuleEngine(Builder builder) {
        this.parser = new NaturalLanguageRuleParser();
        this.maxThreads = builder.maxThreads;
        this.maxRulesPerDmn = builder.maxRulesPerDmn;
        this.validateOutput = builder.validateOutput;
        this.outputDirectory = builder.outputDirectory;
        this.executorService = Executors.newFixedThreadPool(maxThreads);
    }
    
    /**
     * Process all .txt files in a directory.
     */
    public BatchResult processDirectory(Path inputDirectory) throws IOException {
        Instant start = Instant.now();
        
        System.out.println("🚀 Batch Rule Engine Starting...");
        System.out.println("Input Directory: " + inputDirectory);
        System.out.println("Output Directory: " + outputDirectory);
        System.out.println("Max Threads: " + maxThreads);
        System.out.println("Max Rules per DMN: " + maxRulesPerDmn);
        System.out.println();
        
        // Create output directory if it doesn't exist
        Files.createDirectories(outputDirectory);
        
        // Find all .txt files
        List<Path> inputFiles;
        try (Stream<Path> paths = Files.walk(inputDirectory)) {
            inputFiles = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".txt"))
                .collect(Collectors.toList());
        }
        
        System.out.println("📁 Found " + inputFiles.size() + " input files");
        System.out.println();
        
        if (inputFiles.isEmpty()) {
            return new BatchResult(0, 0, 0, 0, Duration.ZERO, Collections.emptyList());
        }
        
        // Process files in parallel
        List<Future<FileResult>> futures = new ArrayList<>();
        for (Path inputFile : inputFiles) {
            futures.add(executorService.submit(() -> processFile(inputFile)));
        }
        
        // Collect results
        List<FileResult> results = new ArrayList<>();
        int totalFiles = 0;
        int successfulFiles = 0;
        int totalRules = 0;
        int totalDmnFiles = 0;
        
        for (int i = 0; i < futures.size(); i++) {
            try {
                FileResult result = futures.get(i).get();
                results.add(result);
                totalFiles++;
                
                if (result.isSuccess()) {
                    successfulFiles++;
                    totalRules += result.getRulesProcessed();
                    totalDmnFiles += result.getDmnFilesGenerated();
                }
                
                // Progress indicator
                if ((i + 1) % 10 == 0 || (i + 1) == futures.size()) {
                    System.out.println("Progress: " + (i + 1) + "/" + futures.size() + " files processed");
                }
            } catch (Exception e) {
                System.err.println("Error processing file: " + e.getMessage());
            }
        }
        
        Duration duration = Duration.between(start, Instant.now());
        
        return new BatchResult(totalFiles, successfulFiles, totalRules, totalDmnFiles, duration, results);
    }
    
    /**
     * Process a single file.
     */
    private FileResult processFile(Path inputFile) {
        String fileName = inputFile.getFileName().toString();
        Instant start = Instant.now();
        
        try {
            // Read rules from file
            String rulesText = Files.readString(inputFile);
            
            // Parse rules
            List<Rule> rules = parser.parseMultiple(rulesText);
            
            if (rules.isEmpty()) {
                return FileResult.failure(fileName, "No valid rules found", Duration.ZERO);
            }
            
            // Partition rules if needed
            List<List<Rule>> partitions = partitionRules(rules);
            
            // Generate DMN files for each partition
            List<Path> generatedFiles = new ArrayList<>();
            for (int i = 0; i < partitions.size(); i++) {
                List<Rule> partition = partitions.get(i);
                
                // Generate output filename
                String baseName = fileName.replace(".txt", "");
                String outputFileName = partitions.size() > 1 
                    ? baseName + "_part" + (i + 1) + ".dmn"
                    : baseName + ".dmn";
                
                Path outputFile = outputDirectory.resolve(outputFileName);
                
                // Build DMN
                DmnBuilder builder = new DmnBuilder();
                builder.withDecisionName(baseName.replace("-", " ").replace("_", " "))
                       .withDecisionId(baseName.toLowerCase().replace(" ", "_"));
                
                String dmnXml = builder.build(partition);
                
                // Validate if enabled
                if (validateOutput) {
                    DmnValidator validator = new DmnValidator();
                    DmnValidator.ValidationResult validation = validator.validate(dmnXml);
                    if (!validation.isValid()) {
                        return FileResult.failure(fileName, 
                            "Validation failed: " + validation.getErrors(), 
                            Duration.between(start, Instant.now()));
                    }
                }
                
                // Write to file
                Files.writeString(outputFile, dmnXml);
                generatedFiles.add(outputFile);
            }
            
            Duration duration = Duration.between(start, Instant.now());
            return FileResult.success(fileName, rules.size(), generatedFiles.size(), duration);
            
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            return FileResult.failure(fileName, e.getMessage(), duration);
        }
    }
    
    /**
     * Partition rules into chunks based on maxRulesPerDmn.
     */
    private List<List<Rule>> partitionRules(List<Rule> rules) {
        if (maxRulesPerDmn <= 0 || rules.size() <= maxRulesPerDmn) {
            return Collections.singletonList(rules);
        }
        
        List<List<Rule>> partitions = new ArrayList<>();
        for (int i = 0; i < rules.size(); i += maxRulesPerDmn) {
            int end = Math.min(i + maxRulesPerDmn, rules.size());
            partitions.add(new ArrayList<>(rules.subList(i, end)));
        }
        
        return partitions;
    }
    
    /**
     * Shutdown the executor service.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Builder for BatchRuleEngine.
     */
    public static class Builder {
        private int maxThreads = Runtime.getRuntime().availableProcessors();
        private int maxRulesPerDmn = 1000; // Partition large rule sets
        private boolean validateOutput = true;
        private Path outputDirectory = Paths.get("output");
        
        public Builder maxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
            return this;
        }
        
        public Builder maxRulesPerDmn(int maxRulesPerDmn) {
            this.maxRulesPerDmn = maxRulesPerDmn;
            return this;
        }
        
        public Builder validateOutput(boolean validateOutput) {
            this.validateOutput = validateOutput;
            return this;
        }
        
        public Builder outputDirectory(Path outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }
        
        public BatchRuleEngine build() {
            return new BatchRuleEngine(this);
        }
    }
    
    /**
     * Result of processing a single file.
     */
    public static class FileResult {
        private final String fileName;
        private final boolean success;
        private final int rulesProcessed;
        private final int dmnFilesGenerated;
        private final String errorMessage;
        private final Duration processingTime;
        
        private FileResult(String fileName, boolean success, int rulesProcessed, 
                          int dmnFilesGenerated, String errorMessage, Duration processingTime) {
            this.fileName = fileName;
            this.success = success;
            this.rulesProcessed = rulesProcessed;
            this.dmnFilesGenerated = dmnFilesGenerated;
            this.errorMessage = errorMessage;
            this.processingTime = processingTime;
        }
        
        public static FileResult success(String fileName, int rulesProcessed, 
                                        int dmnFilesGenerated, Duration processingTime) {
            return new FileResult(fileName, true, rulesProcessed, dmnFilesGenerated, null, processingTime);
        }
        
        public static FileResult failure(String fileName, String errorMessage, Duration processingTime) {
            return new FileResult(fileName, false, 0, 0, errorMessage, processingTime);
        }
        
        public String getFileName() { return fileName; }
        public boolean isSuccess() { return success; }
        public int getRulesProcessed() { return rulesProcessed; }
        public int getDmnFilesGenerated() { return dmnFilesGenerated; }
        public String getErrorMessage() { return errorMessage; }
        public Duration getProcessingTime() { return processingTime; }
    }
    
    /**
     * Overall batch processing result.
     */
    public static class BatchResult {
        private final int totalFiles;
        private final int successfulFiles;
        private final int totalRules;
        private final int totalDmnFiles;
        private final Duration totalTime;
        private final List<FileResult> fileResults;
        
        public BatchResult(int totalFiles, int successfulFiles, int totalRules, 
                          int totalDmnFiles, Duration totalTime, List<FileResult> fileResults) {
            this.totalFiles = totalFiles;
            this.successfulFiles = successfulFiles;
            this.totalRules = totalRules;
            this.totalDmnFiles = totalDmnFiles;
            this.totalTime = totalTime;
            this.fileResults = fileResults;
        }
        
        public int getTotalFiles() { return totalFiles; }
        public int getSuccessfulFiles() { return successfulFiles; }
        public int getFailedFiles() { return totalFiles - successfulFiles; }
        public int getTotalRules() { return totalRules; }
        public int getTotalDmnFiles() { return totalDmnFiles; }
        public Duration getTotalTime() { return totalTime; }
        public List<FileResult> getFileResults() { return fileResults; }
        
        public void printSummary() {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📊 BATCH PROCESSING SUMMARY");
            System.out.println("=".repeat(60));
            System.out.println("Total Files Processed:    " + totalFiles);
            System.out.println("Successful:               " + successfulFiles + " ✓");
            System.out.println("Failed:                   " + getFailedFiles() + (getFailedFiles() > 0 ? " ✗" : ""));
            System.out.println("Total Rules Converted:    " + totalRules);
            System.out.println("Total DMN Files Created:  " + totalDmnFiles);
            System.out.println("Total Processing Time:    " + formatDuration(totalTime));
            System.out.println("Average Time per File:    " + 
                (totalFiles > 0 ? formatDuration(totalTime.dividedBy(totalFiles)) : "N/A"));
            System.out.println("=".repeat(60));
            
            if (getFailedFiles() > 0) {
                System.out.println("\n❌ Failed Files:");
                fileResults.stream()
                    .filter(r -> !r.isSuccess())
                    .forEach(r -> System.out.println("  - " + r.getFileName() + ": " + r.getErrorMessage()));
            }
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
}

