"""Convert Natural Earth's public-domain land GeoJSON to compact bundled polygons."""
import hashlib
import json
from pathlib import Path
import struct
import urllib.request

PROJECT = Path(__file__).resolve().parents[1]
SOURCE = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_land.geojson"


def rings_of(geometry: dict) -> list[list[list[float]]]:
    if geometry["type"] == "Polygon":
        return [geometry["coordinates"][0]]
    return [polygon[0] for polygon in geometry["coordinates"]]


def main() -> None:
    with urllib.request.urlopen(SOURCE, timeout=30) as response:
        raw = response.read()
    collection = json.loads(raw)
    rings = [ring for feature in collection["features"] for ring in rings_of(feature["geometry"])]
    assets = PROJECT / "assets"
    assets.mkdir(exist_ok=True)
    with (assets / "land.bin").open("wb") as out:
        out.write(struct.pack(">I", len(rings)))
        for ring in rings:
            out.write(struct.pack(">I", len(ring)))
            for lon, lat in ring:
                out.write(struct.pack(">ff", lon, lat))
    print(f"Bundled {len(rings)} land polygons. Source SHA256: {hashlib.sha256(raw).hexdigest()}")


if __name__ == "__main__":
    main()
