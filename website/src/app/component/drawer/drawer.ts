import {Component, inject} from "@angular/core";
import {DrawerModule} from "primeng/drawer";
import {ArrivalsService} from "../../service/arrivals.service";
import {DividerModule} from "primeng/divider";
import {TranslocoDirective} from "@jsverse/transloco";
import {ProgressBarModule} from "primeng/progressbar";
import {ButtonModule} from "primeng/button";
import {MINUTES_PER_HOUR} from "../../utility/constants";

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

	getName() {
		return this.arrivalsService.getName();
	}

	getRouteList() {
		return this.arrivalsService.getRoutes().join(", ");
	}

	getId() {
		return this.arrivalsService.getId();
	}

	getArrivals() {
		return this.arrivalsService.getArrivals();
	}

	getLoading() {
		return this.arrivalsService.getLoading();
	}

	getAddMinutesAfterLoading() {
		return this.arrivalsService.getAddMinutesAfterLoading();
	}

	addMinutesAfter() {
		this.arrivalsService.addMinutesAfter();
	}

	getHoursAfter() {
		return Math.floor(this.arrivalsService.getMinutesAfter() / MINUTES_PER_HOUR);
	}
}
