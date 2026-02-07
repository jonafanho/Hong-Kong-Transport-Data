import * as Leaflet from "leaflet";

const iconSize = 16;
const iconShadowRadius = iconSize / 4;

const mtrLogo = `
	<svg width="${iconSize}" height="${iconSize}">
		<ellipse cx="${iconSize / 2}" cy="${iconSize / 2}" rx="${iconSize / 2}" ry="${iconSize * 13 / 32}" fill="#AC2E44"/>
		<path d="
			M ${iconSize * 32 / 64} ${iconSize * 8 / 64 + iconSize * 6 / 64}
			V ${iconSize * 44 / 64 + iconSize * 6 / 64}
			M ${iconSize * 20 / 64} ${iconSize * 9 / 64 + iconSize * 6 / 64}
			A ${iconSize * 12 / 64} ${iconSize * 12 / 64} 0 0 0 ${iconSize * 44 / 64} ${iconSize * 9 / 64 + iconSize * 6 / 64}
			M ${iconSize * 20 / 64} ${iconSize * 43 / 64 + iconSize * 6 / 64}
			A ${iconSize * 12 / 64} ${iconSize * 12 / 64} 0 0 1 ${iconSize * 44 / 64} ${iconSize * 43 / 64 + iconSize * 6 / 64}
		" stroke="white" stroke-width="${iconSize * 5 / 64}" fill="none"/>
	</svg>
`;
const lrtLogo = `
	<svg width="${iconSize}" height="${iconSize}">
		<rect fill="#CDA410" width="${iconSize}" height="${iconSize}"/>
		<path fill="#FFFFFF" d="
			M ${iconSize * 322.9 / 500} ${iconSize * 448.6 / 500}
			l ${iconSize * 18.4 / 500} ${iconSize * 36.3 / 500}
			h ${iconSize * 24.9 / 500}
			l ${iconSize * -29.5 / 500} ${iconSize * -36.3 / 500}
			H ${iconSize * 322.9 / 500}
			Z
			M ${iconSize * 203.3 / 500} ${iconSize * 437.6 / 500}
			h ${iconSize * -1.8 / 500}
			c ${iconSize * 3.5 / 500} 0 ${iconSize * 5.6 / 500} 0 ${iconSize * 5.6 / 500} 0
			l 0 0
			h ${iconSize * 88.9 / 500}
			v ${iconSize * -15.6 / 500}
			h ${iconSize * -92.6 / 500}
			V ${iconSize * 437.6 / 500}
			Z
			M ${iconSize * 132.2 / 500} ${iconSize * 485 / 500}
			h ${iconSize * 24.9 / 500}
			l ${iconSize * 19.7 / 500} ${iconSize * -36.3 / 500}
			h ${iconSize * -13.8 / 500}
			L ${iconSize * 132.2 / 500} ${iconSize * 485 / 500}
			Z
			M ${iconSize * 390.4 / 500} ${iconSize * 129.8 / 500}
			c 0 ${iconSize * -24.4 / 500} ${iconSize * -27.4 / 500} ${iconSize * -34.1 / 500} ${iconSize * -27.4 / 500} ${iconSize * -34.1 / 500}
			l ${iconSize * -7 / 500} ${iconSize * -18.3 / 500}
			l ${iconSize * -108 / 500} ${iconSize * -0.1 / 500}
			l ${iconSize * -0.3 / 500} ${iconSize * 0.3 / 500}
			l ${iconSize * -2 / 500} ${iconSize * -0.1 / 500}
			l ${iconSize * -2 / 500} ${iconSize * 0.1 / 500}
			l ${iconSize * -0.3 / 500} ${iconSize * -0.3 / 500}
			l ${iconSize * -100.6 / 500} ${iconSize * -0.1 / 500}
			l ${iconSize * -6.3 / 500} ${iconSize * 18.4 / 500}
			c 0 0 ${iconSize * -15 / 500} ${iconSize * 9.3 / 500} ${iconSize * -27.2 / 500} ${iconSize * 34.2 / 500}
			c 0 0 ${iconSize * 0.3 / 500} ${iconSize * 252.7 / 500} ${iconSize * 0.3 / 500} ${iconSize * 265.4 / 500}
			c 0 ${iconSize / 500} ${iconSize * 0.2 / 500} ${iconSize * 2.2 / 500} ${iconSize * 0.5 / 500} ${iconSize * 3.4 / 500}
			c ${iconSize * 1.8 / 500} ${iconSize * 18.5 / 500} ${iconSize * 15.5 / 500} ${iconSize * 33.9 / 500} ${iconSize * 33.5 / 500} ${iconSize * 37.1 / 500}
			c ${iconSize * 2.7 / 500} ${iconSize * 1.1 / 500} ${iconSize * 5.3 / 500} ${iconSize * 1.7 / 500} ${iconSize * 7.8 / 500} ${iconSize * 1.7 / 500}
			c ${iconSize * 13.7 / 500} 0 ${iconSize * 27 / 500} 0 ${iconSize * 36 / 500} ${iconSize * 0.1 / 500}
			v ${iconSize * -30.7 / 500}
			h ${iconSize * 125.3 / 500}
			v ${iconSize * 30.3 / 500}
			h ${iconSize * 16.3 / 500}
			l 0 ${iconSize * 0.7 / 500}
			c 0 0 ${iconSize * 1.1 / 500} ${iconSize * -0.4 / 500} ${iconSize * 21.8 / 500} ${iconSize * -0.4 / 500}
			c ${iconSize * 4.1 / 500} 0 ${iconSize * 8.2 / 500} ${iconSize * -1.5 / 500} ${iconSize * 12 / 500} ${iconSize * -3.9 / 500}
			c ${iconSize * 13.7 / 500} ${iconSize * -5.3 / 500} ${iconSize * 24.7 / 500} ${iconSize * -18.3 / 500} ${iconSize * 26.7 / 500} ${iconSize * -33.4 / 500}
			c ${iconSize * 0.9 / 500} ${iconSize * -2.3 / 500} ${iconSize * 1.5 / 500} ${iconSize * -4 / 500} ${iconSize * 1.5 / 500} ${iconSize * -4.9 / 500}
			C ${iconSize * 390.9 / 500} ${iconSize * 376.8 / 500} ${iconSize * 390.4 / 500} ${iconSize * 129.8 / 500} ${iconSize * 390.4 / 500} ${iconSize * 129.8 / 500}
			Z
			M ${iconSize * 132.6 / 500} ${iconSize * 313.3 / 500}
			c 0 0 ${iconSize * -7.7 / 500} ${iconSize * -5.5 / 500} ${iconSize * -7.7 / 500} ${iconSize * -15.6 / 500}
			l ${iconSize * -0.1 / 500} ${iconSize * -124.8 / 500}
			c 0 ${iconSize * -13.7 / 500} ${iconSize * 7.9 / 500} ${iconSize * -15.7 / 500} ${iconSize * 7.9 / 500} ${iconSize * -15.7 / 500}
			c ${iconSize * 8.6 / 500} 0 ${iconSize * 7.8 / 500} ${iconSize * 7 / 500} ${iconSize * 7.8 / 500} ${iconSize * 15.6 / 500}
			v ${iconSize * 132.7 / 500}
			C ${iconSize * 140.5 / 500} ${iconSize * 314.1 / 500} ${iconSize * 141.2 / 500} ${iconSize * 313.3 / 500} ${iconSize * 132.6 / 500} ${iconSize * 313.3 / 500}
			Z
			M ${iconSize * 202.8 / 500} ${iconSize * 357.4 / 500}
			v ${iconSize * 10.4 / 500}
			c 0 ${iconSize * 3.8 / 500} ${iconSize * -1.4 / 500} ${iconSize * 6.9 / 500} ${iconSize * -5.2 / 500} ${iconSize * 6.9 / 500}
			h ${iconSize * -23.2 / 500}
			c ${iconSize * -0.6 / 500} ${iconSize * 0.1 / 500} ${iconSize * -1.2 / 500} ${iconSize * 0.2 / 500} ${iconSize * -1.8 / 500} ${iconSize * 0.2 / 500}
			c ${iconSize * -0.6 / 500} 0 ${iconSize * -1.1 / 500} ${iconSize * -0.1 / 500} ${iconSize * -1.7 / 500} ${iconSize * -0.2 / 500}
			h ${iconSize * -0.5 / 500}
			c ${iconSize * -0.1 / 500} 0 ${iconSize * -0.2 / 500} ${iconSize * -0.1 / 500} ${iconSize * -0.3 / 500} ${iconSize * -0.1 / 500}
			c ${iconSize * -0.4 / 500} ${iconSize * -0.1 / 500} ${iconSize * -0.7 / 500} ${iconSize * -0.2 / 500} ${iconSize * -1 / 500} ${iconSize * -0.3 / 500}
			c ${iconSize * -0.1 / 500} 0 ${iconSize * -0.2 / 500} 0 ${iconSize * -0.3 / 500} ${iconSize * -0.1 / 500}
			c ${iconSize * -5.6 / 500} ${iconSize * -1.7 / 500} ${iconSize * -13.3 / 500} ${iconSize * -10.7 / 500} ${iconSize * -13.3 / 500} ${iconSize * -16.9 / 500}
			c 0 ${iconSize * -0.2 / 500} ${iconSize * 0.1 / 500} ${iconSize * -6.5 / 500} ${iconSize * 0.1 / 500} ${iconSize * -6.7 / 500}
			v ${iconSize * 6.8 / 500}
			c 0 ${iconSize * -3.8 / 500} ${iconSize * 1.6 / 500} ${iconSize * -13 / 500} ${iconSize * 5.4 / 500} ${iconSize * -13 / 500}
			h ${iconSize * 7.6 / 500}
			c ${iconSize * 0.2 / 500} 0 ${iconSize * 0.4 / 500} ${iconSize * -0.1 / 500} ${iconSize * 0.6 / 500} ${iconSize * -0.1 / 500}
			c ${iconSize * 0.2 / 500} 0 ${iconSize * 0.4 / 500} ${iconSize * 0.1 / 500} ${iconSize * 0.6 / 500} ${iconSize * 0.1 / 500}
			H ${iconSize * 201 / 500}
			C ${iconSize * 204.9 / 500} ${iconSize * 344.4 / 500} ${iconSize * 202.8 / 500} ${iconSize * 353.5 / 500} ${iconSize * 202.8 / 500} ${iconSize * 357.4 / 500}
			Z
			M ${iconSize * 331 / 500} ${iconSize * 374.2 / 500}
			c ${iconSize * -0.1 / 500} 0 ${iconSize * -0.2 / 500} 0 ${iconSize * -0.3 / 500} ${iconSize * 0.1 / 500}
			c ${iconSize * -0.3 / 500} ${iconSize * 0.1 / 500} ${iconSize * -0.7 / 500} ${iconSize * 0.2 / 500} ${iconSize * -1 / 500} ${iconSize * 0.3 / 500}
			c ${iconSize * -0.1 / 500} 0 ${iconSize * -0.2 / 500} ${iconSize * 0.1 / 500} ${iconSize * -0.3 / 500} ${iconSize * 0.1 / 500}
			h ${iconSize * -0.5 / 500}
			c ${iconSize * -0.6 / 500} ${iconSize * 0.1 / 500} ${iconSize * -1.1 / 500} ${iconSize * 0.2 / 500} ${iconSize * -1.7 / 500} ${iconSize * 0.2 / 500}
			c ${iconSize * -0.6 / 500} 0 ${iconSize * -1.2 / 500} ${iconSize * -0.1 / 500} ${iconSize * -1.8 / 500} ${iconSize * -0.2 / 500}
			h ${iconSize * -23.2 / 500}
			c ${iconSize * -3.8 / 500} 0 ${iconSize * -5.2 / 500} ${iconSize * -3.1 / 500} ${iconSize * -5.2 / 500} ${iconSize * -6.9 / 500}
			v ${iconSize * -10.4 / 500}
			c 0 ${iconSize * -3.8 / 500} ${iconSize * -2.1 / 500} ${iconSize * -13 / 500} ${iconSize * 1.7 / 500} ${iconSize * -13 / 500}
			h ${iconSize * 31.3 / 500}
			c ${iconSize * 0.2 / 500} 0 ${iconSize * 0.4 / 500} ${iconSize * -0.1 / 500} ${iconSize * 0.6 / 500} ${iconSize * -0.1 / 500}
			c ${iconSize * 0.2 / 500} 0 ${iconSize * 0.4 / 500} ${iconSize * 0.1 / 500} ${iconSize * 0.6 / 500} ${iconSize * 0.1 / 500}
			h ${iconSize * 7.6 / 500}
			c ${iconSize * 3.8 / 500} 0 ${iconSize * 5.4 / 500} ${iconSize * 9.1 / 500} ${iconSize * 5.4 / 500} ${iconSize * 13 / 500}
			v ${iconSize * -6.8 / 500}
			c 0 ${iconSize * 0.2 / 500} ${iconSize * 0.1 / 500} ${iconSize * 6.5 / 500} ${iconSize * 0.1 / 500} ${iconSize * 6.7 / 500}
			C ${iconSize * 344.3 / 500} ${iconSize * 363.5 / 500} ${iconSize * 336.7 / 500} ${iconSize * 372.5 / 500} ${iconSize * 331 / 500} ${iconSize * 374.2 / 500}
			Z
			M ${iconSize * 344.7 / 500} ${iconSize * 155.8 / 500}
			c 0 0 0 ${iconSize * 0.1 / 500} 0 ${iconSize * 0.2 / 500}
			v ${iconSize * 140.6 / 500}
			c 0 ${iconSize * 0.1 / 500} 0 ${iconSize * 0.1 / 500} 0 ${iconSize * 0.2 / 500}
			v ${iconSize * 7.8 / 500}
			c 0 ${iconSize * 4.3 / 500} ${iconSize * -3.5 / 500} ${iconSize * 7.8 / 500} ${iconSize * -7.8 / 500} ${iconSize * 7.8 / 500}
			h ${iconSize * -15.6 / 500}
			c 0 0 0 0 0 0
			h ${iconSize * -62.2 / 500}
			c ${iconSize * -0.6 / 500} 0 ${iconSize * -1.1 / 500} ${iconSize * -0.1 / 500} ${iconSize * -1.7 / 500} ${iconSize * -0.2 / 500}
			c ${iconSize * -0.6 / 500} ${iconSize * 0.1 / 500} ${iconSize * -1.1 / 500} ${iconSize * 0.2 / 500} ${iconSize * -1.7 / 500} ${iconSize * 0.2 / 500}
			h ${iconSize * -91.9 / 500}
			c ${iconSize * -4.3 / 500} 0 ${iconSize * -7.8 / 500} ${iconSize * -3.5 / 500} ${iconSize * -7.8 / 500} ${iconSize * -7.8 / 500}
			v ${iconSize * -6.1 / 500}
			c ${iconSize * -0.1 / 500} ${iconSize * -0.6 / 500} ${iconSize * -0.2 / 500} ${iconSize * -1.1 / 500} ${iconSize * -0.2 / 500} ${iconSize * -1.7 / 500}
			v ${iconSize * -141 / 500}
			c 0 ${iconSize * -0.6 / 500} ${iconSize * 0.1 / 500} ${iconSize * -1.1 / 500} ${iconSize * 0.2 / 500} ${iconSize * -1.7 / 500}
			v ${iconSize * -6 / 500}
			c 0 ${iconSize * -4.3 / 500} ${iconSize * 3.5 / 500} ${iconSize * -7.8 / 500} ${iconSize * 7.8 / 500} ${iconSize * -7.8 / 500}
			h ${iconSize * 6.5 / 500}
			c ${iconSize * 0.4 / 500} 0 ${iconSize * 0.7 / 500} ${iconSize * -0.1 / 500} ${iconSize * 1.1 / 500} ${iconSize * -0.1 / 500}
			h ${iconSize * 84.3 / 500}
			c ${iconSize * 0.4 / 500} 0 ${iconSize * 0.7 / 500} ${iconSize * 0.1 / 500} ${iconSize * 1.1 / 500} ${iconSize * 0.1 / 500}
			h ${iconSize * 1.1 / 500}
			c ${iconSize * 0.4 / 500} 0 ${iconSize * 0.7 / 500} ${iconSize * -0.1 / 500} ${iconSize * 1.1 / 500} ${iconSize * -0.1 / 500}
			h ${iconSize * 70 / 500}
			c ${iconSize * 0.4 / 500} 0 ${iconSize * 0.8 / 500} ${iconSize * 0.1 / 500} ${iconSize * 1.1 / 500} ${iconSize * 0.1 / 500}
			h ${iconSize * 6.7 / 500}
			c ${iconSize * 4.3 / 500} 0 ${iconSize * 7.8 / 500} ${iconSize * 3.5 / 500} ${iconSize * 7.8 / 500} ${iconSize * 7.8 / 500}
			L ${iconSize * 344.7 / 500} ${iconSize * 155.8 / 500}
			L ${iconSize * 344.7 / 500} ${iconSize * 155.8 / 500}
			Z
			M ${iconSize * 374.9 / 500} ${iconSize * 297.7 / 500}
			c 0 ${iconSize * 10 / 500} ${iconSize * -8.2 / 500} ${iconSize * 15.6 / 500} ${iconSize * -8.2 / 500} ${iconSize * 15.6 / 500}
			c ${iconSize * -8.6 / 500} 0 ${iconSize * -8 / 500} ${iconSize * 0.8 / 500} ${iconSize * -8 / 500} ${iconSize * -7.8 / 500}
			V ${iconSize * 172.7 / 500}
			c 0 ${iconSize * -8.6 / 500} ${iconSize * -0.8 / 500} ${iconSize * -15.6 / 500} ${iconSize * 7.8 / 500} ${iconSize * -15.6 / 500}
			c 0 0 ${iconSize * 8.4 / 500} ${iconSize * 2 / 500} ${iconSize * 8.4 / 500} ${iconSize * 15.7 / 500}
			L ${iconSize * 374.9 / 500} ${iconSize * 297.7 / 500}
			Z
			M ${iconSize * 297.2 / 500} ${iconSize * 30.4 / 500}
			c 0 0 ${iconSize * 12.2 / 500} ${iconSize * -1.1 / 500} ${iconSize * 18.3 / 500} ${iconSize * 7.5 / 500}
			c ${iconSize * 6.2 / 500} ${iconSize * 8.6 / 500} ${iconSize * 2.5 / 500} ${iconSize * -22.9 / 500} ${iconSize * -11.1 / 500} ${iconSize * -22.9 / 500}
			H ${iconSize * 189.2 / 500}
			c ${iconSize * -13.6 / 500} 0 ${iconSize * -17.3 / 500} ${iconSize * 31.5 / 500} ${iconSize * -11.1 / 500} ${iconSize * 22.9 / 500}
			c ${iconSize * 6.2 / 500} ${iconSize * -8.6 / 500} ${iconSize * 16.9 / 500} ${iconSize * -7.5 / 500} ${iconSize * 16.9 / 500} ${iconSize * -7.5 / 500}
			l ${iconSize * 48.2 / 500} ${iconSize * 46.9 / 500}
			l ${iconSize * 4.8 / 500} 0
			L ${iconSize * 297.2 / 500} ${iconSize * 30.4 / 500}
			Z
			M ${iconSize * 245.6 / 500} ${iconSize * 65.9 / 500}
			l ${iconSize * -0.8 / 500} ${iconSize * -0.9 / 500}
			v ${iconSize * 0.2 / 500}
			l ${iconSize * -36.5 / 500} ${iconSize * -34.8 / 500}
			l ${iconSize * 38.5 / 500} 0
			l ${iconSize * 36.5 / 500} 0
			l ${iconSize * -37 / 500} ${iconSize * 34.8 / 500}
			v ${iconSize * -0.2 / 500}
			L ${iconSize * 245.6 / 500} ${iconSize * 65.9 / 500}
			Z
			M ${iconSize * 314.7 / 500} ${iconSize * 126.3 / 500}
			H ${iconSize * 185.3 / 500}
			V ${iconSize * 92.4 / 500}
			h ${iconSize * 129.4 / 500}
			V ${iconSize * 126.3 / 500}
			Z
		"/>
	</svg>
`;

export function createIcon(providers: string[]) {
	const icons: string[] = [];
	const hasMTR = providers.includes("MTR");
	const hasLRT = providers.includes("LRT");
	if (hasMTR) {
		icons.push(mtrLogo);
	}
	if (hasLRT) {
		icons.push(lrtLogo);
	}

	const colors: string[] = [];
	if (providers.includes("KMB")) {
		colors.push("#333333");
	}
	if (providers.includes("CTB")) {
		colors.push("#999999");
	}
	if (providers.includes("GMB")) {
		colors.push("#339933");
	}
	if (colors.length > 0) {
		if (colors.length === 1) {
			colors.push(colors[0]);
		}
		const colorsString = colors.map((color, index) => {
			const angle1 = -Math.PI * 2 * index / colors.length;
			const angle2 = -Math.PI * 2 * (index + 1) / colors.length;
			const radius = iconSize * 2 / 5;
			return `
				<path d="
					M ${iconSize / 2 + Math.sin(angle1) * radius / 2} ${iconSize / 2 + Math.cos(angle1) * radius / 2}
					A ${radius / 2} ${radius / 2} 0 0 1 ${iconSize / 2 + Math.sin(angle2) * radius / 2} ${iconSize / 2 + Math.cos(angle2) * radius / 2}
				" fill="none" stroke="${color}" stroke-width="${radius}"/>
			`;
		});
		icons.push(`
			<svg width="${iconSize}" height="${iconSize}">
				<circle fill="white" cx="${iconSize / 2}" cy="${iconSize / 2}" r="${iconSize / 2}"/>
				${colorsString}
			</svg>
		`);
	}

	return Leaflet.divIcon({
		className: "map-icon-wrapper",
		iconSize: [iconSize * icons.length, iconSize],
		iconAnchor: [iconSize * icons.length / 2, iconSize / 2],
		html: `<div class="row" style="filter: drop-shadow(0 0 ${iconShadowRadius}px rgba(0, 0, 0, 0.5))">${icons.join("")}</div>`,
	});
}
