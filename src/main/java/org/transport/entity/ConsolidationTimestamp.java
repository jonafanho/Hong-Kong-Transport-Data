package org.transport.entity;

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
public final class ConsolidationTimestamp {

	public static final int ID = 1;

	@Id
	private int id = ID;
	private long lastUpdated;
}
