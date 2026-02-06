import {inject, Injectable} from "@angular/core";
import {getCookie} from "../utility/utilities";
import {TranslocoService} from "@jsverse/transloco";

@Injectable({providedIn: "root"})
export class LangService {
	private readonly translocoService = inject(TranslocoService);

	constructor() {
		const lang = getCookie("lang");
		if (this.translocoService.getAvailableLangs().some(langDefinition => langDefinition.toString() === lang)) {
			this.translocoService.setActiveLang(lang);
		}
	}

	init() {
		console.log("Started the language service with these languages:", this.translocoService.getAvailableLangs());
	}
}
