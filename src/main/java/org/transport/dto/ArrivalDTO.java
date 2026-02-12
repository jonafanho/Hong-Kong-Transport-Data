package org.transport.dto;

import org.transport.type.Provider;

public record ArrivalDTO(String routeShortName, String routeLongNameEn, String routeLongNameTc, long arrival, boolean realtime, Provider provider) {
}
