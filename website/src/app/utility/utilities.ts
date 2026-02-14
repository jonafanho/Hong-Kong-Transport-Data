import {MILLIS_PER_DAY, MILLIS_PER_HOUR, MILLIS_PER_MINUTE, MILLIS_PER_SECOND, MINUTES_PER_HOUR, SECONDS_PER_MINUTE} from "./constants";

export function getCookie(name: string, defaultValue = "") {
	const splitCookies = document.cookie.split("; ").filter(cookie => cookie.startsWith(name + "="));
	if (splitCookies.length > 0 && splitCookies[0].includes("=")) {
		return decodeURIComponent(splitCookies[0].split("=")[1]);
	} else {
		return defaultValue;
	}
}

export function setCookie(name: string, value: string) {
	document.cookie = `${name}=${value}; expires=${new Date(2999, 11, 31).toUTCString()}; path=/`;
}

export function sortNumbers(item1: string, item2: string) {
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
}

export function sortAndTrim(list: string[], count: number) {
	return list.sort(sortNumbers).slice(0, count).join(", ") + (count < list.length ? `... (+${list.length - count})` : "");
}

export function formatAbsoluteTime(millis: number) {
	const date = new Date(millis);
	return `${millis - Date.now() >= MILLIS_PER_DAY ? date.toLocaleDateString() : ""} ${date.toLocaleTimeString()}`;
}

export function formatRelativeTime(millis: number) {
	if (millis <= 0) {
		return undefined;
	} else {
		const seconds = Math.floor(millis / MILLIS_PER_SECOND) % SECONDS_PER_MINUTE;
		const minutes = Math.floor(millis / MILLIS_PER_MINUTE) % MINUTES_PER_HOUR;
		const hours = Math.floor(millis / MILLIS_PER_HOUR);
		const paddedMinutes = String(minutes).padStart(2, "0");
		const paddedSeconds = String(seconds).padStart(2, "0");
		return hours > 0 ? `${hours}:${paddedMinutes}:${paddedSeconds}` : `${minutes}:${paddedSeconds}`;
	}
}
