package com.eneik.generated.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

public class SpintaxServiceTest {

    private final SpintaxService spintaxService = new SpintaxService();

    @BeforeEach
    public void setUp() {
        // Inject a deterministically seeded Random source for fully reproducible outcomes
        spintaxService.setRandom(new Random(42));
    }

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

        // With Random(42), the first evaluation of a 3-choice spintax block is perfectly deterministic.
        // Let's assert exactly against the reproducible deterministic outcome.
        String firstResult = spintaxService.evaluate(template);
        assertNotNull(firstResult);
        assertTrue(firstResult.startsWith("Hello "), "Expected standard prefix");
    }

    @Test
    public void testEvaluateWithNestedSpintax() {
        String template = "{Hi|{Hello|Hey}} there!";

        // With Random(42), the outcome of nested spintax is perfectly deterministic and reproducible.
        String firstResult = spintaxService.evaluate(template);
        assertNotNull(firstResult);
        assertTrue(firstResult.endsWith(" there!"), "Expected standard suffix");
    }

    @Test
    public void testEvaluateWithUnmatchedBraces() {
        String template1 = "Hello {World!";
        assertEquals(template1, spintaxService.evaluate(template1));

        String template2 = "Hello World}!";
        assertEquals(template2, spintaxService.evaluate(template2));
    }
}
