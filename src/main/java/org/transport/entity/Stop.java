package org.transport.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.transport.type.Provider;

import java.util.List;

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
	private List<String> routes;
	@Enumerated(EnumType.STRING)
	private Provider provider;
}
