package org.example;

import org.example.batch.BatchRuleEngine;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main class for batch processing multiple rule files.
 * 
 * Usage:
 *   java org.example.BatchProcessor <input-directory> [output-directory] [options]
 * 
 * Options:
 *   --threads <n>        Number of parallel threads (default: CPU cores)
 *   --max-rules <n>      Max rules per DMN file (default: 1000)
 *   --no-validate        Skip DMN validation
 * 
 * Example:
 *   java org.example.BatchProcessor rules-input dmn-output --threads 8 --max-rules 500
 */
public class BatchProcessor {
    
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }
        
        // Parse arguments
        Path inputDirectory = Paths.get(args[0]);
        Path outputDirectory = args.length > 1 && !args[1].startsWith("--") 
            ? Paths.get(args[1]) 
            : Paths.get("dmn-output");
        
        int maxThreads = Runtime.getRuntime().availableProcessors();
        int maxRulesPerDmn = 1000;
        boolean validate = true;
        
        // Parse options
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--threads":
                    if (i + 1 < args.length) {
                        maxThreads = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--max-rules":
                    if (i + 1 < args.length) {
                        maxRulesPerDmn = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--no-validate":
                    validate = false;
                    break;
            }
        }
        
        // Build and run the batch engine
        BatchRuleEngine engine = new BatchRuleEngine.Builder()
            .outputDirectory(outputDirectory)
            .maxThreads(maxThreads)
            .maxRulesPerDmn(maxRulesPerDmn)
            .validateOutput(validate)
            .build();
        
        try {
            BatchRuleEngine.BatchResult result = engine.processDirectory(inputDirectory);
            result.printSummary();
            
            // Exit with error code if any files failed
            if (result.getFailedFiles() > 0) {
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("❌ Batch processing failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            engine.shutdown();
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java org.example.BatchProcessor <input-directory> [output-directory] [options]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  input-directory      Directory containing .txt rule files");
        System.out.println("  output-directory     Directory for generated DMN files (default: dmn-output)");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --threads <n>        Number of parallel threads (default: CPU cores)");
        System.out.println("  --max-rules <n>      Max rules per DMN file (default: 1000)");
        System.out.println("  --no-validate        Skip DMN validation");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java org.example.BatchProcessor rules-input");
        System.out.println("  java org.example.BatchProcessor rules-input dmn-output");
        System.out.println("  java org.example.BatchProcessor rules-input dmn-output --threads 8 --max-rules 500");
    }
}

