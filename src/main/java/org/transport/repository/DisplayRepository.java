package org.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.transport.entity.Display;

import java.util.List;

public interface DisplayRepository extends JpaRepository<Display, String> {

	void deleteAllByCategory(String category);

	List<Display> findByGroupsContainingIgnoreCase(String group);

	List<Display> findByCategoryAndGroupsContainingIgnoreCase(String category, String group);
}
