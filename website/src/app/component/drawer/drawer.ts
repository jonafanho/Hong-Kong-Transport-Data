import {Component, inject} from "@angular/core";
import {DrawerModule} from "primeng/drawer";
import {ArrivalsService} from "../../service/arrivals.service";
import {DividerModule} from "primeng/divider";
import {TranslocoDirective} from "@jsverse/transloco";
import {ProgressBarModule} from "primeng/progressbar";
import {ButtonModule} from "primeng/button";
import {formatAbsoluteTime, sortAndTrim} from "../../utility/utilities";
import {Arrival} from "../../data/arrival";
import {getProviderColor} from "../../utility/stopIcon";
import {TooltipModule} from "primeng/tooltip";
import {ThemeService} from "../../service/theme.service";

@Component({
	selector: "app-drawer",
	imports: [
		DrawerModule,
		DividerModule,
		ProgressBarModule,
		ButtonModule,
		TooltipModule,
		TranslocoDirective,
	],
	templateUrl: "./drawer.html",
	styleUrl: "./drawer.scss",
})
export class DrawerComponent {
	private readonly arrivalsService = inject(ArrivalsService);
	private readonly themeService = inject(ThemeService);

	protected visible = false;

	constructor() {
		this.arrivalsService.stopOrAreaClicked.subscribe(stop => this.visible = !!stop);
	}

	closeDrawer() {
		this.arrivalsService.stopOrAreaClicked.emit();
	}

	getArrivals() {
		return this.arrivalsService.arrivals();
	}

	getRelativeTimes(index: number) {
		return this.arrivalsService.relativeTimes()[index];
	}

	getLoading() {
		return this.arrivalsService.loading();
	}

	getTitles() {
		const stopOrArea = this.getStopOrArea();
		return stopOrArea && "namesTc" in stopOrArea ? [sortAndTrim(stopOrArea.namesTc, 100), sortAndTrim(stopOrArea.namesEn, 100)] : [];
	}

	getSubtitles() {
		const stopOrArea = this.getStopOrArea();
		return stopOrArea ? "routes" in stopOrArea ? [sortAndTrim(stopOrArea.routes, 100)] : [`${stopOrArea.minLat} -> ${stopOrArea.maxLat}`, `${stopOrArea.maxLon} -> ${stopOrArea.maxLon}`] : [];
	}

	getColor(arrival: Arrival) {
		return getProviderColor(arrival.provider, arrival.routeShortName);
	}

	formatDate(arrival: Arrival) {
		return arrival.arrival === 0 ? "" : formatAbsoluteTime(arrival.arrival);
	}

	isDarkTheme() {
		return this.themeService.darkTheme();
	}

	toggleTheme() {
		return this.themeService.setTheme(!this.themeService.darkTheme());
	}

	private getStopOrArea() {
		return this.arrivalsService.stopOrArea();
	}
}
