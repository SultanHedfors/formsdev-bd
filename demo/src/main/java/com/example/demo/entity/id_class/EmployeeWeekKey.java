package com.example.demo.entity.id_class;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class EmployeeWeekKey implements Serializable {
    private Integer employeeId;
    private LocalDate weekStart;

    public EmployeeWeekKey() {
    }

    public EmployeeWeekKey(Integer employeeId, LocalDate weekStart) {
        this.employeeId = employeeId;
        this.weekStart = weekStart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeWeekKey)) return false;
        EmployeeWeekKey that = (EmployeeWeekKey) o;
        return Objects.equals(employeeId, that.employeeId) &&
                Objects.equals(weekStart, that.weekStart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, weekStart);
    }
}
