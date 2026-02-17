import {inject, Injectable, signal} from "@angular/core";
import * as Leaflet from "leaflet";
import {sortAndTrim} from "../utility/utilities";
import {url} from "../utility/settings";
import {Stop} from "../data/stop";
import {createIcon} from "../utility/stopIcon";
import {ArrivalsService} from "./arrivals.service";
import {ThemeService} from "./theme.service";
import {HttpClient} from "@angular/common/http";
import {Response} from "../data/response";
import {ProviderProperties} from "../data/providerProperties";

const attribution = {attribution: `&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>`};
const lightTiles = Leaflet.tileLayer("https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png", attribution);
const darkTiles = Leaflet.tileLayer("https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png", attribution);

@Injectable({providedIn: "root"})
export class MapService {
	private readonly arrivalsService = inject(ArrivalsService);
	private readonly themeService = inject(ThemeService);
	private readonly httpClient = inject(HttpClient);

	public readonly appDetails = signal<Response<ProviderProperties[]> | undefined>(undefined);

	private map?: Leaflet.Map;
	private markerGroup?: Leaflet.LayerGroup;
	private boxGroup?: Leaflet.LayerGroup;
	private clickStart?: { lat: number, lon: number };
	private timeoutId = 0;

	init() {
		this.httpClient.get<Response<ProviderProperties[]>>(`${url}api/getProviderProperties`).subscribe({
			next: response => {
				this.appDetails.set(response);
				const minLat = Math.min(...response.data.map(data => data.minLat));
				const maxLat = Math.max(...response.data.map(data => data.maxLat));
				const minLon = Math.min(...response.data.map(data => data.minLon));
				const maxLon = Math.max(...response.data.map(data => data.maxLon));
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

	updateMapTheme() {
		if (this.map) {
			this.map.removeLayer(lightTiles);
			this.map.removeLayer(darkTiles);
			(this.themeService.darkTheme() ? darkTiles : lightTiles).addTo(this.map);
		}
	}

	private initMap(lat: number, lon: number, zoom: number) {
		this.map = Leaflet.map("map").setView([lat, lon], zoom);
		this.themeService.themeChanged.subscribe(() => this.updateMapTheme());
		this.updateMapTheme();
		this.markerGroup = Leaflet.layerGroup().addTo(this.map);
		this.boxGroup = Leaflet.layerGroup().addTo(this.map);

		this.map.on("moveend zoomend", () => {
			clearTimeout(this.timeoutId);
			this.timeoutId = setTimeout(() => this.getStops(), 100) as unknown as number;
		});

		this.map.on("mousedown", event => {
			if (event.originalEvent.ctrlKey) {
				this.map?.dragging.disable();
				this.boxGroup?.clearLayers();
				this.clickStart = {lat: event.latlng.lat, lon: event.latlng.lng};
			}
		});
		this.map.on("mousemove", event => {
			if (this.clickStart) {
				this.boxGroup?.clearLayers();
				this.boxGroup?.addLayer(Leaflet.rectangle([[this.clickStart.lat, this.clickStart.lon], [event.latlng.lat, event.latlng.lng]]));
			}
		});
		this.map.on("mouseup", event => {
			if (this.clickStart) {
				this.boxGroup?.clearLayers();
				const rectangle = Leaflet.rectangle([[this.clickStart.lat, this.clickStart.lon], [event.latlng.lat, event.latlng.lng]]);
				const emit = () => this.arrivalsService.stopOrAreaClicked.emit({
					minLat: rectangle.getBounds().getSouthWest().lat,
					maxLat: rectangle.getBounds().getNorthEast().lat,
					minLon: rectangle.getBounds().getSouthWest().lng,
					maxLon: rectangle.getBounds().getNorthEast().lng,
				});
				rectangle.on("click", emit);
				this.boxGroup?.addLayer(rectangle);
				emit();
				this.clickStart = undefined;
			}
			this.map?.dragging.enable();
		});

		this.getStops();
	}

	private getStops() {
		const fullUrl = this.getFullUrl();

		if (fullUrl) {
			this.httpClient.get<Response<Stop[]>>(fullUrl).subscribe(({data}) => {
				if (this.getFullUrl() === fullUrl && this.map && this.markerGroup) {
					const center = this.map.getBounds().getCenter();
					const latOffset = Math.floor((center.lat + 180) / 360) * 360;
					const lonOffset = Math.floor((center.lng + 180) / 360) * 360;
					this.markerGroup.clearLayers();

					data.forEach(stop => {
						const marker = Leaflet.marker([stop.lat + latOffset, stop.lon + lonOffset], {icon: createIcon(stop.providers), riseOnHover: true});
						marker.bindPopup(`
								<div class="column gap-small">
									<div class="column"> 
										<strong>${sortAndTrim(stop.namesTc, 5)}</strong>
										<strong>${sortAndTrim(stop.namesEn, 5)}</strong>
									</div>
									<div>${sortAndTrim(stop.routes, 100)}</div>
								</div>
							`, {closeButton: false});
						marker.on("mouseover", () => marker.openPopup());
						marker.on("mouseout", () => marker.closePopup());
						marker.on("click", () => this.arrivalsService.stopOrAreaClicked.emit(stop));
						this.markerGroup?.addLayer(marker);
					});
				} else {
					console.warn("Skipping request", fullUrl);
				}
			});
		}
	}

	private getFullUrl() {
		if (this.map) {
			const latLngBounds = this.map.getBounds();
			const northEast = latLngBounds.getNorthEast();
			const southWest = latLngBounds.getSouthWest();
			const center = latLngBounds.getCenter();
			const latOffset = Math.floor((center.lat + 180) / 360) * 360;
			const lonOffset = Math.floor((center.lng + 180) / 360) * 360;
			const mergeDistance = 50 / Math.pow(2, this.map.getZoom());
			return `${url}api/getStops?minLat=${southWest.lat - latOffset}&maxLat=${northEast.lat - latOffset}&minLon=${southWest.lng - lonOffset}&maxLon=${northEast.lng - lonOffset}&mergeDistance=${mergeDistance}`;
		} else {
			return undefined;
		}
	}
}
