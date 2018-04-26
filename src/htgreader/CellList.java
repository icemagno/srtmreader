package htgreader;

import java.util.List;

public class CellList {
	private List<CellData> cells;
	
	public CellList( List<CellData> cells ) {
		this.cells = cells;
	}
	
	public List<CellData> getCells() {
		return cells;
	}
	
	public String asFeatureCollection() {
		return "";
	}
	
}
