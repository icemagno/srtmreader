package htgreader;

public class LatLon {
	public double lat;
	public double lon;
	
	public LatLon( double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}
	
	public double getLat() {
		return lat;
	}
	
	public double getLon() {
		return lon;
	}
	
	@Override
	public String toString() {
		return String.valueOf(lat) + ", " + String.valueOf(lon);
	}
	
}
