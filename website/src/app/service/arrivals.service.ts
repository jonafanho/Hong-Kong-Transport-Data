import {EventEmitter, inject, Injectable, signal} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Stop} from "../data/stop";
import {url} from "../utility/settings";
import {Arrival} from "../data/arrival";
import {formatRelativeTime, sortNumbers} from "../utility/utilities";

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
	private intervalId = -1;

	constructor() {
		this.stopOrAreaClicked.subscribe(stopOrArea => {
			this.stopOrArea.set(stopOrArea);
			this.arrivals.set([]);
			this.groupedArrivals.set([]);
			this.relativeTimes.set({});
			this.loading.set(!!this.stopOrArea);
			this.fetchData();
		});

		setInterval(() => this.updateRelativeTimes(), 100);
	}

	private fetchData() {
		const urlPart = this.getUrlPart();
		if (urlPart) {
			clearTimeout(this.intervalId);
			this.httpClient.get<Arrival[]>(`${url}api/${urlPart}`).subscribe({
				next: arrivals => {
					if (this.getUrlPart() === urlPart) {
						this.arrivals.set(arrivals);

						const groupedArrivals: Arrival[][] = [];
						arrivals.forEach(arrival => {
							const existingGroup = groupedArrivals.find(existingArrivals => existingArrivals.some(existingArrival => arrival.routeShortName === existingArrival.routeShortName && (
								ArrivalsService.containsString(arrival.routeLongNameEn, existingArrival.routeLongNameEn) ||
								ArrivalsService.containsString(existingArrival.routeLongNameEn, arrival.routeLongNameEn) ||
								ArrivalsService.containsString(arrival.routeLongNameTc, existingArrival.routeLongNameTc) ||
								ArrivalsService.containsString(existingArrival.routeLongNameTc, arrival.routeLongNameTc)
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
								const result = sortNumbers(arrival1.routeShortName, arrival2.routeShortName);
								return result === 0 ? arrival1.routeLongNameEn.localeCompare(arrival2.routeLongNameEn) : result;
							}
						});
						this.groupedArrivals.set(groupedArrivalsList);

						this.updateRelativeTimes();
						this.loading.set(false);
						clearTimeout(this.intervalId);
						this.intervalId = setTimeout(() => this.fetchData(), fetchInterval) as unknown as number;
					} else {
						console.warn("Skipping request", urlPart);
					}
				},
				error: () => {
					if (this.getUrlPart() === urlPart) {
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

	private getUrlPart() {
		const stopOrArea = this.stopOrArea();
		return stopOrArea ? "ids" in stopOrArea ? `getArrivalsByStopIds?stopIds=${stopOrArea?.ids?.join(",")}` : `getArrivalsByArea?minLat=${stopOrArea.minLat}&maxLat=${stopOrArea.maxLat}&minLon=${stopOrArea.minLon}&maxLon=${stopOrArea.maxLon}` : undefined;
	}

	private static containsString(text1: string, text2: string) {
		return text1.toLowerCase().replaceAll(/[()（）]/g, " ").split(" ").every(text => text2.toLowerCase().includes(text));
	}
}
