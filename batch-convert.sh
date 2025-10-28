#!/bin/bash

# Batch Rule Converter - Convert 100+ rule files to DMN
# Usage: ./batch-convert.sh [input-dir] [output-dir] [threads]

set -e

INPUT_DIR=${1:-rules-input}
OUTPUT_DIR=${2:-dmn-output}
THREADS=${3:-$(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 4)}

echo "╔════════════════════════════════════════════════════════════╗"
echo "║         Batch Rule Engine - English to DMN Converter       ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "Configuration:"
echo "  Input Directory:  $INPUT_DIR"
echo "  Output Directory: $OUTPUT_DIR"
echo "  Threads:          $THREADS"
echo ""

# Check if input directory exists
if [ ! -d "$INPUT_DIR" ]; then
    echo "❌ Error: Input directory '$INPUT_DIR' does not exist"
    echo ""
    echo "Would you like to generate sample test files? (y/n)"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        echo ""
        echo "How many test files? (default: 100)"
        read -r num_files
        num_files=${num_files:-100}
        
        chmod +x generate-test-files.sh
        ./generate-test-files.sh "$INPUT_DIR" "$num_files"
        echo ""
        echo "✅ Test files generated! Continuing with conversion..."
        echo ""
    else
        exit 1
    fi
fi

# Count input files
NUM_FILES=$(find "$INPUT_DIR" -name "*.txt" -type f | wc -l | tr -d ' ')

if [ "$NUM_FILES" -eq 0 ]; then
    echo "❌ Error: No .txt files found in '$INPUT_DIR'"
    exit 1
fi

echo "Found $NUM_FILES rule files to process"
echo ""

# Compile
echo "🔨 Compiling..."
./gradlew compileJava --no-daemon -q

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed"
    exit 1
fi

echo "✅ Compilation successful"
echo ""

# Run batch processor
echo "🚀 Starting batch conversion..."
echo ""

java -Xmx4g -cp build/classes/java/main org.example.BatchProcessor \
    "$INPUT_DIR" "$OUTPUT_DIR" --threads "$THREADS"

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║                    ✅ SUCCESS!                             ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo "DMN files are ready in: $OUTPUT_DIR/"
    echo ""
    echo "Next steps:"
    echo "  1. Open DMN files in Camunda Modeler"
    echo "  2. Deploy to Camunda Platform"
    echo "  3. Test with sample data"
    echo ""
    echo "Quick check:"
    echo "  ls -lh $OUTPUT_DIR/*.dmn | head -5"
else
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║                    ❌ FAILED                               ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo "Some files failed to process. Check the error messages above."
    exit $EXIT_CODE
fi

