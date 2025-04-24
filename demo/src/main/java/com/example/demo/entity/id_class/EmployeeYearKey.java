package com.example.demo.entity.id_class;

import java.io.Serializable;
import java.util.Objects;

public class EmployeeYearKey implements Serializable {
    private Integer employeeId;
    private Integer year;

    public EmployeeYearKey() {
    }

    public EmployeeYearKey(Integer employeeId, Integer year) {
        this.employeeId = employeeId;
        this.year = year;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeYearKey)) return false;
        EmployeeYearKey that = (EmployeeYearKey) o;
        return Objects.equals(employeeId, that.employeeId) &&
                Objects.equals(year, that.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, year);
    }
}
