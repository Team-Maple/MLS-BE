package com.maple.api.common.observability;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessResidentMemoryMetricsTest {

    @Test
    void parsesVmRssKilobytesAsBytes() throws Exception {
        BufferedReader status = status(
            "Name:\tjava\n"
                + "VmSize:\t987654 kB\n"
                + "VmRSS:\t 12345 kB\n"
                + "Threads:\t42\n");

        assertThat(ProcessResidentMemoryMetrics.parseResidentBytes(status))
            .isEqualTo(12_641_280L);
    }

    @Test
    void rejectsStatusWithoutVmRss() {
        BufferedReader status = status("Name:\tjava\nVmSize:\t987654 kB\n");

        assertThatIOException()
            .isThrownBy(() -> ProcessResidentMemoryMetrics.parseResidentBytes(status))
            .withMessageContaining("VmRSS");
    }

    @Test
    void rejectsVmRssWithUnexpectedUnit() {
        BufferedReader status = status("VmRSS:\t12345 MB\n");

        assertThatIOException()
            .isThrownBy(() -> ProcessResidentMemoryMetrics.parseResidentBytes(status))
            .withMessageContaining("VmRSS");
    }

    @Test
    void rejectsNonNumericVmRss() {
        BufferedReader status = status("VmRSS:\tnot-a-number kB\n");

        assertThatThrownBy(() -> ProcessResidentMemoryMetrics.parseResidentBytes(status))
            .isInstanceOf(NumberFormatException.class);
    }

    private BufferedReader status(String contents) {
        return new BufferedReader(new StringReader(contents));
    }
}
