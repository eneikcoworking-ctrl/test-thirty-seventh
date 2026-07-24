package com.eneik.generated.service;

import com.eneik.generated.Application;
import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.Lead;
import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.SenderType;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Transactional
public class PersonalizationEngineTest {

    @Autowired
    private SpintaxService spintaxService;

    @Autowired
    private LlmPersonalizationService llmPersonalizationService;

    @Autowired
    private PersonalizationEngine personalizationEngine;

    @Autowired
    private DialogService dialogService;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    public void testSpintaxFlatEvaluation() {
        String template = "{Hello|Hi|Hey} user, please review our {fantastic|incredible} product!";

        // Let's run multiple evaluations and make sure we get expected random variants
        Set<String> possibleHellos = Set.of("Hello", "Hi", "Hey");
        Set<String> possibleAdjectives = Set.of("fantastic", "incredible");

        for (int i = 0; i < 20; i++) {
            String result = spintaxService.evaluate(template);
            assertThat(result).isNotNull();

            // Verify structure has been correctly replaced
            boolean matched = false;
            for (String hello : possibleHellos) {
                for (String adj : possibleAdjectives) {
                    String expected = hello + " user, please review our " + adj + " product!";
                    if (expected.equals(result)) {
                        matched = true;
                        break;
                    }
                }
            }
            assertThat(matched).isTrue();
        }
    }

    @Test
    public void testSpintaxNestedEvaluation() {
        // Nested curly braces: {A|{B|C}}
        // Inner {B|C} should be resolved first, then outer {A|<inner>}
        String nestedTemplate = "Choose: {Option A|{Option B|Option C}}";

        Set<String> possibleOutcomes = Set.of("Choose: Option A", "Choose: Option B", "Choose: Option C");

        for (int i = 0; i < 20; i++) {
            String result = spintaxService.evaluate(nestedTemplate);
            assertThat(result).isIn(possibleOutcomes);
        }
    }

    @Test
    public void testSpintaxEdgeCases() {
        assertThat(spintaxService.evaluate(null)).isNull();
        assertThat(spintaxService.evaluate("")).isEmpty();
        assertThat(spintaxService.evaluate("No brackets at all")).isEqualTo("No brackets at all");
        assertThat(spintaxService.evaluate("{Unmatched bracket")).isEqualTo("{Unmatched bracket");
        assertThat(spintaxService.evaluate("Another unmatched}")).isEqualTo("Another unmatched}");
    }

    @Test
    public void testLlmPersonalizationDefaultBehavior() {
        String template = "This is a cold email offer.";
        String metadata = "CEO of Acme Corp";

        String result = llmPersonalizationService.personalize(template, metadata);
        assertThat(result).contains("CEO of Acme Corp").contains("This is a cold email offer.");

        // Without metadata, should return template unchanged
        assertThat(llmPersonalizationService.personalize(template, null)).isEqualTo(template);
        assertThat(llmPersonalizationService.personalize(template, "   ")).isEqualTo(template);
    }

    @Test
    public void testLlmPersonalizationCustomRephraser() {
        String template = "Hello";
        String metadata = "Developer";

        // Inject custom rephraser
        llmPersonalizationService.setRephraser((temp, meta) -> "Hey " + meta + ", " + temp + "!");

        String result = llmPersonalizationService.personalize(template, metadata);
        assertThat(result).isEqualTo("Hey Developer, Hello!");

        // Restore default rephraser by passing custom back or re-injecting
        llmPersonalizationService.setRephraser((temp, meta) -> {
            if (meta == null || meta.trim().isEmpty()) return temp;
            return "Personalized offer based on [" + meta.trim() + "]: " + temp;
        });
    }

    @Test
    public void testPersonalizationEngineOrchestration() {
        String campaignId = UUID.randomUUID().toString();
        Campaign campaign = new Campaign(campaignId, "AI Software Campaign", "{Hi|Hey}, we built {cool|neat} tech.");

        String leadId = UUID.randomUUID().toString();
        Lead lead = new Lead(leadId, campaignId, "elonmusk", "+123", "NEW", "Tesla CEO");

        // Custom mock rephraser for precise testing
        llmPersonalizationService.setRephraser((temp, meta) -> temp + " / " + meta);

        String personalizedAndEvaluatedMessage = personalizationEngine.generatePersonalizedMessage(campaign, lead);

        assertThat(personalizedAndEvaluatedMessage).isNotNull();
        assertThat(personalizedAndEvaluatedMessage).contains("/ Tesla CEO");
        assertThat(personalizedAndEvaluatedMessage).matches("(Hi|Hey), we built (cool|neat) tech. / Tesla CEO");

        // Reset rephraser
        llmPersonalizationService.setRephraser(null);
    }

    @Test
    public void testDialogMessageLimitBlocker() {
        String chatId = "dialog_limit_test_chat";

        // Store first 7 messages successfully
        for (int i = 1; i <= 7; i++) {
            dialogService.receiveInboundMessage(chatId, "Inbound message " + i, SenderType.USER);
        }

        // The 8th message is stored successfully, but sets the AI State to STOPPED
        dialogService.receiveInboundMessage(chatId, "Inbound message 8", SenderType.USER);

        Optional<Dialog> dbDialog = dialogRepository.findByTelegramChatId(chatId);
        assertThat(dbDialog).isPresent();
        assertThat(dbDialog.get().getAiState()).isEqualTo(AiState.STOPPED);

        // Attempting to store a 9th message will be strictly blocked with an exception and state remains STOPPED
        assertThatThrownBy(() -> {
            dialogService.receiveInboundMessage(chatId, "Inbound message 9", SenderType.USER);
        }).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Conversation limit reached: back-and-forth message count exceeds 8.");

        assertThat(dbDialog.get().getAiState()).isEqualTo(AiState.STOPPED);
    }
}
