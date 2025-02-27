package com.sustech.cs307.project2.shenzhenmetro.repository;

import com.sustech.cs307.project2.shenzhenmetro.object.Line;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LineRepository extends JpaRepository<Line, Integer> {
    @Query(value = "SELECT * FROM line ORDER BY LPAD(line_name, 10,0)", nativeQuery = true)
    List<Line> findAllOrderedByName();

    Line findByLineName(String lineName);
}
