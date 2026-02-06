import {AfterViewInit, Component, inject} from "@angular/core";

import * as Leaflet from "leaflet";
import {HttpClient} from "@angular/common/http";
import {url} from "../../utility/settings";
import {createIcon} from "../../utility/stopIcon";
import {Stop} from "../../data/stop";
import {ArrivalsService} from "../../service/arrivals.service";

@Component({
	selector: "app-map",
	imports: [],
	templateUrl: "./map.html",
	styleUrl: "./map.scss",
})
export class MapComponent implements AfterViewInit {
	private readonly arrivalsService = inject(ArrivalsService);
	private readonly httpClient = inject(HttpClient);

	private map?: Leaflet.Map;
	private markerGroup?: Leaflet.LayerGroup;
	private timeoutId = 0;

	ngAfterViewInit() {
		this.httpClient.get<{ lat: number, lon: number }>(`${url}api/centerPoint`).subscribe({
			next: ({lat, lon}) => {
				if (navigator.geolocation) {
					navigator.geolocation.getCurrentPosition(({coords}) => {
						if (Math.abs(coords.latitude - lat) >= 2 || Math.abs(coords.longitude - lat) >= 2) {
							this.initMap(lat, lon, 10);
						} else {
							this.initMap(coords.latitude, coords.longitude, 13);
						}
					}, () => this.initMap(lat, lon, 10));
				} else {
					this.initMap(lat, lon, 10);
				}
			},
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
			const northEast = latLngBounds.getNorthEast();
			const southWest = latLngBounds.getSouthWest();
			this.httpClient.get<Stop[]>(`${url}api/getStops?minLat=${southWest.lat}&maxLat=${northEast.lat}&minLon=${southWest.lng}&maxLon=${northEast.lng}&maxCount=64`).subscribe(stops => {
				if (this.markerGroup) {
					this.markerGroup.clearLayers();
					stops.forEach(stop => {
						if (stop.routes.length > 0) {
							const marker = Leaflet.marker([stop.lat, stop.lon], {icon: createIcon(primaryColor), riseOnHover: true});
							marker.bindPopup(`
								<div class="column gap-small">
									<strong>${stop.nameTc} ${stop.nameEn}</strong>
									<div>${stop.routes.join(", ")}</div>
								</div>
							`, {closeButton: false});
							marker.on("mouseover", () => marker.openPopup());
							marker.on("mouseout", () => marker.closePopup());
							marker.on("click", () => this.arrivalsService.stopClicked.emit(stop));
							this.markerGroup?.addLayer(marker);
						}
					});
				}
			});
		}
	}
}
