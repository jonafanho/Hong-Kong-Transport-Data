package org.transport.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class RouteMapping {

	private String route;
	private String destinationEn;
	private String destinationTc;
}
