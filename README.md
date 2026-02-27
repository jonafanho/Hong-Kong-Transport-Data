# Hong Kong Transport Data

The Hong Kong government has provided [a collection of public transport ETA APIs](https://data.gov.hk/en-datasets?isApiAvailable=true&category=transport) for public use. However, the schemas vary greatly from one transport provider to another. This project aims to provide a unified API for users to query stops and ETA arrivals across all transport providers.

## Installation

The recommended way to run this application is through Docker. See the [docker-compose.yml](https://github.com/jonafanho/Hong-Kong-Transport-Data/blob/master/docker-compose.yml) file for an example.

## Usage

After the application starts, the webserver can be accessed from the internal port 8080 (or another port if forwarded by Docker).

### Viewing Arrivals At A Stop

Clicking on a dot on the map will bring up current arrivals at that location. Each dot can represent a list of stops close to each other. Zooming on the map can change which stops are grouped together.

### Viewing Arrivals Within An Area

When holding down control and using the left mouse button to click and drag, a blue rectangle is created. All arrivals within this area will be shown. Note that if the area is too big, the request might take a long time or fail altogether.

### Obtaining the Request URL

When viewing arrivals at a stop or within an area, the "Copy HTTP Request URL" button at the top right of the arrivals panel will copy the request URL to the clipboard. This can be useful for debugging or creating custom applications that consume this API.

## API Documentation

This application provides several useful APIs.

- [GET: `getArrivalsByStopIds`](#get-getarrivalsbystopids)
- [GET: `getArrivalsByArea`](#get-getarrivalsbyarea)
- [GET: `getStops`](#get-getstops)
- [GET: `getProviderProperties`](#get-getproviderproperties)

All requests are under `/api/`, for example:

```
http://localhost:8080/api/myEndpoint
```

All responses are wrapped in the following format:

```json
{
	"data": [
		...
	],
	"currentTime": 946684800000,
	"version": "build-20000101-123456"
}
```

- `data` (array, required): The data to be retrieved
- `currentTime` (number, required): The current epoch time (provided by the server)
- `version` (string, required): The version number of the current application

### GET: `getArrivalsByStopIds`

Obtains a list of arrivals for one or more stop IDs.

#### Parameters

Example:

```
/api/getArrivalsByStopIds?stopIds=GMB_20007686,KMB_82F22BA7623AA4CB,CTB_003085,KMB_C43782B93826F4CD,KMB_B8E228521E55C7C5,CTB_003081
```

- `stopIds` (string list, required): Comma-separated list of stop IDs

#### Response

A list of arrivals sorted by `arrival` (or `minutes` if `arrival` is zero). Example:

```json
{
	"data": [
		{
			"route": "40",
			"destinationEn": "Laguna City",
			"destinationTc": "麗港城",
			"platform": "",
			"arrival": 946684860000,
			"minutes": 1,
			"realtime": false,
			"provider": "KMB"
		},
		{
			"route": "751",
			"destinationEn": "Tin Yat",
			"destinationTc": "天逸",
			"platform": "1",
			"arrival": 0,
			"minutes": 3,
			"realtime": true,
			"provider": "LRT"
		},
		...
	],
	"currentTime": 946684800000,
	"version": "build-20000101-123456"
}
```

- `route` (string, required): The route number (or line number for MTR)
- `destinationEn` (string, required): The destination in English
- `destinationTc` (string, required): The destination in Traditional Chinese
- `platform` (string, optional): The platform number or an empty string if not applicable
- `arrival` (number, optional): The arrival time (milliseconds after epoch) or 0 if not provided
- `minutes` (number, required): The arrival time (minutes from now)
- `realtime` (boolean, required): Whether the arrival is being tracked (not a scheduled departure)
- `provider` (string, required): The 3-letter code of the transport provider

#### Notes

If `provider` is `LRT`, the `arrival` field will be `0`. If `provider` is `LRT` or `MTR`, the `platform` field will be provided, otherwise an empty string will be returned.

### GET: `getArrivalsByArea`

Obtains a list of arrivals within latitude/longitude bounds.

#### Parameters

Example:

```
/api/getArrivalsByArea?minLat=22.38591257075172&maxLat=22.38669627285693&minLon=114.20568928182738&maxLon=114.20665467392338
```

- `minLat` (number, required): The minimum latitude value of the bounding box
- `maxLat` (number, required): The maximum latitude value of the bounding box
- `minLon` (number, required): The minimum longitude value of the bounding box
- `maxLon` (number, required): The maximum longitude value of the bounding box

#### Response

Same as the `getArrivalsByStopIds` response; see the section above.

### GET: `getStops`

Obtains a list of stops within latitude/longitude bounds with optional merging.

#### Parameters

Example:

```
/api/getStops?minLat=22.383442392189114&maxLat=22.39451321165635&minLon=114.20016415508856&maxLon=114.21473391445745&mergeDistance=0.0003814697265625
```

- `minLat` (number, required): The minimum latitude value of the bounding box
- `maxLat` (number, required): The maximum latitude value of the bounding box
- `minLon` (number, required): The minimum longitude value of the bounding box
- `maxLon` (number, required): The maximum longitude value of the bounding box
- `mergeDistance` (number, optional): If provided and if stops are closer than this distance, return them as merged, otherwise don't perform any stop merging

#### Response

A list of stops within the requested area. Example:

```json
{
	"data": [
		{
			"ids": [
				"GMB_20002261",
				"KMB_C305A8135A09C094"
			],
			"namesEn": [
				"FORTUNE CITY ONE (ST300)",
				"City One, Ngan Shing Street, Outside Fortune City One"
			],
			"namesTc": [
				"置富第一城 (ST300)",
				"第一城, 置富第一城外, 銀城街"
			],
			"lat": 22.386874499999998,
			"lon": 114.2036565,
			"routes": [
				"806X",
				"811A",
				"65S",
				"89X",
				"90",
				"811",
				"806A",
				"813",
				"804",
				"813A"
			],
			"providers": [
				"KMB",
				"GMB"
			]
		},
		{
			"ids": [
				"MTR_SHM"
			],
			"namesEn": [
				"Shek Mun"
			],
			"namesTc": [
				"石門"
			],
			"lat": 22.38786111,
			"lon": 114.20847222,
			"routes": [
				"TML"
			],
			"providers": [
				"MTR"
			]
		},
		...
	],
	"currentTime": 946684800000,
	"version": "build-20000101-123456"
}
```

- `ids` (string array, required): The stop IDs (can be more than one if stop merging occurred)
- `namesEn` (string array, required): The stop names in English (can be more than one if stop merging occurred)
- `namesTc` (string array, required): The stop names in Traditional Chinese (can be more than one if stop merging occurred)
- `lat` (number, required): The latitude of the stop (or average latitude of merged stops)
- `lon` (number, required): The longitude of the stop (or average longitude of merged stops)
- `routes` (string array, required): The routes serving this stop (or merged stops)
- `providers` (string array, required): The providers serving this stop (or merged stops)

### GET: `getProviderProperties`

Obtains a list of transport providers and additional information about each provider.

#### Parameters

This method has no parameters. Example:

```
/api/getProviderProperties
```

#### Response

A list of transport providers with additional information. Example:

```json
{
	"data": [
		{
			"provider": "MTR",
			"lastUpdated": 946684800000,
			"minLat": 22.24199167,
			"maxLat": 22.529,
			"minLon": 113.9365,
			"maxLon": 114.2697
		},
		{
			"provider": "CTB",
			"lastUpdated": 946684800000,
			"minLat": 22.205280632091,
			"maxLat": 22.552058932091,
			"minLon": 113.89929240053,
			"maxLon": 114.27471312053
		},
		...
	],
	"currentTime": 946684800000,
	"version": "build-20000101-123456"
}
```

- `provider` (string, required): The 3-letter code of the transport provider
- `lastUpdated` (number, required): The time (milliseconds after epoch) that the static stop data for the specific transport provider was last fetched
- `minLat` (number, required): The minimum latitude value of the stops for the transport provider
- `maxLat` (number, required): The maximum latitude value of the stops for the transport provider
- `minLon` (number, required): The minimum longitude value of the stops for the transport provider
- `maxLon` (number, required): The maximum longitude value of the stops for the transport provider
