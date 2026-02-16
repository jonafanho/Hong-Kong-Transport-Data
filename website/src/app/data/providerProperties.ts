import {Provider} from "./provider";

export interface ProviderProperties {
	readonly minLat: number;
	readonly maxLat: number;
	readonly minLon: number;
	readonly maxLon: number;
	readonly provider: Provider;
	readonly lastUpdated: number;
}
