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
		this.httpClient.get<{ minLat: number, maxLat: number, minLon: number, maxLon: number }[]>(`${url}api/getProviderProperties`).subscribe({
			next: data => {
				const minLat = Math.min(...data.map(data => data.minLat));
				const maxLat = Math.max(...data.map(data => data.maxLat));
				const minLon = Math.min(...data.map(data => data.minLon));
				const maxLon = Math.max(...data.map(data => data.maxLon));
				const lat = (minLat + maxLat) / 2;
				const lon = (maxLon + maxLon) / 2;

				if (navigator.geolocation) {
					navigator.geolocation.getCurrentPosition(({coords}) => {
						if (coords.latitude >= minLat - 1 && coords.latitude <= maxLat + 1 && coords.latitude >= minLon - 1 && coords.latitude <= maxLon + 1) {
							this.initMap(coords.latitude, coords.longitude, 13);
						} else {
							this.initMap(lat, lon, 11);
						}
					}, () => this.initMap(lat, lon, 11));
				} else {
					this.initMap(lat, lon, 11);
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
			const latLngBounds = this.map.getBounds();
			const northEast = latLngBounds.getNorthEast();
			const southWest = latLngBounds.getSouthWest();
			const center = latLngBounds.getCenter();
			const latOffset = Math.floor((center.lat + 180) / 360) * 360;
			const lonOffset = Math.floor((center.lng + 180) / 360) * 360;
			const mergeDistance = 50 / Math.pow(2, this.map.getZoom());

			this.httpClient.get<Stop[]>(`${url}api/getStops?minLat=${southWest.lat - latOffset}&maxLat=${northEast.lat - latOffset}&minLon=${southWest.lng - lonOffset}&maxLon=${northEast.lng - lonOffset}&mergeDistance=${mergeDistance}`).subscribe(stops => {
				if (this.markerGroup) {
					this.markerGroup.clearLayers();
					stops.forEach(stop => {
						const marker = Leaflet.marker([stop.lat + latOffset, stop.lon + lonOffset], {icon: createIcon(stop.providers), riseOnHover: true});
						marker.bindPopup(`
								<div class="column gap-small">
									<div class="column"> 
										<strong>${MapComponent.sortAndTrim(stop.namesTc, 5)}</strong>
										<strong>${MapComponent.sortAndTrim(stop.namesEn, 5)}</strong>
									</div>
									<div>${MapComponent.sortAndTrim(stop.routes, 100)}</div>
								</div>
							`, {closeButton: false});
						marker.on("mouseover", () => marker.openPopup());
						marker.on("mouseout", () => marker.closePopup());
						marker.on("click", () => this.arrivalsService.stopClicked.emit(stop));
						this.markerGroup?.addLayer(marker);
					});
				}
			});
		}
	}

	private static sortAndTrim(list: string[], count: number) {
		list.sort((item1, item2) => {
			const item1FirstNumbers = /\d+/.exec(item1);
			const item2FirstNumbers = /\d+/.exec(item2);
			if (item1FirstNumbers && !item2FirstNumbers) {
				return -1;
			} else if (!item1FirstNumbers && item2FirstNumbers) {
				return 1;
			} else {
				const difference = item1FirstNumbers && item2FirstNumbers ? parseInt(item1FirstNumbers[0]) - parseInt(item2FirstNumbers[0]) : 0;
				return difference === 0 ? item1.localeCompare(item2) : difference;
			}
		});
		return list.slice(0, count).join(", ") + (count < list.length ? `... (+${list.length - count})` : "");
	}
}
