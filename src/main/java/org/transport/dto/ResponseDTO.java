package org.transport.dto;

import org.transport.Version;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public final class ResponseDTO<T> {

	public final long currentTime = System.currentTimeMillis();
	public final String version = Version.VERSION;
	public final T data;

	public ResponseDTO(T data) {
		this.data = data;
	}

	public static <T> Mono<ResponseDTO<T>> build(Mono<T> mono) {
		return mono.map(ResponseDTO::new);
	}

	public static <T> Mono<ResponseDTO<List<T>>> build(Flux<T> flux) {
		return build(flux.collectList());
	}
}
