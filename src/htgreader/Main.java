package htgreader;

import java.util.ArrayList;
import java.util.List;

public class Main {
	

    
	public static void main(String[] args)  throws Exception {
		String exportPath = "C:/Magno/DEMRJ/";
		HGTReader reader = new HGTReader( "C:/Magno/Magno/SRTM1v3.0/", "http://osm.casnav.mb/osmope/wms/" );

		// LatLon coord = new LatLon( -22.80, -43.22 );
		// readElevation( latLon, data);

		List<LatLon> path = new ArrayList<LatLon>(); 

		path.add( new LatLon( -22.7997939,-43.8751273 ) );
		path.add( new LatLon( -22.4949849,-43.6265616 ) );
		path.add( new LatLon( -22.4880063,-42.9886679 ) );
		path.add( new LatLon( -22.6858074,-42.5986533 ) );
		path.add( new LatLon( -22.7801697,-42.3033957 ) );
		
		CellList cellList = new CellList( reader.getProfile( path ) );
		System.out.println( cellList.asFeatureCollection() );
		
		reader.saveAsImage( exportPath, cellList );
		cellList.saveToCSV( exportPath + "data.csv" );
		
	}

	
    
    
	
}
