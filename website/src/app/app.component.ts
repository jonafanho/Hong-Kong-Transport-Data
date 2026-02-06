import {Component, inject} from "@angular/core";
import {MapComponent} from "./component/map/map";
import {DrawerComponent} from "./component/drawer/drawer";
import {LangService} from "./service/lang.service";

@Component({
	selector: "app-root",
	imports: [
		MapComponent,
		DrawerComponent,
	],
	templateUrl: "./app.component.html",
	styleUrls: ["./app.component.scss"],
})
export class AppComponent {

	constructor() {
		const langService = inject(LangService);
		langService.init();
	}
}
