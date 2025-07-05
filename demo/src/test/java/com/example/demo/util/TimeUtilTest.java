package com.example.demo.util;

import com.example.demo.exception.ScheduleValidationException;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.*;

class TimeUtilTest {

    @Test
    void formatTime_validFormats() {
        assertThat(TimeUtil.formatTime("9:05")).isEqualTo("09:05");
        assertThat(TimeUtil.formatTime("09:05")).isEqualTo("09:05");
        assertThat(TimeUtil.formatTime(" 9:05 ")).isEqualTo("09:05");
        assertThat(TimeUtil.formatTime("9:5")).isNull();
        assertThat(TimeUtil.formatTime(null)).isNull();
        assertThat(TimeUtil.formatTime("")).isNull();
        assertThat(TimeUtil.formatTime("xx:yy")).isNull();
    }

    @Test
    void formatTime_removesGarbage() {
        assertThat(TimeUtil.formatTime("09:05abc")).isEqualTo("09:05");
        assertThat(TimeUtil.formatTime("abc09:05")).isEqualTo("09:05");
        assertThat(TimeUtil.formatTime("abc")).isNull();
    }

    @Test
    void calculateDuration_worksForCorrectTimes() {
        assertThat(TimeUtil.calculateDuration("9:00", "10:15")).isEqualTo(75);
        assertThat(TimeUtil.calculateDuration("09:30", "09:45")).isEqualTo(15);
        assertThat(TimeUtil.calculateDuration("09:00", "08:00")).isEqualTo(-1);
    }

    @Test
    void calculateDuration_handlesInvalidInput() {
        assertThat(TimeUtil.calculateDuration("abc", "9:00")).isNull();
        assertThat(TimeUtil.calculateDuration("9:00", "abc")).isNull();
        assertThat(TimeUtil.calculateDuration(null, "9:00")).isNull();
        assertThat(TimeUtil.calculateDuration("9:00", null)).isNull();
    }

    @Test
    void addOneSecondToTimeString_basicUsage() {
        assertThat(TimeUtil.addOneSecondToTimeString("10:00")).isEqualTo("10:00:01");
        assertThat(TimeUtil.addOneSecondToTimeString("10:00:58")).isEqualTo("10:00:59");
        assertThat(TimeUtil.addOneSecondToTimeString("23:59:59")).isEqualTo("00:00:00");
    }

    @Test
    void addOneSecondToTimeString_invalidInputReturnsInput() {
        assertThat(TimeUtil.addOneSecondToTimeString("")).isEmpty();
    }

    @Test
    void parseYearMonthFromFileName_correctFormat() {
        assertThat(TimeUtil.parseYearMonthFromFileName("grafik_pracy_2024-06.xlsx"))
                .isEqualTo(YearMonth.of(2024, 6));
        assertThat(TimeUtil.parseYearMonthFromFileName("GRAFIK_PRACY_2020-12.xlsx"))
                .isEqualTo(YearMonth.of(2020, 12));
        assertThat(TimeUtil.parseYearMonthFromFileName("xxx_grafik_pracy_1999-01.xlsx"))
                .isEqualTo(YearMonth.of(1999, 1));
    }

    @Test
    void parseYearMonthFromFileName_wrongFormatThrows() {
        assertThatThrownBy(() -> TimeUtil.parseYearMonthFromFileName("wrongname.xlsx"))
                .isInstanceOf(ScheduleValidationException.class);
        assertThatThrownBy(() -> TimeUtil.parseYearMonthFromFileName("grafik_pracy_202406.xlsx"))
                .isInstanceOf(ScheduleValidationException.class);
    }
}
