package org.transport.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.transport.entity.Stop;
import org.transport.type.Provider;

import java.util.List;

public interface StopRepository extends JpaRepository<Stop, String> {

	Slice<Stop> findByLatBetweenAndLonBetween(double minLat, double maxLat, double minLon, double maxLon, Pageable pageable);

	List<Stop> findStopsByProvider(Provider provider);

	void deleteAllByProvider(Provider provider);
}
