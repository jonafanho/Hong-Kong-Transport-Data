export interface Stop {
	readonly id: string;
	readonly nameEn: string;
	readonly nameTc: string;
	readonly lat: number;
	readonly lon: number;
	readonly routes: string[];
	readonly provider: string;
}
