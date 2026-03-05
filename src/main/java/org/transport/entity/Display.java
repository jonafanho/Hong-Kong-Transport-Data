package org.transport.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.transport.service.DisplayService.RawDisplayDTO;

import java.util.List;
import java.util.UUID;

@Entity
@Table
@Getter
@NoArgsConstructor
public final class Display {

	@Id
	@GeneratedValue
	@Nullable
	private UUID id;
	@Column(nullable = false)
	private String category;
	@Column(nullable = false)
	private List<String> groups;
	@Column(nullable = false)
	private int width;
	@Column(nullable = false)
	private int height;
	@Lob
	@Column(nullable = false)
	private byte[] imageBytes;

	public Display(String category, List<String> groups, RawDisplayDTO rawDisplay) {
		id = null;
		this.category = category;
		this.groups = groups;
		width = rawDisplay.width();
		height = rawDisplay.height();
		imageBytes = rawDisplay.imageBytes();
	}
}
