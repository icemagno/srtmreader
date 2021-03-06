package htgreader;

public class CellData {
	private int ele;
	private int cellIndex;
	private int row;
	private int col;
	private String fileName;
	private LatLon latLon;
	private boolean visible;
	
	public CellData( LatLon latLon, int ele, int cellIndex, int row, int col, String fileName) {
		this.latLon = latLon;
		this.ele = ele;
		this.cellIndex = cellIndex;
		this.row = row;
		this.col = col;
		this.fileName = fileName;
	}

	public CellData() {
		//
	}
	
	public boolean isVisible() {
		return this.visible;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public String getAsFeature() {
		String res = "{\"type\":\"Feature\",";
		res = res + "\"geometry\":{\"type\":\"Point\",\"coordinates\":["+ getLatLon().getLat() + ","+ getLatLon().getLon() + "]},";
		res = res + "\"properties\":{\"ele\":\""+ getEle()+"\", \"fileName\":\""+getFileName()+"\"}}";
		return res;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
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

	public LatLon getLatLon() {
		return latLon;
	}
	
	public void setLatLon(LatLon latLon) {
		this.latLon = latLon;
	}
	
	public int getEle() {
		return ele;
	}
	
	public void setEle(int ele) {
		this.ele = ele;
	}
	
	

}
