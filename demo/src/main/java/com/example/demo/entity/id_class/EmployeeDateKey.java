package com.example.demo.entity.id_class;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class EmployeeDateKey implements Serializable {

    private Integer employeeId;
    private LocalDate startDay;

    public EmployeeDateKey() {
    }

    public EmployeeDateKey(Integer employeeId, LocalDate startDay) {
        this.employeeId = employeeId;
        this.startDay = startDay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeDateKey that)) return false;
        return Objects.equals(employeeId, that.employeeId) &&
                Objects.equals(startDay, that.startDay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, startDay);
    }
}
