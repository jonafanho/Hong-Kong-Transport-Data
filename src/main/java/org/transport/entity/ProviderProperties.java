package org.transport.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.transport.type.Provider;

@Entity
@Table
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class ProviderProperties {

	@Id
	@Enumerated(EnumType.STRING)
	private Provider provider;
	private long lastUpdated;
	private double minLat;
	private double maxLat;
	private double minLon;
	private double maxLon;
}
