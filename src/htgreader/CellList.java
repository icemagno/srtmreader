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
		StringBuilder sb = new StringBuilder();
		for( CellData cd : cells ) {
			sb.append( cd.getAsFeature() );
		}
		return sb.toString();
	}
	
}
