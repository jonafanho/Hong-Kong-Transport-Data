package org.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.transport.entity.Display;

public interface DisplayRepository extends JpaRepository<Display, String> {

	void deleteAllByCategory(String category);
}
