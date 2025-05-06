package com.example.demo.entity.id_class;

import java.io.Serializable;
import java.util.Objects;

public class EmployeeMonthKey implements Serializable {
    private Integer employeeId;
    private String month;

    public EmployeeMonthKey() {
    }

    public EmployeeMonthKey(Integer employeeId, String month) {
        this.employeeId = employeeId;
        this.month = month;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeMonthKey that)) return false;
        return Objects.equals(employeeId, that.employeeId) &&
                Objects.equals(month, that.month);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, month);
    }
}
