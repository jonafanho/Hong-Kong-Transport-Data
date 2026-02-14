import {EventEmitter, inject, Injectable, signal} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Stop} from "../data/stop";
import {url} from "../utility/settings";
import {Arrival} from "../data/arrival";
import {formatRelativeTime} from "../utility/utilities";

const fetchInterval = 3000;

@Injectable({providedIn: "root"})
export class ArrivalsService {
	private readonly httpClient = inject(HttpClient);

	public readonly stopOrAreaClicked = new EventEmitter<Stop | { minLat: number, maxLat: number, minLon: number, maxLon: number } | undefined>();
	public readonly stopOrArea = signal<Stop | { minLat: number, maxLat: number, minLon: number, maxLon: number } | undefined>(undefined);
	public readonly arrivals = signal<Arrival[]>([]);
	public readonly relativeTimes = signal<(string | undefined)[]>([]);
	public readonly loading = signal<boolean>(false);
	private intervalId = -1;

	constructor() {
		this.stopOrAreaClicked.subscribe(stopOrArea => {
			this.stopOrArea.set(stopOrArea);
			this.arrivals.set([]);
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
		this.relativeTimes.set(this.arrivals().map(arrival => arrival.arrival === 0 ? undefined : formatRelativeTime(arrival.arrival - millis)));
	}

	private getUrlPart() {
		const stopOrArea = this.stopOrArea();
		return stopOrArea ? "ids" in stopOrArea ? `getArrivalsByStopIds?stopIds=${stopOrArea?.ids?.join(",")}` : `getArrivalsByArea?minLat=${stopOrArea.minLat}&maxLat=${stopOrArea.maxLat}&minLon=${stopOrArea.minLon}&maxLon=${stopOrArea.maxLon}` : undefined;
	}
}
