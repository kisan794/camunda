import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestParser {
    public static void main(String[] args) {
        String test = "customer membership in (silver, bronze) and total quantity >= 5";
        
        // Pattern: variable in (list)
        Pattern IN_LIST_PATTERN = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_\\s]*)\\s+in\\s+\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = IN_LIST_PATTERN.matcher(test);
        if (matcher.find()) {
            System.out.println("Match found!");
            System.out.println("Variable: " + matcher.group(1));
            System.out.println("List: " + matcher.group(2));
        } else {
            System.out.println("No match");
        }
    }
}

