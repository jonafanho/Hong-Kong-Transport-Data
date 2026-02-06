package org.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.transport.entity.ConsolidationTimestamp;

public interface ConsolidationTimestampRepository extends JpaRepository<ConsolidationTimestamp, Integer> {
}
