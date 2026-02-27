package com.example.Central_Kitchens_and_Franchise_Store_BE.mapper;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class GhnStatusMapperTest {

    private final GhnStatusMapper mapper = new GhnStatusMapper();

    static Stream<Arguments> knownStatusMappings() {
        return Stream.of(
                Arguments.of("ready_to_pick", OrderStatus.READY_TO_PICK),
                Arguments.of("picking", OrderStatus.PICKING),
                Arguments.of("picked", OrderStatus.PICKED),
                Arguments.of("delivering", OrderStatus.DELIVERING),
                Arguments.of("delivered", OrderStatus.DELIVERED),
                Arguments.of("delivery_fail", OrderStatus.DELIVERY_FAILED),
                Arguments.of("waiting_to_return", OrderStatus.WAITING_TO_RETURN)
        );
    }

    static Stream<Arguments> aliasStatusMappings() {
        return Stream.of(
                Arguments.of("return_transporting", OrderStatus.RETURNED),
                Arguments.of("returned", OrderStatus.RETURNED)
        );
    }

    @ParameterizedTest
    @MethodSource("knownStatusMappings")
    @DisplayName("Maps known GHN statuses to corresponding OrderStatus values")
    void mapsKnownStatuses(String input, OrderStatus expected) {
        assertEquals(expected, mapper.map(input));
    }

    @ParameterizedTest
    @MethodSource("aliasStatusMappings")
    @DisplayName("Maps alias GHN statuses to RETURNED")
    void mapsAliasStatuses(String input, OrderStatus expected) {
        assertEquals(expected, mapper.map(input));
    }

    @Test
    @DisplayName("Is case-insensitive when mapping statuses")
    void isCaseInsensitive() {
        assertEquals(OrderStatus.DELIVERED, mapper.map("DELIVERED"));
        assertEquals(OrderStatus.PICKING, mapper.map("PiCkInG"));
    }

    @Test
    @DisplayName("Maps unknown statuses to PENDING and logs a warning with the original value")
    void unknownStatusMapsToPendingAndLogsWarning(CapturedOutput output) {
        OrderStatus status = mapper.map("some_new_status");
        assertEquals(OrderStatus.PENDING, status);
        String logs = output.getOut() + output.getErr();
        assertTrue(logs.contains("Unknown GHN status: some_new_status"),
                () -> "Expected warning log to contain original value, logs were: " + logs);
    }

    @Test
    @DisplayName("Maps blank string to PENDING and logs a warning")
    void blankStatusMapsToPendingAndLogsWarning(CapturedOutput output) {
        OrderStatus status = mapper.map("");
        assertEquals(OrderStatus.PENDING, status);
        String logs = output.getOut() + output.getErr();
        assertTrue(logs.contains("Unknown GHN status: "));
    }

    @Test
    @DisplayName("Throws NullPointerException when input is null")
    void nullInputThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> mapper.map(null));
    }

    @Test
    @DisplayName("Maps 'cancel' to CANCELLED without logging warnings")
    void cancelMapsToCancelledWithoutWarning(CapturedOutput output) {
        OrderStatus status = mapper.map("cancel");
        assertEquals(OrderStatus.CANCELLED, status);
        String logs = output.getOut() + output.getErr();
        assertFalse(logs.contains("Unknown GHN status"),
                () -> "Did not expect warning log for known status, logs were: " + logs);
    }
}
