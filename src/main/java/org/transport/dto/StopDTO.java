package org.transport.dto;

import org.transport.type.Provider;

import java.util.List;

public record StopDTO(List<String> ids, List<String> namesEn, List<String> namesTc, double lat, double lon, List<String> routes, List<Provider> providers) {
}
