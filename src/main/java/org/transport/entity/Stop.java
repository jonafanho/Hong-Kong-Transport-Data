package org.transport.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.transport.type.Provider;

import java.util.List;
import java.util.Map;

@Entity
@Table
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class Stop {

	@Id
	private String id;
	@Column(nullable = false)
	private String nameEn;
	@Column(nullable = false)
	private String nameTc;
	@Column(nullable = false)
	private double lat;
	@Column(nullable = false)
	private double lon;
	@Column(nullable = false)
	private List<String> routes;
	@JdbcTypeCode(SqlTypes.JSON)
	@Nullable
	private Map<String, RouteMapping> routeIdMapping;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Provider provider;
}
