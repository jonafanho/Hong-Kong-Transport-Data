export interface Response<T> {
	readonly currentTime: number;
	readonly version: string;
	readonly data: T;
}
