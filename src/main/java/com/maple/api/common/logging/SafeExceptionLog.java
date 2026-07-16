package com.maple.api.common.logging;

import org.slf4j.spi.LoggingEventBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Adds useful exception diagnostics without copying exception messages, request
 * values, or credentials into production logs.
 */
public final class SafeExceptionLog {

    static final int MAX_CAUSE_DEPTH = 8;
    static final int MAX_FRAMES_PER_THROWABLE = 40;
    static final int MAX_STACK_TRACE_CHARS = 16 * 1024;
    private static final int MAX_SUMMARY_TYPE_CHARS = 256;
    private static final int MAX_SUMMARY_FRAME_CHARS = 768;
    private static final String APPLICATION_PACKAGE_PREFIX = "com.maple.api.";
    private static final String REDACTED_MESSAGE = "Exception message redacted by logging policy";

    private SafeExceptionLog() {
    }

    public static LoggingEventBuilder addException(LoggingEventBuilder event, Throwable throwable) {
        if (throwable == null) {
            return event;
        }
        return event
            .addKeyValue("error.type", throwable.getClass().getName())
            .addKeyValue("error.message", REDACTED_MESSAGE)
            .addKeyValue("error.stack_trace", sanitizedStackTrace(throwable));
    }

    static String sanitizedStackTrace(Throwable throwable) {
        List<Throwable> causes = collectCauses(throwable);
        StringBuilder result = new StringBuilder(MAX_STACK_TRACE_CHARS);

        append(result, "Exception cause summary:\n");
        for (int depth = 0; depth < causes.size(); depth++) {
            Throwable cause = causes.get(depth);
            append(result, "  [" + depth + "] type=");
            appendSummaryValue(result, cause.getClass().getName(), MAX_SUMMARY_TYPE_CHARS);
            append(result, "\n");

            StackTraceElement applicationFrame = firstApplicationFrame(cause);
            if (applicationFrame != null) {
                append(result, "  [" + depth + "] application_frame=");
                appendSummaryValue(result, applicationFrame.toString(), MAX_SUMMARY_FRAME_CHARS);
                append(result, "\n");
            }
        }

        append(result, "Detailed stack trace:\n");
        for (int depth = 0; depth < causes.size(); depth++) {
            Throwable current = causes.get(depth);
            if (depth > 0) {
                append(result, "Caused by: ");
            }
            append(result, current.getClass().getName());
            append(result, "\n");

            StackTraceElement[] frames = current.getStackTrace();
            int frameLimit = Math.min(frames.length, MAX_FRAMES_PER_THROWABLE);
            for (int index = 0; index < frameLimit; index++) {
                append(result, "\tat " + frames[index] + "\n");
            }
            if (frames.length > frameLimit) {
                append(result, "\t... " + (frames.length - frameLimit) + " frames omitted\n");
            }
        }
        if (hasUncollectedCause(causes)) {
            append(result, "Caused by: ... cause chain truncated\n");
        }
        return result.toString();
    }

    private static List<Throwable> collectCauses(Throwable throwable) {
        List<Throwable> causes = new ArrayList<>(MAX_CAUSE_DEPTH);
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        while (current != null && causes.size() < MAX_CAUSE_DEPTH && visited.add(current)) {
            causes.add(current);
            current = current.getCause();
        }
        return causes;
    }

    private static StackTraceElement firstApplicationFrame(Throwable throwable) {
        for (StackTraceElement frame : throwable.getStackTrace()) {
            if (frame.getClassName().startsWith(APPLICATION_PACKAGE_PREFIX)) {
                return frame;
            }
        }
        return null;
    }

    private static boolean hasUncollectedCause(List<Throwable> causes) {
        return !causes.isEmpty() && causes.getLast().getCause() != null;
    }

    private static void appendSummaryValue(StringBuilder target, String value, int maxChars) {
        if (value.length() <= maxChars) {
            append(target, value);
            return;
        }
        append(target, value.substring(0, maxChars - 3));
        append(target, "...");
    }

    private static void append(StringBuilder target, String value) {
        if (target.length() >= MAX_STACK_TRACE_CHARS) {
            return;
        }
        int remaining = MAX_STACK_TRACE_CHARS - target.length();
        target.append(value, 0, Math.min(value.length(), remaining));
    }
}
