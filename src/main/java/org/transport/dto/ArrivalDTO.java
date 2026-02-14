package org.transport.dto;

import org.transport.type.Provider;

public record ArrivalDTO(String routeShortName, String routeLongNameEn, String routeLongNameTc, String platform, long arrival, int minutes, boolean realtime, Provider provider) {
}
