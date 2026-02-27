import {Component, inject} from "@angular/core";
import {DrawerModule} from "primeng/drawer";
import {ArrivalsService} from "../../service/arrivals.service";
import {DividerModule} from "primeng/divider";
import {TranslocoDirective} from "@jsverse/transloco";
import {ProgressBarModule} from "primeng/progressbar";
import {ButtonModule} from "primeng/button";
import {formatAbsoluteTime, getCookie, setCookie, sortAndTrim} from "../../utility/utilities";
import {Arrival} from "../../data/arrival";
import {getProviderColor} from "../../utility/stopIcon";
import {TooltipModule} from "primeng/tooltip";
import {ThemeService} from "../../service/theme.service";
import {FormBuilder, FormControl, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {CheckboxModule} from "primeng/checkbox";
import {CardModule} from "primeng/card";
import {MapService} from "../../service/map.service";
import {DialogModule} from "primeng/dialog";
import {TableModule} from "primeng/table";

@Component({
	selector: "app-drawer",
	imports: [
		DrawerModule,
		DividerModule,
		CardModule,
		ProgressBarModule,
		ButtonModule,
		TooltipModule,
		CheckboxModule,
		DialogModule,
		TableModule,
		TranslocoDirective,
		ReactiveFormsModule,
	],
	templateUrl: "./drawer.html",
	styleUrl: "./drawer.scss",
})
export class DrawerComponent {
	private readonly arrivalsService = inject(ArrivalsService);
	private readonly mapService = inject(MapService);
	private readonly themeService = inject(ThemeService);

	protected drawerVisible = false;
	protected dialogVisible = false;
	protected readonly formGroup: FormGroup;

	constructor() {
		const formBuilder = inject(FormBuilder);

		this.arrivalsService.stopOrAreaClicked.subscribe(stop => this.drawerVisible = !!stop);
		this.formGroup = formBuilder.group({
			groupArrivals: new FormControl(getCookie("group_arrivals") === "true"),
		});

		this.formGroup.valueChanges.subscribe(() => setCookie("group_arrivals", this.formGroup.getRawValue().groupArrivals));
	}

	closeDrawer() {
		this.arrivalsService.stopOrAreaClicked.emit();
	}

	getArrivals() {
		return this.formGroup.getRawValue().groupArrivals ? [] : this.arrivalsService.arrivals();
	}

	getGroupedArrivals() {
		return this.formGroup.getRawValue().groupArrivals ? this.arrivalsService.groupedArrivals() : [];
	}

	getRelativeTimes() {
		return this.arrivalsService.relativeTimes();
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
		return getProviderColor(arrival.provider, arrival.route);
	}

	formatDate(arrival: Arrival) {
		return arrival.arrival === 0 ? "" : formatAbsoluteTime(arrival.arrival);
	}

	copyRequest() {
		this.arrivalsService.copyRequest();
	}

	copyStatus() {
		return this.arrivalsService.copyStatus();
	}

	openDocumentation() {
		return window.open("https://github.com/jonafanho/Hong-Kong-Transport-Data/blob/master/README.md", "_blank");
	}

	isDarkTheme() {
		return this.themeService.darkTheme();
	}

	toggleTheme() {
		return this.themeService.setTheme(!this.themeService.darkTheme());
	}

	getAppDetails() {
		return this.mapService.appDetails();
	}

	formatAbsoluteTime(millis: number) {
		return formatAbsoluteTime(millis, true);
	}

	private getStopOrArea() {
		return this.arrivalsService.stopOrArea();
	}
}
