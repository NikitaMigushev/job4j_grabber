package ru.job4j.grabber;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

public class HabrCareerDateTimeParserTest {
    private HabrCareerDateTimeParser parser = new HabrCareerDateTimeParser();

    @Test
    public void testParse() {
        String input = "2023-05-20T10:30:00+00:00";
        LocalDateTime expected = LocalDateTime.of(2023, 5, 20, 10, 30, 0);
        LocalDateTime result = parser.parse(input);
        assertEquals(expected, result);
    }

    @Test
    public void testParseWithDifferentTimezoneOffset() {
        String input = "2023-05-20T10:30:00+03:00";
        LocalDateTime expected = LocalDateTime.of(2023, 5, 20, 7, 30, 0); // converted to UTC
        LocalDateTime result = parser.parse(input);
        assertEquals(expected, result);
    }
}