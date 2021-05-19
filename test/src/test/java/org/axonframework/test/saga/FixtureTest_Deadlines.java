/*
 * Copyright (c) 2010-2021. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.test.saga;

import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.eventhandling.Timestamp;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.test.AxonAssertionError;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.Instant;

import static org.axonframework.deadline.GenericDeadlineMessage.asDeadlineMessage;
import static org.axonframework.test.matchers.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class intended to validate all methods in regards to scheduling and validating deadlines.
 *
 * @author Milan Savic
 * @author Steven van Beelen
 */
class FixtureTest_Deadlines {

    private static final String AGGREGATE_ID = "id";
    private static final TriggerSagaStartEvent START_SAGA_EVENT = new TriggerSagaStartEvent(AGGREGATE_ID);
    private static final int TRIGGER_DURATION_MINUTES = 10;
    private static final String DEADLINE_NAME = "deadlineName";
    private static final String DEADLINE_PAYLOAD = "deadlineDetails";
    private static final String NONE_OCCURRING_DEADLINE_PAYLOAD = "none-occurring-deadline";

    private SagaTestFixture<MySaga> fixture;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(MySaga.class);
    }

    @Test
    void testExpectScheduledDeadline() {
        fixture.givenNoPriorActivity()
               .whenAggregate(AGGREGATE_ID)
               .publishes(START_SAGA_EVENT)
               .expectActiveSagas(1)
               .expectScheduledDeadline(Duration.ofMinutes(TRIGGER_DURATION_MINUTES), DEADLINE_PAYLOAD)
               .expectNoScheduledEvents();
    }

    @Test
    void testExpectScheduledDeadlineOfType() {
        fixture.givenNoPriorActivity()
               .whenAggregate(AGGREGATE_ID)
               .publishes(START_SAGA_EVENT)
               .expectActiveSagas(1)
               .expectScheduledDeadlineOfType(Duration.ofMinutes(TRIGGER_DURATION_MINUTES), String.class)
               .expectNoScheduledEvents();
    }

    @Test
    void testExpectScheduledDeadlineWithName() {
        fixture.givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenAggregate(AGGREGATE_ID)
               .publishes(new PayloadlessDeadlineShouldBeSetEvent(AGGREGATE_ID))
               .expectScheduledDeadlineWithName(Duration.ofMinutes(TRIGGER_DURATION_MINUTES), "payloadless-deadline");
    }

    @Test
    void testExpectNoScheduledDeadline() {
        fixture.givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenPublishingA(new ResetTriggerEvent(AGGREGATE_ID))
               .expectActiveSagas(1)
               .expectNoScheduledDeadline(Duration.ofMinutes(TRIGGER_DURATION_MINUTES), DEADLINE_PAYLOAD)
               .expectNoScheduledEvents();
    }

    @Test
    void testExpectNoScheduledDeadlineOfType() {
        fixture.givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenPublishingA(new ResetTriggerEvent(AGGREGATE_ID))
               .expectActiveSagas(1)
               .expectNoScheduledDeadlineOfType(Duration.ofMinutes(TRIGGER_DURATION_MINUTES), String.class)
               .expectNoScheduledEvents();
    }

    @Test
    void testExpectNoScheduledDeadlineWithName() {
        fixture.givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenPublishingA(new ResetTriggerEvent(AGGREGATE_ID))
               .expectActiveSagas(1)
               .expectNoScheduledDeadlineWithName(Duration.ofMinutes(TRIGGER_DURATION_MINUTES), DEADLINE_NAME)
               .expectNoScheduledEvents();
    }

    @Test
    void testDeadlineMetMatching() {
        //noinspection deprecation
        fixture.givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
               .expectActiveSagas(1)
               .expectDeadlinesMetMatching(payloadsMatching(exactSequenceOf(equalTo(DEADLINE_PAYLOAD))))
               .expectNoScheduledEvents();
    }

    @Test
    void testTriggeredDeadlinesMatching() {
        fixture.givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
               .expectActiveSagas(1)
               .expectTriggeredDeadlinesMatching(payloadsMatching(exactSequenceOf(equalTo(DEADLINE_PAYLOAD))))
               .expectNoScheduledEvents();
    }

    @Test
    void testDeadlineMet() {
        //noinspection deprecation
        fixture.givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
               .expectActiveSagas(1)
               .expectDeadlinesMet(DEADLINE_PAYLOAD)
               .expectNoScheduledEvents();
    }

    @Test
    void testTriggeredDeadlines() {
        fixture.givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
               .expectActiveSagas(1)
               .expectTriggeredDeadlines(DEADLINE_PAYLOAD)
               .expectNoScheduledEvents();
    }

    @Test
    void testTriggeredDeadlinesFailsForIncorrectDeadlines() {
        ContinuedGivenState given = fixture.givenAggregate(AGGREGATE_ID)
                                           .published(START_SAGA_EVENT);

        AxonAssertionError result = assertThrows(
                AxonAssertionError.class,
                () -> given.whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
                           .expectTriggeredDeadlines(NONE_OCCURRING_DEADLINE_PAYLOAD)
        );

        assertTrue(
                result.getMessage().contains("Expected deadlines were not triggered at the given deadline manager.")
        );
    }

    @Test
    void testTriggeredDeadlinesFailsForIncorrectNumberOfDeadlines() {
        ContinuedGivenState given = fixture.givenAggregate(AGGREGATE_ID)
                                           .published(START_SAGA_EVENT);

        AxonAssertionError result = assertThrows(
                AxonAssertionError.class,
                () -> given.whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
                           .expectTriggeredDeadlines(DEADLINE_PAYLOAD, NONE_OCCURRING_DEADLINE_PAYLOAD)
        );

        assertTrue(result.getMessage().contains("Got wrong number of triggered deadlines."));
    }

    @Test
    void testTriggeredDeadlinesWithName() {
        fixture.givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
               .expectActiveSagas(1)
               .expectTriggeredDeadlinesWithName(DEADLINE_NAME)
               .expectNoScheduledEvents();
    }

    @Test
    void testTriggeredDeadlinesWithNameFailsForIncorrectDeadlines() {
        ContinuedGivenState given = fixture.givenAggregate(AGGREGATE_ID)
                                           .published(START_SAGA_EVENT);
        AxonAssertionError result = assertThrows(
                AxonAssertionError.class,
                () -> given.whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
                           .expectTriggeredDeadlinesWithName(NONE_OCCURRING_DEADLINE_PAYLOAD)
        );

        assertTrue(
                result.getMessage().contains("Expected deadlines were not triggered at the given deadline manager.")
        );
    }

    @Test
    void testTriggeredDeadlinesWithNameFailsForIncorrectNumberOfDeadlines() {
        ContinuedGivenState given = fixture.givenAggregate(AGGREGATE_ID)
                                           .published(START_SAGA_EVENT);

        AxonAssertionError result = assertThrows(
                AxonAssertionError.class,
                () -> given.whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
                           .expectTriggeredDeadlinesWithName(DEADLINE_NAME, NONE_OCCURRING_DEADLINE_PAYLOAD)
        );

        assertTrue(result.getMessage().contains("Got wrong number of triggered deadlines."));
    }

    @Test
    void testTriggeredDeadlinesOfType() {
        fixture.givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
               .expectActiveSagas(1)
               .expectTriggeredDeadlinesOfType(String.class)
               .expectNoScheduledEvents();
    }

    @Test
    void testTriggeredDeadlinesOfTypeFailsForIncorrectDeadlines() {
        ContinuedGivenState given = fixture.givenAggregate(AGGREGATE_ID)
                                           .published(START_SAGA_EVENT);

        AxonAssertionError result = assertThrows(
                AxonAssertionError.class,
                () -> given.whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
                           .expectTriggeredDeadlinesOfType(Integer.class)
        );

        assertTrue(
                result.getMessage().contains("Expected deadlines were not triggered at the given deadline manager.")
        );
    }

    @Test
    void testTriggeredDeadlinesOfTypeFailsForIncorrectNumberOfDeadlines() {
        ContinuedGivenState given = fixture.givenAggregate(AGGREGATE_ID)
                                           .published(START_SAGA_EVENT);

        AxonAssertionError result = assertThrows(
                AxonAssertionError.class,
                () -> given.whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
                           .expectTriggeredDeadlinesOfType(String.class, String.class)
        );

        assertTrue(result.getMessage().contains("Got wrong number of triggered deadlines."));
    }

    @Test
    void testDeadlineCancelled() {
        fixture.givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenPublishingA(new ResetTriggerEvent(AGGREGATE_ID))
               .expectActiveSagas(1)
               .expectNoScheduledDeadlines()
               .expectNoScheduledEvents();
    }

    @Test
    void testDeadlineWhichCancelsAll() {
        fixture.givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenPublishingA(new ResetAllTriggeredEvent(AGGREGATE_ID))
               .expectActiveSagas(1)
               .expectNoScheduledDeadlines()
               .expectNoScheduledEvents();
    }

    @Test
    void testDeadlineDispatchInterceptor() {
        fixture.registerDeadlineDispatchInterceptor(
                messages -> (i, m) -> asDeadlineMessage(m.getDeadlineName(), "fakeDeadlineDetails", m.getTimestamp())
        )
               .givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
               .expectActiveSagas(1)
               .expectTriggeredDeadlines("fakeDeadlineDetails")
               .expectNoScheduledEvents();
    }

    @Test
    void testDeadlineHandlerInterceptor() {
        fixture.registerDeadlineHandlerInterceptor((uow, chain) -> {
            uow.transformMessage(deadlineMessage -> asDeadlineMessage(
                    deadlineMessage.getDeadlineName(), "fakeDeadlineDetails", deadlineMessage.getTimestamp())
            );
            return chain.proceed();
        })
               .givenAggregate(AGGREGATE_ID)
               .published(START_SAGA_EVENT)
               .whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
               .expectActiveSagas(1)
               .expectTriggeredDeadlines("fakeDeadlineDetails")
               .expectNoScheduledEvents();
    }

    @Test
    void testDeadlineHandlerEndsSagaLifecycle() {
        fixture.givenAggregate(AGGREGATE_ID)
               .published(new TriggerSagaStartEvent(AGGREGATE_ID, "sagaEndingDeadline"))
               .whenTimeElapses(Duration.ofMinutes(TRIGGER_DURATION_MINUTES + 1))
               .expectActiveSagas(0);
    }

    private static class ResetAllTriggeredEvent {

        private final String identifier;

        private ResetAllTriggeredEvent(String identifier) {
            this.identifier = identifier;
        }

        public String getIdentifier() {
            return identifier;
        }
    }

    private static class PayloadlessDeadlineShouldBeSetEvent {

        private final String identifier;

        private PayloadlessDeadlineShouldBeSetEvent(String identifier) {
            this.identifier = identifier;
        }

        public String getIdentifier() {
            return identifier;
        }
    }

    @SuppressWarnings("unused")
    public static class MySaga {

        private String deadlineId;
        private String deadlineName;

        @StartSaga
        @SagaEventHandler(associationProperty = "identifier")
        public void on(TriggerSagaStartEvent event, @Timestamp Instant timestamp, DeadlineManager deadlineManager) {
            deadlineName = event.getDeadlineName();
            deadlineId = deadlineManager.schedule(
                    Duration.ofMinutes(TRIGGER_DURATION_MINUTES), deadlineName, DEADLINE_PAYLOAD
            );
        }

        @SagaEventHandler(associationProperty = "identifier")
        public void on(ResetTriggerEvent event, DeadlineManager deadlineManager) {
            deadlineManager.cancelSchedule(deadlineName, deadlineId);
        }

        @SagaEventHandler(associationProperty = "identifier")
        public void on(ResetAllTriggeredEvent event, DeadlineManager deadlineManager) {
            deadlineManager.cancelAll(deadlineName);
        }

        @SagaEventHandler(associationProperty = "identifier")
        public void on(PayloadlessDeadlineShouldBeSetEvent event, DeadlineManager deadlineManager) {
            deadlineManager.schedule(Duration.ofMinutes(TRIGGER_DURATION_MINUTES), "payloadless-deadline");
        }

        @DeadlineHandler
        public void handleDeadline(String deadlineInfo) {
            // Nothing to be done for test purposes, having this deadline handler invoked is sufficient
        }

        @DeadlineHandler(deadlineName = "payloadless-deadline")
        public void handle() {
            // Nothing to be done for test purposes, having this deadline handler invoked is sufficient
        }

        @EndSaga
        @DeadlineHandler(deadlineName = "sagaEndingDeadline")
        public void sagaEndingDeadline() {
        }
    }
}
