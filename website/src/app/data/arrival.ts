import {Provider} from "./provider";

export interface Arrival {
	readonly route: string;
	readonly destinationEn: string;
	readonly destinationTc: string;
	readonly platform: string;
	readonly arrival: number;
	readonly minutes: number;
	readonly realtime: boolean;
	readonly provider: Provider;
}
