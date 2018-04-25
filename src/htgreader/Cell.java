package htgreader;

public class Cell {
	private double lat;
	private double lon;
	private int ele;
	private int cellIndex;
	private int row;
	private int col;
	
	public Cell( Double lat, double lon, int ele, int cellIndex, int row, int col) {
		this.lat = lat;
		this.lon = lon;
		this.ele = ele;
		this.cellIndex = cellIndex;
		this.row = row;
		this.col = col;
	}

	
	public String getAsFeature() {
		String res = "{\"type\":\"Feature\",";
		res = res + "\"geometry\":{\"type\":\"Point\",\"coordinates\":["+getLat()+","+getLon()+"]},";
		res = res + "\"properties\":{\"ele\":\""+ getEle()+"\"}}";
		return res;
	}
	
	public int getRow() {
		return row;
	}
	
	public void setRow(int row) {
		this.row = row;
	}
	
	public int getCol() {
		return col;
	}
	
	public void setCol(int col) {
		this.col = col;
	}
	
	public int getCellIndex() {
		return cellIndex;
	}

	public void setCellIndex(int cellIndex) {
		this.cellIndex = cellIndex;
	}

	public double getLat() {
		return lat;
	}
	
	public void setLat(double lat) {
		this.lat = lat;
	}
	
	public double getLon() {
		return lon;
	}
	
	public void setLon(double lon) {
		this.lon = lon;
	}
	
	public int getEle() {
		return ele;
	}
	
	public void setEle(int ele) {
		this.ele = ele;
	}
	
	

}
