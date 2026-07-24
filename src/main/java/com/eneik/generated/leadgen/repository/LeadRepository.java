package com.eneik.generated.leadgen.repository;

import com.eneik.generated.leadgen.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends JpaRepository<Lead, String> {
}
