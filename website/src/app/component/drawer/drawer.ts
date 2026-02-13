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

@Component({
	selector: "app-drawer",
	imports: [
		DrawerModule,
		DividerModule,
		ProgressBarModule,
		ButtonModule,
		TranslocoDirective,
	],
	templateUrl: "./drawer.html",
	styleUrl: "./drawer.scss",
})
export class DrawerComponent {
	private readonly arrivalsService = inject(ArrivalsService);

	protected visible = false;

	constructor() {
		this.arrivalsService.stopClicked.subscribe(stop => this.visible = !!stop);
	}

	closeDrawer() {
		this.arrivalsService.stopClicked.emit();
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

	getNames() {
		const stop = this.getStop();
		return stop ? [sortAndTrim(stop.namesTc, 100), sortAndTrim(stop.namesEn, 100)] : [];
	}

	getRoutes() {
		const stop = this.getStop();
		return stop ? sortAndTrim(stop.routes, 100) : "";
	}

	getColor(arrival: Arrival) {
		return getProviderColor(arrival.provider, arrival.routeShortName);
	}

	formatDate(arrival: Arrival) {
		return arrival.arrival === 0 ? "" : formatAbsoluteTime(arrival.arrival);
	}

	private getStop() {
		return this.arrivalsService.stop();
	}
}
