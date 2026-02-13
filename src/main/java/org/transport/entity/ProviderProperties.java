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
	@Column(nullable = false)
	private long lastUpdated;
	@Column(nullable = false)
	private double minLat;
	@Column(nullable = false)
	private double maxLat;
	@Column(nullable = false)
	private double minLon;
	@Column(nullable = false)
	private double maxLon;
}
