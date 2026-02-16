import {AfterViewInit, Component, inject} from "@angular/core";
import {MapService} from "../../service/map.service";

@Component({
	selector: "app-map",
	imports: [],
	templateUrl: "./map.html",
	styleUrl: "./map.scss",
})
export class MapComponent implements AfterViewInit {
	private readonly mapService = inject(MapService);

	ngAfterViewInit() {
		this.mapService.init();
	}
}
