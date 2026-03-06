package org.transport.dto;

import java.util.List;

public record DisplayDTO(String category, List<String> groups, int width, int height, byte[] imageBytes) {
}
