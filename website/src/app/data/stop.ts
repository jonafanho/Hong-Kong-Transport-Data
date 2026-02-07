export interface Stop {
	readonly ids: string[];
	readonly namesEn: string[];
	readonly namesTc: string[];
	readonly lat: number;
	readonly lon: number;
	readonly routes: string[];
	readonly providers: string[];
}
