import {AfterViewInit, Component} from "@angular/core";

import * as Leaflet from "leaflet";
import {HttpClient} from "@angular/common/http";
import {url} from "../../utility/settings";
import {createIcon} from "../../utility/stopIcon";
import {StopExtension} from "../../data/stopExtension";
import {ArrivalsService} from "../../service/arrivals.service";

@Component({
	selector: "app-map",
	imports: [],
	templateUrl: "./map.html",
	styleUrl: "./map.scss",
})
export class MapComponent implements AfterViewInit {
	private map?: Leaflet.Map;
	private markerGroup?: Leaflet.LayerGroup;
	private timeoutId = 0;

	constructor(private readonly arrivalsService: ArrivalsService, private readonly httpClient: HttpClient) {
	}

	ngAfterViewInit() {
		if (navigator.geolocation) {
			navigator.geolocation.getCurrentPosition(
				({coords}) => this.initMap(coords.latitude, coords.longitude, 13),
				() => this.getApproximateLocation(),
			);
		} else {
			this.getApproximateLocation();
		}
	}

	private getApproximateLocation() {
		this.httpClient.get<{ data: { lat: number, lon: number } }>(`${url}api/centerPoint`).subscribe({
			next: ({data}) => this.initMap(data.lat, data.lon, 10),
			error: () => this.initMap(0, 0, 3),
		});
	}

	private initMap(lat: number, lon: number, zoom: number) {
		this.map = Leaflet.map("map").setView([lat, lon], zoom);
		Leaflet.tileLayer("https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png", {
			attribution: `&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>`,
		}).addTo(this.map);
		this.markerGroup = Leaflet.layerGroup().addTo(this.map);
		this.map.on("moveend zoomend", () => {
			clearTimeout(this.timeoutId);
			this.timeoutId = setTimeout(() => this.getStops(), 100) as unknown as number;
		});
		this.getStops();
	}

	private getStops() {
		if (this.map) {
			const primaryColor = getComputedStyle(document.documentElement).getPropertyValue("--p-primary-color");
			const latLngBounds = this.map.getBounds();
			const center = latLngBounds.getCenter();
			const northEast = latLngBounds.getNorthEast();
			const southWest = latLngBounds.getSouthWest();
			this.httpClient.get<{ data: { list: StopExtension[] } }>(`${url}api/stops-for-location?lat=${center.lat}&lon=${center.lng}&latSpan=${northEast.lat - southWest.lat}&lonSpan=${northEast.lng - southWest.lng}&maxCount=128`).subscribe(({data}) => {
				if (this.markerGroup) {
					this.markerGroup.clearLayers();
					data.list.forEach(stopExtension => {
						if (stopExtension.routes.length > 0) {
							const marker = Leaflet.marker([stopExtension.stop.lat, stopExtension.stop.lon], {icon: createIcon(primaryColor, stopExtension.direction), riseOnHover: true});
							marker.bindPopup(`
								<div class="column gap-small">
									<strong>${stopExtension.stop.name}</strong>
									<div>${stopExtension.routes.join(", ")}</div>
								</div>
							`, {closeButton: false});
							marker.on("mouseover", () => marker.openPopup());
							marker.on("mouseout", () => marker.closePopup());
							marker.on("click", () => this.arrivalsService.stopClicked.emit(stopExtension));
							this.markerGroup?.addLayer(marker);
						}
					});
				}
			});
		}
	}
}
