package io.lukmandev.excel_pdf_crud.employee.repository;

import io.lukmandev.excel_pdf_crud.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EmployeeRespository extends JpaRepository<Employee, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Employee> findByIdAndActiveTrue(Long id);

    List<Employee> findBySalaryBetween(BigDecimal min, BigDecimal max);

    @Query("SELECT e FROM Employee e WHERE (:department IS NULL OR e.department = :department) AND (:active IS NULL OR e.active = :active)")
    List<Employee> findByFilters(@Param("department") String department, @Param("active") Boolean active);

    @Modifying
    @Query("UPDATE Employee e SET e.active = false WHERE e.id = :id")
    void softDeleteById(@Param("id") Long id);
}
