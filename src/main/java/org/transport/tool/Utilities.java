package org.transport.tool;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Utilities {

	public static String joinNonNull(String separator, @Nullable Object... fields) {
		final List<String> result = new ArrayList<>();
		for (final Object field : fields) {
			if (field != null) {
				final String fieldString = field.toString();
				if (!fieldString.isEmpty()) {
					result.add(fieldString);
				}
			}
		}
		return String.join(separator, result);
	}
}
