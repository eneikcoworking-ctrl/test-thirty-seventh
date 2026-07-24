package com.eneik.generated.service;

import com.eneik.generated.domain.DailyLimitTracker;
import com.eneik.generated.domain.OutreachSession;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.exception.DailyLimitExceededException;
import com.eneik.generated.exception.SessionLimitReachedException;
import com.eneik.generated.integration.TelegramClient;
import com.eneik.generated.repository.DailyLimitTrackerRepository;
import com.eneik.generated.repository.OutreachSessionRepository;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.util.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Random;

@Service
public class MessageDispatchService {

    private static final Logger log = LoggerFactory.getLogger(MessageDispatchService.class);

    private final TgAccountRepository tgAccountRepository;
    private final DailyLimitTrackerRepository dailyLimitTrackerRepository;
    private final OutreachSessionRepository outreachSessionRepository;
    private final Sleeper sleeper;
    private final TelegramClient telegramClient;
    private final Random random = new Random();

    @Value("${app.messaging.pause.min-seconds:120}")
    private int minPauseSeconds;

    @Value("${app.messaging.pause.max-seconds:300}")
    private int maxPauseSeconds;

    public MessageDispatchService(
            TgAccountRepository tgAccountRepository,
            DailyLimitTrackerRepository dailyLimitTrackerRepository,
            OutreachSessionRepository outreachSessionRepository,
            Sleeper sleeper,
            TelegramClient telegramClient) {
        this.tgAccountRepository = tgAccountRepository;
        this.dailyLimitTrackerRepository = dailyLimitTrackerRepository;
        this.outreachSessionRepository = outreachSessionRepository;
        this.sleeper = sleeper;
        this.telegramClient = telegramClient;
    }

    @Transactional
    public void dispatchMessage(Long tgAccountId, String leadIdentifier, String messageText) {
        log.info("Attempting to dispatch message from tgAccountId {} to lead {}", tgAccountId, leadIdentifier);

        TgAccount tgAccount = tgAccountRepository.findById(tgAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Telegram account not found with ID: " + tgAccountId));

        // 1. Check outreach session status and limit (max 8 back-and-forth messages)
        OutreachSession session = outreachSessionRepository.findByTgAccountIdAndLeadIdentifier(tgAccountId, leadIdentifier)
                .orElseGet(() -> {
                    OutreachSession s = new OutreachSession();
                    s.setTgAccount(tgAccount);
                    s.setLeadIdentifier(leadIdentifier);
                    s.setMessageCount(0);
                    s.setBlocked(false);
                    return s;
                });

        if (Boolean.TRUE.equals(session.getBlocked()) || session.getMessageCount() >= 8) {
            log.warn("Session with lead {} blocked or message count limit reached (count: {})", leadIdentifier, session.getMessageCount());
            session.setBlocked(true);
            outreachSessionRepository.save(session);
            throw new SessionLimitReachedException("Session has reached the limit of 8 back-and-forth messages.");
        }

        // 2. Check daily message/conversation limit
        LocalDate today = LocalDate.now();
        DailyLimitTracker tracker = dailyLimitTrackerRepository.findByTgAccountIdAndTrackedDate(tgAccountId, today)
                .orElseGet(() -> {
                    DailyLimitTracker t = new DailyLimitTracker();
                    t.setTgAccount(tgAccount);
                    t.setTrackedDate(today);
                    t.setSentCount(0);
                    return t;
                });

        int limit = tgAccount.getDailyLimit() != null ? tgAccount.getDailyLimit() : 20;
        if (tracker.getSentCount() >= limit) {
            log.warn("Daily messaging limit reached for account {}: current sent count: {}, limit: {}", tgAccount.getPhoneNumber(), tracker.getSentCount(), limit);
            throw new DailyLimitExceededException("Daily messaging limit reached for this account.");
        }

        // 3. Emulate human behavior: typing signal preceding a randomized pause, then send
        int pauseSeconds = minPauseSeconds;
        if (maxPauseSeconds > minPauseSeconds) {
            pauseSeconds = minPauseSeconds + random.nextInt(maxPauseSeconds - minPauseSeconds + 1);
        }

        log.info("Sending typing signal. Randomized pause of {} seconds preceding message dispatch...", pauseSeconds);
        telegramClient.sendTypingSignal(tgAccount.getPhoneNumber(), leadIdentifier);

        try {
            sleeper.sleep(pauseSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Message dispatch pause interrupted", e);
        }

        // 4. Send the message
        telegramClient.sendMessage(tgAccount.getPhoneNumber(), leadIdentifier, messageText);

        // 5. Update tracker counters
        tracker.setSentCount(tracker.getSentCount() + 1);
        dailyLimitTrackerRepository.save(tracker);

        session.setMessageCount(session.getMessageCount() + 1);
        if (session.getMessageCount() >= 8) {
            session.setBlocked(true);
        }
        outreachSessionRepository.save(session);

        log.info("Message successfully dispatched. Session message count: {}, Daily sent count: {}", session.getMessageCount(), tracker.getSentCount());
    }

    @Transactional
    public void receiveMessage(Long tgAccountId, String leadIdentifier, String messageText) {
        log.info("Received message on tgAccountId {} from lead {}", tgAccountId, leadIdentifier);

        TgAccount tgAccount = tgAccountRepository.findById(tgAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Telegram account not found with ID: " + tgAccountId));

        OutreachSession session = outreachSessionRepository.findByTgAccountIdAndLeadIdentifier(tgAccountId, leadIdentifier)
                .orElseGet(() -> {
                    OutreachSession s = new OutreachSession();
                    s.setTgAccount(tgAccount);
                    s.setLeadIdentifier(leadIdentifier);
                    s.setMessageCount(0);
                    s.setBlocked(false);
                    return s;
                });

        session.setMessageCount(session.getMessageCount() + 1);
        if (session.getMessageCount() >= 8) {
            session.setBlocked(true);
        }
        outreachSessionRepository.save(session);

        log.info("Session updated with received message. Session message count: {}", session.getMessageCount());
    }
}
