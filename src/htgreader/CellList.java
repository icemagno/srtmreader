package htgreader;

import java.util.List;

public class CellList {
	private List<Cell> cells;
	
	public CellList( List<Cell> cells ) {
		this.cells = cells;
	}
	
	public List<Cell> getCells() {
		return cells;
	}
	
	public String asFeatureCollection() {
		return "";
	}
	
}
