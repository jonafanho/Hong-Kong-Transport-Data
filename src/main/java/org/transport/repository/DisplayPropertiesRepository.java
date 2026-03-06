package org.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.transport.entity.DisplayProperties;

public interface DisplayPropertiesRepository extends JpaRepository<DisplayProperties, String> {
}
