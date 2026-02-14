import {Provider} from "./provider";

export interface Arrival {
	readonly routeShortName: string;
	readonly routeLongNameEn: string;
	readonly routeLongNameTc: string;
	readonly platform: string;
	readonly arrival: number;
	readonly minutes: number;
	readonly realtime: boolean;
	readonly provider: Provider;
}
