#!/bin/bash

# Script to generate sample rule files for testing batch processing

OUTPUT_DIR=${1:-rules-input}
NUM_FILES=${2:-100}

echo "🔧 Generating $NUM_FILES test rule files in $OUTPUT_DIR/"

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Sample rule templates
declare -a RULE_TEMPLATES=(
    "If customer_type is {type} and order_amount > {amount}, discount is {discount}% and priority is {priority}."
    "If product_category is {category} and quantity >= {qty}, shipping_cost is {cost} and delivery_time is {time}."
    "If user_age >= {age} and membership_level is {level}, access_level is {access} and trial_period is {trial}."
    "If transaction_amount between {min} and {max}, risk_score is {risk} and approval_status is {status}."
    "If region in ({region1}, {region2}) and total_value > {value}, tax_rate is {tax}%."
    "If inventory_level < {threshold} and demand_forecast is {demand}, reorder_quantity is {qty} and priority is {priority}."
    "If customer_segment is {segment} and purchase_frequency > {freq}, loyalty_points is {points} and tier is {tier}."
    "If payment_method is {method} and transaction_type is {type}, processing_fee is {fee}% and settlement_time is {time}."
)

# Value options
TYPES=("premium" "gold" "silver" "bronze" "standard")
CATEGORIES=("electronics" "clothing" "food" "books" "furniture")
LEVELS=("basic" "standard" "premium" "enterprise")
PRIORITIES=("high" "medium" "low")
STATUSES=("approved" "pending" "rejected" "review")
SEGMENTS=("new" "regular" "vip" "inactive")
METHODS=("credit_card" "debit_card" "paypal" "bank_transfer")
REGIONS=("north" "south" "east" "west" "central")

# Function to get random element from array
get_random() {
    local arr=("$@")
    echo "${arr[$RANDOM % ${#arr[@]}]}"
}

# Function to generate random number in range
random_range() {
    local min=$1
    local max=$2
    echo $((min + RANDOM % (max - min + 1)))
}

# Generate files
for i in $(seq 1 $NUM_FILES); do
    FILE_NAME=$(printf "%s/rules_%03d.txt" "$OUTPUT_DIR" "$i")
    
    # Determine number of rules per file (5-20)
    NUM_RULES=$(random_range 5 20)
    
    # Clear file
    > "$FILE_NAME"
    
    # Generate rules
    for j in $(seq 1 $NUM_RULES); do
        # Pick random template
        TEMPLATE_IDX=$((RANDOM % ${#RULE_TEMPLATES[@]}))
        TEMPLATE="${RULE_TEMPLATES[$TEMPLATE_IDX]}"
        
        # Replace placeholders
        RULE="$TEMPLATE"
        RULE="${RULE//\{type\}/$(get_random "${TYPES[@]}")}"
        RULE="${RULE//\{category\}/$(get_random "${CATEGORIES[@]}")}"
        RULE="${RULE//\{level\}/$(get_random "${LEVELS[@]}")}"
        RULE="${RULE//\{priority\}/$(get_random "${PRIORITIES[@]}")}"
        RULE="${RULE//\{status\}/$(get_random "${STATUSES[@]}")}"
        RULE="${RULE//\{segment\}/$(get_random "${SEGMENTS[@]}")}"
        RULE="${RULE//\{method\}/$(get_random "${METHODS[@]}")}"
        RULE="${RULE//\{access\}/$(get_random "${LEVELS[@]}")}"
        RULE="${RULE//\{demand\}/$(get_random "${PRIORITIES[@]}")}"
        RULE="${RULE//\{tier\}/$(get_random "${LEVELS[@]}")}"
        RULE="${RULE//\{risk\}/$(get_random "${PRIORITIES[@]}")}"
        
        RULE="${RULE//\{amount\}/$(random_range 100 10000)}"
        RULE="${RULE//\{discount\}/$(random_range 5 50)}"
        RULE="${RULE//\{qty\}/$(random_range 1 100)}"
        RULE="${RULE//\{cost\}/$(random_range 5 50)}"
        RULE="${RULE//\{time\}/$(random_range 1 30)}"
        RULE="${RULE//\{age\}/$(random_range 18 65)}"
        RULE="${RULE//\{trial\}/$(random_range 7 90)}"
        RULE="${RULE//\{min\}/$(random_range 100 1000)}"
        RULE="${RULE//\{max\}/$(random_range 2000 10000)}"
        RULE="${RULE//\{value\}/$(random_range 500 5000)}"
        RULE="${RULE//\{tax\}/$(random_range 5 25)}"
        RULE="${RULE//\{threshold\}/$(random_range 10 100)}"
        RULE="${RULE//\{freq\}/$(random_range 1 50)}"
        RULE="${RULE//\{points\}/$(random_range 100 10000)}"
        RULE="${RULE//\{fee\}/$(random_range 1 5)}"
        
        RULE="${RULE//\{region1\}/$(get_random "${REGIONS[@]}")}"
        RULE="${RULE//\{region2\}/$(get_random "${REGIONS[@]}")}"
        
        echo "$RULE" >> "$FILE_NAME"
    done
    
    # Add a default rule
    echo "Otherwise, status is default and action is none." >> "$FILE_NAME"
    
    # Progress indicator
    if [ $((i % 10)) -eq 0 ]; then
        echo "  Generated $i/$NUM_FILES files..."
    fi
done

echo "✅ Generated $NUM_FILES rule files in $OUTPUT_DIR/"
echo ""
echo "Sample file content ($OUTPUT_DIR/rules_001.txt):"
echo "---"
head -5 "$OUTPUT_DIR/rules_001.txt"
echo "..."
echo "---"
echo ""
echo "Total rules generated: $(cat $OUTPUT_DIR/*.txt | wc -l)"
echo ""
echo "To process these files, run:"
echo "  ./gradlew compileJava -q && java -cp build/classes/java/main org.example.BatchProcessor $OUTPUT_DIR"

