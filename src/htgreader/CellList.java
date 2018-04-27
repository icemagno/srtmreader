package htgreader;

import java.io.PrintWriter;
import java.util.List;

public class CellList {
	private List<CellData> cells;
	
	public CellList( List<CellData> cells ) {
		this.cells = cells;
	}
	
	public List<CellData> getCells() {
		return cells;
	}
	
	public void saveToCSV( String fileName ) throws Exception {
		PrintWriter writer = new PrintWriter( fileName, "UTF-8" );
		writer.println("srtm;lat;lon;ele");
		
		for( CellData cd : cells ) {
			writer.println( cd.getFileName() + ";" + cd.getLatLon().getLat() + ";" + cd.getLatLon().getLon() + ";" + cd.getEle()  );
		}
		
		writer.close();		
	}
	
	public String asFeatureCollection() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("{\"type\":\"FeatureCollection\",\"Features\":[");
		String prefix = "";
		for( CellData cd : cells ) {
			sb.append( prefix );
			prefix = ",";
			sb.append( cd.getAsFeature() );
		}
		
		sb.append("]}");
		
		return sb.toString();
	}
	
}
