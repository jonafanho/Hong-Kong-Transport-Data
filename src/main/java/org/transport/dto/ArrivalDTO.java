package org.transport.dto;

import org.transport.type.Provider;

public record ArrivalDTO(String route, String destinationEn, String destinationTc, String platform, long arrival, int minutes, boolean realtime, Provider provider) {
}
