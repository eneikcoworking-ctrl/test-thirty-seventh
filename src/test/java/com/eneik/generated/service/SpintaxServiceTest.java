package com.eneik.generated.service;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class SpintaxServiceTest {

    private final SpintaxService spintaxService = new SpintaxService();

    @Test
    public void testEvaluateWithNull() {
        assertNull(spintaxService.evaluate(null));
    }

    @Test
    public void testEvaluateWithEmptyString() {
        assertEquals("", spintaxService.evaluate(""));
    }

    @Test
    public void testEvaluateWithNoSpintax() {
        String template = "Hello World!";
        assertEquals(template, spintaxService.evaluate(template));
    }

    @Test
    public void testEvaluateWithSingleChoice() {
        String template = "Hello {World}!";
        assertEquals("Hello World!", spintaxService.evaluate(template));
    }

    @Test
    public void testEvaluateWithMultipleChoices() {
        String template = "Hello {World|Universe|Everyone}!";
        Set<String> expected = Set.of("Hello World!", "Hello Universe!", "Hello Everyone!");

        // Run several times to ensure all random choices are covered
        for (int i = 0; i < 50; i++) {
            String result = spintaxService.evaluate(template);
            assertTrue(expected.contains(result), "Unexpected result: " + result);
        }
    }

    @Test
    public void testEvaluateWithNestedSpintax() {
        String template = "{Hi|{Hello|Hey}} there!";
        Set<String> expected = Set.of("Hi there!", "Hello there!", "Hey there!");

        for (int i = 0; i < 50; i++) {
            String result = spintaxService.evaluate(template);
            assertTrue(expected.contains(result), "Unexpected nested result: " + result);
        }
    }

    @Test
    public void testEvaluateWithUnmatchedBraces() {
        String template1 = "Hello {World!";
        assertEquals(template1, spintaxService.evaluate(template1));

        String template2 = "Hello World}!";
        assertEquals(template2, spintaxService.evaluate(template2));
    }
}
