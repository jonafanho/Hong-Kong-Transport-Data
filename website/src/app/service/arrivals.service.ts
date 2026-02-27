import {EventEmitter, inject, Injectable, signal} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Stop} from "../data/stop";
import {url} from "../utility/settings";
import {Arrival} from "../data/arrival";
import {formatRelativeTime, sortNumbers} from "../utility/utilities";
import {Response} from "../data/response";

const fetchInterval = 3000;

@Injectable({providedIn: "root"})
export class ArrivalsService {
	private readonly httpClient = inject(HttpClient);

	public readonly stopOrAreaClicked = new EventEmitter<Stop | { minLat: number, maxLat: number, minLon: number, maxLon: number } | undefined>();
	public readonly stopOrArea = signal<Stop | { minLat: number, maxLat: number, minLon: number, maxLon: number } | undefined>(undefined);
	public readonly arrivals = signal<Arrival[]>([]);
	public readonly groupedArrivals = signal<Arrival[][]>([]);
	public readonly relativeTimes = signal<Record<string, string>>({});
	public readonly loading = signal<boolean>(false);
	public readonly copyStatus = signal<"none" | "success" | "error">("none");
	private intervalId = -1;
	private timeoutId = -1;

	constructor() {
		this.stopOrAreaClicked.subscribe(stopOrArea => {
			this.stopOrArea.set(stopOrArea);
			this.arrivals.set([]);
			this.groupedArrivals.set([]);
			this.relativeTimes.set({});
			this.loading.set(!!this.stopOrArea);
			this.copyStatus.set("none");
			clearTimeout(this.timeoutId);
			this.fetchData();
		});

		setInterval(() => this.updateRelativeTimes(), 100);
	}

	copyRequest() {
		const fullUrl = this.getFullUrl();
		if (fullUrl) {
			const update = (status: "success" | "error") => {
				this.copyStatus.set(status);
				clearTimeout(this.timeoutId);
				this.timeoutId = setTimeout(() => this.copyStatus.set("none"), 2000);
			};
			navigator.clipboard.writeText(new URL(fullUrl, document.baseURI).href).then(() => update("success"), () => update("error"));
		}
	}

	private fetchData() {
		const fullUrl = this.getFullUrl();
		if (fullUrl) {
			clearTimeout(this.intervalId);
			this.httpClient.get<Response<Arrival[]>>(fullUrl).subscribe({
				next: ({data}) => {
					if (this.getFullUrl() === fullUrl) {
						this.arrivals.set(data);

						const groupedArrivals: Arrival[][] = [];
						data.forEach(arrival => {
							const existingGroup = groupedArrivals.find(existingArrivals => existingArrivals.some(existingArrival => arrival.route === existingArrival.route && (
								ArrivalsService.containsString(arrival.destinationEn, existingArrival.destinationEn) ||
								ArrivalsService.containsString(existingArrival.destinationEn, arrival.destinationEn) ||
								ArrivalsService.containsString(arrival.destinationTc, existingArrival.destinationTc) ||
								ArrivalsService.containsString(existingArrival.destinationTc, arrival.destinationTc)
							)));
							if (existingGroup) {
								existingGroup.push(arrival);
							} else {
								groupedArrivals.push([arrival]);
							}
						});
						const groupedArrivalsList = Object.values(groupedArrivals);
						groupedArrivalsList.sort((arrivals1, arrivals2) => {
							const arrival1 = arrivals1[0];
							const arrival2 = arrivals2[0];
							if (arrival1.platform && arrival2.platform) {
								return sortNumbers(arrival1.platform, arrival2.platform);
							} else if (arrival1.platform && !arrival2.platform) {
								return -1;
							} else if (!arrival1.platform && arrival2.platform) {
								return 1;
							} else {
								const result = sortNumbers(arrival1.route, arrival2.route);
								return result === 0 ? arrival1.destinationEn.localeCompare(arrival2.destinationEn) : result;
							}
						});
						this.groupedArrivals.set(groupedArrivalsList);

						this.updateRelativeTimes();
						this.loading.set(false);
						clearTimeout(this.intervalId);
						this.intervalId = setTimeout(() => this.fetchData(), fetchInterval) as unknown as number;
					} else {
						console.warn("Skipping request", fullUrl);
					}
				},
				error: () => {
					if (this.getFullUrl() === fullUrl) {
						this.loading.set(false);
						clearTimeout(this.intervalId);
						this.intervalId = setTimeout(() => this.fetchData(), fetchInterval) as unknown as number;
					}
				},
			});
		}
	}

	private updateRelativeTimes() {
		const millis = Date.now();
		const relativeTimes: Record<string, string> = {};
		this.arrivals().forEach(arrival => {
			const relativeTime = formatRelativeTime(arrival.arrival - millis);
			if (arrival.arrival > 0 && relativeTime) {
				relativeTimes[arrival.arrival.toString()] = relativeTime;
			}
		});
		this.relativeTimes.set(relativeTimes);
	}

	private getFullUrl() {
		const stopOrArea = this.stopOrArea();
		return stopOrArea ? "ids" in stopOrArea ? `${url}api/getArrivalsByStopIds?stopIds=${stopOrArea?.ids?.join(",")}` : `${url}api/getArrivalsByArea?minLat=${stopOrArea.minLat}&maxLat=${stopOrArea.maxLat}&minLon=${stopOrArea.minLon}&maxLon=${stopOrArea.maxLon}` : undefined;
	}

	private static containsString(text1: string, text2: string) {
		return text1.toLowerCase().replaceAll(/[()（）]/g, " ").split(" ").every(text => text2.toLowerCase().includes(text));
	}
}
