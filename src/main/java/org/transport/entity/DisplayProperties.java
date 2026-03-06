package org.transport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class DisplayProperties {

	@Id
	private String category;
	@Column(nullable = false)
	private long lastUpdated;
}
