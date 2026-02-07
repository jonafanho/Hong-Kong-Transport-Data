package org.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.transport.entity.ProviderProperties;
import org.transport.type.Provider;

public interface ProviderPropertiesRepository extends JpaRepository<ProviderProperties, Provider> {
}
