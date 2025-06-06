/*
 * Copyright 2019-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.ui.monitor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import com.github.valfirst.slf4jtest.TestLoggerFactoryExtension;

import org.jbehave.core.annotations.When;
import org.jbehave.core.model.Meta;
import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import org.vividus.context.RunContext;
import org.vividus.model.RunningScenario;
import org.vividus.model.RunningStory;
import org.vividus.proxy.IProxy;
import org.vividus.reporter.event.IAttachmentPublisher;
import org.vividus.selenium.IWebDriverProvider;
import org.vividus.testcontext.TestContext;

import de.sstoehr.harreader.HarReader;
import de.sstoehr.harreader.HarReaderException;
import de.sstoehr.harreader.HarWriter;
import de.sstoehr.harreader.HarWriterException;
import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarLog;
import de.sstoehr.harreader.model.HarPage;
import de.sstoehr.harreader.model.HarTiming;

@ExtendWith({ MockitoExtension.class, TestLoggerFactoryExtension.class })
class PublishingHarOnFailureMonitorTests
{
    private static final String I_DO_ACTION = "I do action";
    private static final String NO_HAR_ON_FAILURE_META_NAME = "noHarOnFailure";
    private static final String ERROR_MESSAGE = "Unable to capture HAR";
    private static final Meta EMPTY_META = new Meta();

    @Mock private IAttachmentPublisher attachmentPublisher;
    @Mock private RunContext runContext;
    @Mock private IWebDriverProvider webDriverProvider;
    @Mock private IProxy proxy;
    @Mock private TestContext testContext;

    private final TestLogger logger = TestLoggerFactory.getTestLogger(
            AbstractPublishingAttachmentOnFailureMonitor.class);

    private PublishingHarOnFailureMonitor createMonitor(boolean publishHarOnFailure)
    {
        return new PublishingHarOnFailureMonitor(publishHarOnFailure, runContext, webDriverProvider,
                attachmentPublisher, proxy, testContext);
    }

    private static Method getCapturingHarMethod() throws NoSuchMethodException
    {
        return PublishingHarOnFailureMonitorTests.class.getDeclaredMethod("whenStep");
    }

    @TestFactory
    Stream<DynamicTest> getProcessStepWithAnnotation() throws NoSuchMethodException
    {
        var monitor = createMonitor(true);
        return Stream.of(getCapturingHarMethod(), TestSteps.class.getDeclaredMethod("innerWhenStep"))
                .flatMap(method -> Stream.of(
                            dynamicTest("beforePerformingProcessesStepWithAnnotation",
                                    () -> {
                                        mockScenarioAndStoryMeta();
                                        monitor.beforePerforming(I_DO_ACTION, false, method);
                                    }),
                            dynamicTest("afterPerformingProcessesStepWithAnnotation",
                                    () -> monitor.afterPerforming(I_DO_ACTION, false, method))
                        )
                );
    }

    @TestFactory
    Stream<DynamicTest> getIgnoreStepWithoutAnnotation() throws NoSuchMethodException
    {
        var monitor = createMonitor(false);
        return Stream.of(getClass().getDeclaredMethod("anotherWhenStep"), null)
                .flatMap(method -> Stream.of(
                            dynamicTest("beforePerformingIgnoresStepWithoutAnnotation",
                                    () -> monitor.beforePerforming(I_DO_ACTION, false, method)),
                            dynamicTest("afterPerformingIgnoresStepWithoutAnnotation",
                                    () -> monitor.afterPerforming(I_DO_ACTION, false, method))
                        )
                );
    }

    @Test
    void shouldEnableHarIfNoRunningScenario() throws NoSuchMethodException
    {
        mockStoryMeta(new RunningStory(), EMPTY_META);
        var monitor = createMonitor(true);
        monitor.beforePerforming(I_DO_ACTION, false, getCapturingHarMethod());
        monitor.onAssertionFailure(null);
        verify(webDriverProvider).isWebDriverInitialized();
    }

    @Test
    void shouldNotTakeHarIfMethodAnnotatedButStoryHasNoHarOnFailure()
    {
        mockStoryMeta(new RunningStory(), new Meta(List.of(NO_HAR_ON_FAILURE_META_NAME)));
        var monitor = createMonitor(true);
        monitor.beforePerforming(I_DO_ACTION, false, null);
        monitor.onAssertionFailure(null);
        verifyNoInteractions(webDriverProvider);
    }

    @Test
    void shouldNotTakeHarIfMethodAnnotatedButScenarioHasNoHarOnFailure() throws NoSuchMethodException
    {
        var runningStory = new RunningStory();
        mockStoryMeta(runningStory, EMPTY_META);
        mockScenarioMeta(runningStory, new Meta(List.of(NO_HAR_ON_FAILURE_META_NAME)));
        var monitor = createMonitor(false);
        monitor.beforePerforming(I_DO_ACTION, false, getCapturingHarMethod());
        monitor.onAssertionFailure(null);
        verifyNoInteractions(webDriverProvider);
    }

    @Test
    void shouldCaptureFullHarOnAssertionFailureAtFirstInvocation() throws NoSuchMethodException
    {
        mockScenarioAndStoryMeta();
        var har = new Har();
        har.setLog(new HarLog());
        when(proxy.getRecordedData()).thenReturn(har);
        when(webDriverProvider.isWebDriverInitialized()).thenReturn(true);
        when(testContext.get(PublishingHarOnFailureMonitor.class)).thenReturn(null);
        var monitor = createMonitor(false);
        monitor.beforePerforming(I_DO_ACTION, false, getCapturingHarMethod());
        monitor.onAssertionFailure(null);
        assertEquals("{\"log\":{\"version\":\"1.1\",\"creator\":{},\"pages\":[],\"entries\":[]}}",
                captureAttachmentPublishData());
        assertThat(logger.getLoggingEvents(), empty());
        verifyNoMoreInteractions(testContext);
    }

    @Test
    void shouldCaptureFilteredHarOnAssertionFailureAtNonFirstInvocation() throws HarReaderException
    {
        var dateAfterLastCapturing = Instant.now();
        var dateBeforeLastCapturing = dateAfterLastCapturing.minus(2, ChronoUnit.MINUTES);
        var dateOfLastCapturing = dateAfterLastCapturing.minus(1, ChronoUnit.MINUTES);

        mockScenarioAndStoryMeta();
        var har = createHarWithData(dateBeforeLastCapturing, dateAfterLastCapturing);
        when(proxy.getRecordedData()).thenReturn(har);
        when(webDriverProvider.isWebDriverInitialized()).thenReturn(true);
        when(testContext.get(PublishingHarOnFailureMonitor.class)).thenReturn(Date.from(dateOfLastCapturing));
        var monitor = createMonitor(true);
        monitor.beforePerforming(I_DO_ACTION, false, null);
        monitor.onAssertionFailure(null);
        var actualHar = new HarReader().readFromString(captureAttachmentPublishData());
        var actualHarLog = actualHar.getLog();
        assertEquals(List.of(har.getLog().getPages().get(1)), actualHarLog.getPages());
        assertEquals(List.of(har.getLog().getEntries().get(1)), actualHarLog.getEntries());
        assertThat(logger.getLoggingEvents(), empty());
        verify(testContext).put(PublishingHarOnFailureMonitor.class, Date.from(dateAfterLastCapturing));
    }

    @SuppressWarnings("PMD.UnusedLocalVariable")
    @Test
    void shouldLogErrorIfHarPublishingIsFailed()
    {
        var exception = mock(HarWriterException.class);
        try (MockedConstruction<HarWriter> harReaderConstruction = Mockito.mockConstruction(HarWriter.class,
                (m, c) -> doThrow(exception).when(m).writeTo(any(Writer.class), any())))
        {
            mockScenarioAndStoryMeta();
            var har = mock(Har.class);
            when(proxy.getRecordedData()).thenReturn(har);
            when(webDriverProvider.isWebDriverInitialized()).thenReturn(true);
            var monitor = createMonitor(true);
            monitor.beforePerforming(I_DO_ACTION, false, null);
            monitor.onAssertionFailure(null);
            List<LoggingEvent> events = logger.getLoggingEvents();
            assertThat(events, hasSize(1));
            LoggingEvent errorEvent = events.get(0);
            assertEquals(Level.ERROR, errorEvent.getLevel());
            assertEquals(exception, errorEvent.getThrowable().get().getCause());
            assertEquals(ERROR_MESSAGE, errorEvent.getMessage());
        }
    }

    private void mockScenarioAndStoryMeta()
    {
        var runningStory = new RunningStory();
        mockStoryMeta(runningStory, EMPTY_META);
        mockScenarioMeta(runningStory, EMPTY_META);
    }

    private void mockStoryMeta(RunningStory runningStory, Meta meta)
    {
        when(runContext.getRunningStory()).thenReturn(runningStory);
        runningStory.setStory(new Story(null, null, meta, null, null));
    }

    private void mockScenarioMeta(RunningStory runningStory, Meta meta)
    {
        var runningScenario = new RunningScenario();
        runningScenario.setScenario(new Scenario("test scenario", meta));
        runningStory.setRunningScenario(runningScenario);
    }

    private Har createHarWithData(Instant dateBeforeLastCapturing, Instant dateAfterLastCapturing)
    {
        var harLog = new HarLog();
        harLog.setEntries(List.of(
                createHarEntry("Entry 1", dateBeforeLastCapturing),
                createHarEntry("Entry 2", dateAfterLastCapturing)
        ));
        harLog.setPages(List.of(
                createHarPage("Page 1", dateBeforeLastCapturing),
                createHarPage("Page 2", dateAfterLastCapturing)
        ));

        var har = new Har();
        har.setLog(harLog);
        return har;
    }

    private HarEntry createHarEntry(String comment, Instant startedDateTime)
    {
        var harEntry = new HarEntry();
        harEntry.setComment(comment);
        harEntry.setStartedDateTime(Date.from(startedDateTime));
        harEntry.getResponse().setStatus(0);
        HarTiming timing = harEntry.getTimings();
        timing.setBlocked(-1);
        timing.setDns(-1);
        timing.setConnect(-1);
        timing.setSend(-1);
        timing.setWait(-1);
        timing.setReceive(-1);
        timing.setSsl(-1);
        return harEntry;
    }

    private HarPage createHarPage(String title, Instant startedDateTime)
    {
        var harPage = new HarPage();
        harPage.setTitle(title);
        harPage.setStartedDateTime(Date.from(startedDateTime));
        return harPage;
    }

    private String captureAttachmentPublishData()
    {
        var bodyCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(attachmentPublisher).publishAttachment(bodyCaptor.capture(), eq("har-on-failure.har"));
        return new String(bodyCaptor.getValue(), StandardCharsets.UTF_8);
    }

    @CaptureHarOnFailure
    @When(I_DO_ACTION)
    void whenStep()
    {
        // nothing to do
    }

    @When(I_DO_ACTION)
    void anotherWhenStep()
    {
        // nothing to do
    }

    @CaptureHarOnFailure
    static class TestSteps
    {
        @When(I_DO_ACTION)
        void innerWhenStep()
        {
            // nothing to do
        }
    }
}
