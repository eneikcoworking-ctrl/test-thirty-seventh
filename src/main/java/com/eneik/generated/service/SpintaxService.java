package com.eneik.generated.service;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class SpintaxService {

    private final Random random = new Random();

    /**
     * Replaces all occurrences of {choice1|choice2|...} with a randomly selected choice.
     * Supports nested curly braces by resolving innermost structures first.
     */
    public String evaluate(String template) {
        if (template == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(template);
        while (true) {
            int end = sb.indexOf("}");
            if (end == -1) {
                break;
            }
            int start = sb.lastIndexOf("{", end);
            if (start == -1) {
                break; // unmatched closing brace
            }
            String content = sb.substring(start + 1, end);
            String[] choices = content.split("\\|");
            String selection = choices[random.nextInt(choices.length)];
            sb.replace(start, end + 1, selection);
        }
        return sb.toString();
    }
}
