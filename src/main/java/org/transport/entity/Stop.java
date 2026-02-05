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
public final class Stop {

	@Id
	private String id;
	private String nameEn;
	private String nameTc;
	private double lat;
	private double lon;
	@Enumerated(EnumType.STRING)
	private Provider provider;

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof Stop stop) {
			return id.equals(stop.id);
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
