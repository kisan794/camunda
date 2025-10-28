# Batch Processing Guide - 100+ Rule Files to DMN

## 🚀 Quick Start

### Step 1: Generate Test Files (Optional)
```bash
chmod +x generate-test-files.sh
./generate-test-files.sh rules-input 100
```

This creates 100 test files with 5-20 rules each (~1000-2000 total rules).

### Step 2: Run Batch Processing
```bash
# Compile
./gradlew compileJava -q

# Process all files
java -cp build/classes/java/main org.example.BatchProcessor rules-input dmn-output
```

### Step 3: Check Results
```bash
# Count generated DMN files
ls -l dmn-output/*.dmn | wc -l

# View a sample DMN file
head -50 dmn-output/rules_001.dmn
```

---

## 📋 Detailed Usage

### Command Line Options

```bash
java -cp build/classes/java/main org.example.BatchProcessor <input-dir> [output-dir] [options]
```

**Arguments:**
- `input-dir` - Directory containing `.txt` rule files (required)
- `output-dir` - Directory for generated DMN files (default: `dmn-output`)

**Options:**
- `--threads <n>` - Number of parallel threads (default: CPU cores)
- `--max-rules <n>` - Max rules per DMN file (default: 1000)
- `--no-validate` - Skip DMN validation (faster but less safe)

### Examples

#### Example 1: Basic Processing
```bash
java -cp build/classes/java/main org.example.BatchProcessor rules-input
```
- Processes all `.txt` files in `rules-input/`
- Outputs to `dmn-output/`
- Uses all CPU cores
- Max 1000 rules per DMN file

#### Example 2: Custom Output Directory
```bash
java -cp build/classes/java/main org.example.BatchProcessor rules-input my-dmn-files
```

#### Example 3: High Performance (8 threads, no validation)
```bash
java -cp build/classes/java/main org.example.BatchProcessor rules-input dmn-output --threads 8 --no-validate
```

#### Example 4: Large Rule Sets (partition at 500 rules)
```bash
java -cp build/classes/java/main org.example.BatchProcessor rules-input dmn-output --max-rules 500
```

If a file has 1200 rules, it will create:
- `filename_part1.dmn` (500 rules)
- `filename_part2.dmn` (500 rules)
- `filename_part3.dmn` (200 rules)

---

## 📁 Directory Structure

### Input Structure
```
rules-input/
├── customer-rules.txt
├── order-processing.txt
├── pricing-rules.txt
├── shipping-rules.txt
└── ... (100+ files)
```

### Output Structure
```
dmn-output/
├── customer-rules.dmn
├── order-processing.dmn
├── pricing-rules.dmn
├── shipping-rules.dmn
└── ... (100+ DMN files)
```

### With Partitioning (large files)
```
dmn-output/
├── large-ruleset_part1.dmn
├── large-ruleset_part2.dmn
├── large-ruleset_part3.dmn
└── ...
```

---

## 🎯 Performance Characteristics

### Benchmarks (approximate)

| Files | Rules | Threads | Time | Throughput |
|-------|-------|---------|------|------------|
| 10    | 100   | 4       | 2s   | 50 rules/s |
| 100   | 1000  | 8       | 15s  | 67 rules/s |
| 500   | 5000  | 16      | 60s  | 83 rules/s |
| 1000  | 10000 | 16      | 120s | 83 rules/s |

**Factors affecting performance:**
- Number of CPU cores
- Rule complexity
- Validation enabled/disabled
- Disk I/O speed
- File size distribution

### Optimization Tips

1. **Use more threads** for I/O-bound workloads:
   ```bash
   --threads 16
   ```

2. **Disable validation** for faster processing (validate later):
   ```bash
   --no-validate
   ```

3. **Partition large files** to avoid memory issues:
   ```bash
   --max-rules 500
   ```

4. **Use SSD storage** for input/output directories

---

## 🔧 Advanced Usage

### Programmatic API

```java
import org.example.batch.BatchRuleEngine;
import java.nio.file.Paths;

public class CustomBatchProcessor {
    public static void main(String[] args) throws Exception {
        // Build custom engine
        BatchRuleEngine engine = new BatchRuleEngine.Builder()
            .outputDirectory(Paths.get("output"))
            .maxThreads(8)
            .maxRulesPerDmn(1000)
            .validateOutput(true)
            .build();
        
        // Process directory
        BatchRuleEngine.BatchResult result = 
            engine.processDirectory(Paths.get("input"));
        
        // Print summary
        result.printSummary();
        
        // Access detailed results
        for (BatchRuleEngine.FileResult fileResult : result.getFileResults()) {
            if (fileResult.isSuccess()) {
                System.out.println("✓ " + fileResult.getFileName() + 
                    " - " + fileResult.getRulesProcessed() + " rules");
            } else {
                System.err.println("✗ " + fileResult.getFileName() + 
                    " - " + fileResult.getErrorMessage());
            }
        }
        
        // Cleanup
        engine.shutdown();
    }
}
```

### Custom Rule Processing

```java
import org.example.dmn.parser.NaturalLanguageRuleParser;
import org.example.dmn.builder.DmnBuilder;
import org.example.dmn.model.Rule;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class CustomProcessor {
    public static void main(String[] args) throws Exception {
        NaturalLanguageRuleParser parser = new NaturalLanguageRuleParser();
        
        // Read and parse
        String rulesText = Files.readString(Paths.get("my-rules.txt"));
        List<Rule> rules = parser.parseMultiple(rulesText);
        
        // Filter or transform rules
        List<Rule> filteredRules = rules.stream()
            .filter(r -> r.getConditions().size() > 1)
            .toList();
        
        // Generate DMN
        DmnBuilder builder = new DmnBuilder();
        builder.withDecisionName("MyDecision")
               .withDecisionId("my_decision");
        String dmn = builder.build(filteredRules);
        
        // Save
        Files.writeString(Paths.get("output.dmn"), dmn);
    }
}
```

---

## 📊 Output Summary

After processing, you'll see a summary like:

```
============================================================
📊 BATCH PROCESSING SUMMARY
============================================================
Total Files Processed:    100
Successful:               98 ✓
Failed:                   2 ✗
Total Rules Converted:    1547
Total DMN Files Created:  100
Total Processing Time:    12.345s
Average Time per File:    0.123s
============================================================

❌ Failed Files:
  - invalid-syntax.txt: Could not parse condition: ...
  - empty-file.txt: No valid rules found
```

---

## 🐛 Troubleshooting

### Issue: "Out of Memory"
**Solution:** Reduce `--max-rules` or increase JVM heap:
```bash
java -Xmx4g -cp build/classes/java/main org.example.BatchProcessor ...
```

### Issue: "Too many open files"
**Solution:** Reduce `--threads`:
```bash
java -cp build/classes/java/main org.example.BatchProcessor ... --threads 4
```

### Issue: Slow processing
**Solution:** 
1. Disable validation: `--no-validate`
2. Increase threads: `--threads 16`
3. Use faster storage (SSD)

### Issue: Some files fail to parse
**Solution:** Check the error messages in the summary. Common issues:
- Invalid rule syntax
- Empty files
- Unsupported patterns

---

## 📝 Rule File Format

Each `.txt` file should contain one rule per line:

```
If customer is platinum and total amount > 3000, discount is 25%.
If customer is gold and total amount between 1000 and 3000, discount is 15%.
If customer membership in (silver, bronze), discount is 5%.
Otherwise, discount is 0%.
```

**Supported patterns:**
- `If X is Y` - Equality
- `If X > Y` - Comparison (>, <, >=, <=)
- `If X between Y and Z` - Range
- `If X in (A, B, C)` - List membership
- `If X contains Y` - Contains
- `Otherwise` - Default rule

---

## 🚀 Production Deployment

### 1. Create Executable JAR
```bash
./gradlew jar
java -jar build/libs/Camunda-1.0-SNAPSHOT.jar rules-input dmn-output
```

### 2. Create Shell Script
```bash
cat > batch-convert.sh << 'EOF'
#!/bin/bash
INPUT_DIR=${1:-rules-input}
OUTPUT_DIR=${2:-dmn-output}
THREADS=${3:-8}

java -Xmx4g -cp build/classes/java/main org.example.BatchProcessor \
  "$INPUT_DIR" "$OUTPUT_DIR" --threads "$THREADS"
EOF

chmod +x batch-convert.sh
./batch-convert.sh my-rules my-dmn 16
```

### 3. Integrate with CI/CD
```yaml
# .github/workflows/convert-rules.yml
name: Convert Rules to DMN
on: [push]
jobs:
  convert:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '17'
      - run: ./gradlew compileJava
      - run: java -cp build/classes/java/main org.example.BatchProcessor rules dmn-output
      - uses: actions/upload-artifact@v2
        with:
          name: dmn-files
          path: dmn-output/
```

---

## 📈 Scaling to 1000+ Files

For very large batches:

1. **Split into chunks** and process separately
2. **Use distributed processing** (multiple machines)
3. **Monitor memory usage** and adjust heap size
4. **Use database** for rule storage instead of files
5. **Implement caching** for repeated patterns

Example for 10,000 files:
```bash
# Split into 10 batches of 1000
for i in {0..9}; do
  java -Xmx8g -cp build/classes/java/main org.example.BatchProcessor \
    rules-batch-$i dmn-output-$i --threads 16 &
done
wait
```

