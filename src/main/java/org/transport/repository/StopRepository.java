package org.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.transport.entity.Stop;

public interface StopRepository extends JpaRepository<Stop, String> {

}
