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

	public readonly stopClicked = new EventEmitter<Stop | undefined>();
	public readonly stop = signal<Stop | undefined>(undefined);
	public readonly arrivals = signal<Arrival[]>([]);
	public readonly relativeTimes = signal<(string | undefined)[]>([]);
	public readonly loading = signal<boolean>(false);
	private intervalId = -1;

	constructor() {
		this.stopClicked.subscribe(stop => {
			this.stop.set(stop);
			this.arrivals.set([]);
			this.loading.set(!!this.stop);
			this.fetchData();
		});

		setInterval(() => this.updateRelativeTimes(), 100);
	}

	private fetchData() {
		const stopIdsString = this.getStopIdsString();
		if (stopIdsString) {
			clearTimeout(this.intervalId);
			this.httpClient.get<Arrival[]>(`${url}api/getArrivals?stopIds=${stopIdsString}`).subscribe({
				next: arrivals => {
					if (this.getStopIdsString() === stopIdsString) {
						this.arrivals.set(arrivals);
						this.updateRelativeTimes();
						this.loading.set(false);
						clearTimeout(this.intervalId);
						this.intervalId = setTimeout(() => this.fetchData(), fetchInterval) as unknown as number;
					} else {
						console.warn("Skipping request", stopIdsString);
					}
				},
				error: () => {
					if (this.getStopIdsString() === stopIdsString) {
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

	private getStopIdsString() {
		return this.stop()?.ids?.join(",");
	}
}
