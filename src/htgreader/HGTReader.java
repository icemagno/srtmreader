package htgreader;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import com.sun.org.apache.bcel.internal.generic.GETFIELD;



public class HGTReader {
	// 	https://gis.stackexchange.com/questions/43743/extracting-elevation-from-hgt-file
	private final int SECONDS_PER_MINUTE = 60;
    
	private final int HGT_RES = 1; // resolution in arc seconds
	private final int HGT_ROW_LENGTH = 3601; // number of elevation values per line
	private final int HGT_VOID = -32768; // magic number which indicates 'void data' in HGT file	
	private final String USER_AGENT = "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.13) Gecko/20101206 Ubuntu/10.10 (maverick) Firefox/3.6.13";
	private final int EARTH_RADIUS = 6371; // Approx Earth radius in KM
	
	private String srtmPath;
	private String fileExt = ".hgt";
	private String WMSMapSource;
	private final HashMap<String, ShortBuffer> cache = new HashMap<>();
	
	
	public HGTReader( String srtmPath, String WMSMapSource ) {
		this.srtmPath = srtmPath;
		this.WMSMapSource = WMSMapSource;
	}
	
	public  List<CellData> getProfile( List<LatLon> path ) throws Exception {
		List<CellData> cells = new ArrayList<CellData>();
		for( LatLon latLon : path ) {
			CellData cell = readElevation( latLon );
			cells.add(cell);
		}
		return cells;
	}
	
	public LatLon projectPoint( LatLon center, double distance, double bearing ) {
		// http://www.movable-type.co.uk/scripts/latlong.html
			
		double radius = 6371;
		
		double delta = distance / radius;
		double theta = Math.toRadians( bearing );
		
		double phi1 = Math.toRadians( center.getLat() );
		double lambda1 = Math.toRadians( center.getLon() );
		
		double sinPhi1 = Math.sin(phi1);
		double cosPhi1 = Math.cos(phi1);
		
		double sindelta = Math.sin(delta); 
		double cosdelta = Math.cos(delta);
		double sinTheta = Math.sin(theta); 
		double cosTheta = Math.cos(theta);
		
		double sinPhi2 = sinPhi1 * cosdelta + cosPhi1 * sindelta * cosTheta;
		double phi2 = Math.asin(sinPhi2);
		double y = sinTheta * sindelta * cosPhi1;
		double x = cosdelta - sinPhi1 * sinPhi2;
		double lambda2 = lambda1 + Math.atan2(y, x);		
				
		return new LatLon( Math.toDegrees(phi2), ( Math.toDegrees(lambda2) +540 ) % 360-180 ); 
	}
	
    public double calcDistance( LatLon start,  LatLon end) {

		double dLat  = Math.toRadians( ( end.getLat() - start.getLat() ) );
		double dLong = Math.toRadians( ( end.getLon() - start.getLon() ) );
		
		double startLat = Math.toRadians( start.getLat() );
		double endLat   = Math.toRadians( end.getLat() );
		
		double a = haversin(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversin(dLong);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		
		return EARTH_RADIUS * c; // <-- d
	}
		
	private double haversin(double val) {
		return Math.pow(Math.sin(val / 2), 2);
	}
	
	
	
	public void computeViewShed( LatLon coord, double distance, String exportPath ) throws Exception {
		System.out.println("Calculating viewshad...");
		System.out.println(" > Observer at " + coord.toString() );
		
		CellData observerElevation = readElevation(coord);
		ShortBuffer data = readHgtFile( observerElevation.getFileName() );
		
		
		System.out.println(" > Observer elevation: " + observerElevation.getEle() );
		
		System.out.println(" > Center coordinates : " + coord.toString()  );
		
		LatLon temp = projectPoint( coord, distance, 180);  
		System.out.println(" > Projecting point to 180 degres at " + distance + " Km : " + temp.toString() );
		
		int centerPixelCol = latLonToCell( coord ).getCol();
		int centerPixelRow = latLonToCell( coord ).getRow();
		
		int borderPixelCol = latLonToCell( temp ).getCol();
		int borderPixelRow = latLonToCell( temp ).getRow();
		
		//int deltaCol = centerPixelCol - borderPixelCol; 
		int deltaRow = centerPixelRow - borderPixelRow;
		
		
		
		System.out.println(" > Center : " + centerPixelCol + "," + centerPixelRow );
		System.out.println(" > Border : " + borderPixelCol + "," + borderPixelRow );
		System.out.println(" > Distance in pixels : " + deltaRow );
		
		
		
		
		// ----------------  PROCEDIMENTO PARA SALVAR IMAGEM --------------------------------------------------------
		String fileName = observerElevation.getFileName();
		// Usa quadro branco
    	BufferedImage bufferedImage = new BufferedImage(HGT_ROW_LENGTH, HGT_ROW_LENGTH, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bufferedImage.createGraphics();
		g2d.setColor( Color.WHITE );
		g2d.fillRect(0, 0, HGT_ROW_LENGTH, HGT_ROW_LENGTH);				
		/*  Usa Mapa de fundo OSM
		String bbox = getTileBBox( fileName );
		saveImage( exportPath + "viewshad_map.jpg", HGT_ROW_LENGTH, "osm:OSMMapa", bbox );		
		BufferedImage bufferedImage = ImageIO.read( new File(exportPath + "viewshad_map.jpg") );
		Graphics2D g2d = bufferedImage.createGraphics();
		*/
		g2d.setComposite(AlphaComposite.Src);
		
		
		// DESENHA O Modelo Digital de Elevacao
		for( int col = 0; col < HGT_ROW_LENGTH; col++  ) {
			for( int row = HGT_ROW_LENGTH; row > 0 ; row--   ) {
    			
    			int cell = (HGT_ROW_LENGTH * (row - 1)) + col;
    			short ele = data.get(cell);
    			g2d.setColor( Color.WHITE );
    			if( ele > 5 ) {
    				g2d.setColor( new Color(249, 249, 249) );
    			}
    			if( ele > 10 ) {
    				g2d.setColor( new Color(239, 239, 239) );
    			}
    			if( ele > 50 ) {
    				g2d.setColor( new Color(229, 229, 229) );
    			}
    			if( ele > 100 ) {
    				g2d.setColor( new Color(219, 219, 219) );
    			}
    			if( ele > 150 ) {
    				g2d.setColor( new Color(209, 209, 209) );
    			}
    			
    			if ( ele > 200 ) {
    	    		if( ele > 1200 ) { ele = 1200; }
    	    		int color = 255 - (ele / 5) ;
    	    		g2d.setColor( new Color(color, color, color) );
    			}
    			g2d.drawLine(col,row,col,row);
    		}
    	}		
		
		
		
		// Desenha o VIEWSHAD
		g2d.setColor( Color.RED );
		for( int tempRadius = 0; tempRadius <= deltaRow; tempRadius++  ) {
			int tallestPoint = -1;
			
			int observerEle = observerElevation.getEle(); 
			
			for ( int angle = 0; angle < 360; angle+= 2 ) {
				int x = (int) (centerPixelCol + tempRadius * Math.cos( angle ) );
				int y = (int) ( centerPixelRow + tempRadius * Math.sin( angle ) );
				
				int cell = (HGT_ROW_LENGTH * (y - 1)) + x;
				short ele = data.get(cell);
				boolean canSee = true;
				
				
				if ( (ele > tallestPoint) ) {
					tallestPoint = ele; 
				}
				
				if ( (ele < tallestPoint) && ( ele > observerEle ) ) canSee = false;
				
				
				// ------------------ SO DESENHA o PIXEL SE PUDER VER ------------------------
				if ( canSee ) {
					g2d.drawLine( x, y, x, y ); // <<< -- DESENHA UM PIXEL VERMELHO
				}
				
				
			}
		}
		
		
		
		
		g2d.dispose();
		File file = new File( exportPath + "viewshad.png");
		ImageIO.write(bufferedImage, "png", file);			
		
		System.out.println("Done.");
		
	}
	

	private  CellData latLonToCell( LatLon coord ) {
        double fLat = frac( Math.abs( coord.getLat() ) ) * SECONDS_PER_MINUTE;
        double fLon = frac( Math.abs( coord.getLon() ) ) * SECONDS_PER_MINUTE;		
        int row = (int) Math.round(fLat * SECONDS_PER_MINUTE / HGT_RES);
        int col = (int) Math.round(fLon * SECONDS_PER_MINUTE / HGT_RES);		
        row = HGT_ROW_LENGTH - row;
        int cell = (HGT_ROW_LENGTH * (row - 1)) + col;
        
        CellData cellData = new CellData();
        cellData.setCellIndex( cell );
        cellData.setCol(col);
        cellData.setRow(row);
        
        return cellData;
	}
	
	private  CellData readElevation( LatLon coord ) throws Exception {
		
		String fileName = getHgtFileName( coord );
		ShortBuffer data = readHgtFile( fileName );
		
		CellData cellData = latLonToCell(coord);
		cellData.setFileName(fileName);
		cellData.setLatLon(coord);
		cellData.setEle( 0 );
		int cell = cellData.getCellIndex();
		
        if ( cell < data.limit() ) {
            short ele = data.get(cell);

            if (ele != HGT_VOID) {
            	cellData.setEle(ele);
            }
            
        } else {
            System.out.println("No Elevation - Out of Range");
        }
        
        return cellData;
	}
	
    private  ShortBuffer readHgtFile(String fileName) throws Exception {
		String htgFile = srtmPath + fileName + fileExt;

		if ( cache.containsKey( fileName ) ) {
			return cache.get( fileName );
		}
    	
		cache.put(fileName, null);
		
        FileInputStream fis = new FileInputStream(htgFile);
        FileChannel fc = fis.getChannel();

        ByteBuffer bb = ByteBuffer.allocateDirect((int) fc.size());
        while (bb.remaining() > 0) fc.read(bb);

        bb.flip();
        ShortBuffer sb = bb.order(ByteOrder.BIG_ENDIAN).asShortBuffer();
        
        fc.close();
        fis.close();
        
        cache.put(fileName, sb);
        return sb;
    }	
	
	public void saveImage( String destinationFile, int resolution, String layerName, String bbox) throws IOException {
		String imageUrl = WMSMapSource + "/?service=WMS&srs=EPSG:4326&width="+resolution+"&height="+resolution+"&version=1.3&transparent=true&request=GetMap&layers="+layerName+"&format=image/png8&mode=8bit&bbox=" + bbox;
		
		System.out.println(" > Downloading image " + layerName + " from " + WMSMapSource );
		//System.out.println( imageUrl );
		
		
		imageUrl = imageUrl.replace(" ", "");
		File picutreFile = new File( destinationFile );
        URL url=new URL( imageUrl );
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setConnectTimeout( 240000 );
        conn.setRequestMethod("GET");  
        conn.connect();
        
        java.nio.file.Files.copy( conn.getInputStream(), 
        		picutreFile.toPath(), StandardCopyOption.REPLACE_EXISTING);        
        
	}	    
    
	
    public  String getHgtFileName( LatLon coord ) {
        
    	int lat = (int) coord.getLat();
        int lon = (int) coord.getLon();
        
        System.out.println("Detecting SRTM file from corrdinates " + coord.getLat() + ", " + coord.getLon() + "..." );
        
        String latPref = "N";
        if (lat < 0) {
        	latPref = "S";
        	lat--;
        } else {
        	lat++;
        }

        String lonPref = "E";
        if (lon < 0) {
            lonPref = "W";
            lon--;
        } else {
        	lon++;
        }
        
        String lonT = String.valueOf(lon).replace("-", "");
        String latT = String.valueOf(lat).replace("-", "");
        if ( lonT.length() == 2 ) {
        	lonT = "0" + lonT;
        }
        
        String ret = latPref + latT + lonPref + lonT;
        System.out.println(" > " + ret );
        return ret;
    }	
	
    
    private double frac(double d) {
        long iPart;
        double fPart;
        iPart = (long) d;
        fPart = d - iPart;
        return fPart;
    }    
    
    public String getTileBBox( String tileName ) {
    	// bbox = -43,-22,-42,-21
    	// S23W044
    	String signalLat = "";
    	String signalLon = "";
    	if ( tileName.contains("S") ) {
    		signalLat = "-";
    	}
    	
    	if ( tileName.contains("W") ) {
    		signalLon = "-";
    	}
    	
    	tileName = tileName.replace("S", "").replace("N", "");
    	
    	String latS = tileName.substring(0,2);
    	String lonS = tileName.substring(4,6);
    	
    	int lat = Integer.valueOf( latS );
    	int lon = Integer.valueOf( lonS );
    	
    	int latF = lat - 1;
    	int lonF = lon - 1;
    	
    	String lonFS = String.valueOf( lonF );
    	String latFS = String.valueOf( latF );
    	
    	return signalLon + lonS + "," + signalLat + latS + "," + signalLon + lonFS + "," + signalLat + latFS ;
    }
    
    public  void saveAsImage( String exportPath, CellList cellList ) throws Exception {
    	
        Iterator<Entry<String, ShortBuffer>> it = cache.entrySet().iterator();
        while ( it.hasNext() ) {    	
        	Entry<String, ShortBuffer> pair = it.next();
        	
    		String fileName = pair.getKey();
    		ShortBuffer data = pair.getValue();

    		System.out.println("Processing tile " + fileName);
    		
    		String bufferFile = exportPath + fileName + ".png";
    		String profileFile = exportPath + fileName + "_profile.png";
    		String contourFile = exportPath + fileName + "_contour.png";
    		String mapFile = exportPath + fileName + "_map.png";
    		String finalFile = exportPath + fileName + "_final.png";
    		
    		String bbox = getTileBBox( fileName );
    		saveImage( contourFile, HGT_ROW_LENGTH, "osm:curvas_nivel", bbox );
    		saveImage( mapFile, HGT_ROW_LENGTH, "osm:OSMMapa", bbox );

    		System.out.println( " > Exporting buffer data to " + bufferFile );
    		
	    	BufferedImage bufferedImage = new BufferedImage(HGT_ROW_LENGTH, HGT_ROW_LENGTH, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = bufferedImage.createGraphics();
	    	
			g2d.setComposite(AlphaComposite.Clear);
			g2d.fillRect(0, 0, HGT_ROW_LENGTH, HGT_ROW_LENGTH);		
			
			g2d.setComposite(AlphaComposite.Src);
			for( int col = 0; col < HGT_ROW_LENGTH; col++  ) {
				for( int row = HGT_ROW_LENGTH; row > 0 ; row--   ) {
	    			
	    			int cell = (HGT_ROW_LENGTH * (row - 1)) + col;
	    			short ele = data.get(cell);
	    			
	    			g2d.setColor( Color.WHITE );
	    			
	    			
	    			if( ele > 5 ) {
	    				g2d.setColor( new Color(249, 249, 249) );
	    			}
	    			if( ele > 10 ) {
	    				g2d.setColor( new Color(239, 239, 239) );
	    			}
	    			if( ele > 50 ) {
	    				g2d.setColor( new Color(229, 229, 229) );
	    			}
	    			if( ele > 100 ) {
	    				g2d.setColor( new Color(219, 219, 219) );
	    			}
	    			if( ele > 150 ) {
	    				g2d.setColor( new Color(209, 209, 209) );
	    			}
	    			
	    			if ( ele > 200 ) {
	    	    		if( ele > 1200 ) { ele = 1200; }
	    	    		int color = 255 - (ele / 5) ;
	    	    		g2d.setColor( new Color(color, color, color) );
	    			}
	    			
	    			
	    			g2d.drawLine(col,row,col,row);
	    			
	    		}
	    		
	    	}
	    	
			System.out.println( " > Exporting profile data to " + profileFile );
			
	    	BufferedImage bufferedImageData = new BufferedImage(HGT_ROW_LENGTH, HGT_ROW_LENGTH, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2Data = bufferedImageData.createGraphics();
			g2Data.setComposite(AlphaComposite.Clear);
			g2Data.fillRect(0, 0, HGT_ROW_LENGTH, HGT_ROW_LENGTH);		
			g2Data.setComposite(AlphaComposite.Src);
			
	
			Font font = new Font("Courier New", Font.BOLD, 20);    
			
			//AffineTransform affineTransform = new AffineTransform();
			//affineTransform.rotate(Math.toRadians(45), 0, 0);
			//Font rotatedFont = font.deriveFont(affineTransform);
			
			g2Data.setFont( font );
			
			g2Data.setColor( Color.BLUE );
	    	for ( CellData cellData : cellList.getCells() ) {
	    		if ( cellData.getFileName().equals( fileName ) ) { 
 		    		//g2Data.drawLine( cellData.getCol(), cellData.getRow(), lastCol , lastRow );
	    			Ellipse2D.Double circle = new Ellipse2D.Double(cellData.getCol() - 5, cellData.getRow() - 5 , 10, 10);
		    		g2Data.fill(circle);
		    		String s = String.valueOf( cellData.getEle() );
		            g2Data.drawString( s, cellData.getCol() - 10, cellData.getRow() - 10 );  
	    		}
	    	}
	    	
	    	g2d.dispose();
	    	g2Data.dispose();
			
			File file = new File( bufferFile );
			ImageIO.write(bufferedImage, "png", file);

			File fileData = new File( profileFile );
			ImageIO.write(bufferedImageData, "png", fileData);
			
			System.out.println(" > Composing...");
			
			//BufferedImage map = ImageIO.read( new File(mapFile) );
			BufferedImage profile = ImageIO.read( new File(profileFile) );
			BufferedImage contour = ImageIO.read( new File(contourFile) );
			BufferedImage buffer = ImageIO.read( new File(bufferFile) );
			
			Graphics g = buffer.getGraphics();
			
			g.drawImage(contour, 0, 0, null);
			g.drawImage(profile, 0, 0, null);
			//g.drawImage(buffer, 0, 0, null);
			//g.drawImage(map, 0, 0, null);
			
			ImageIO.write(buffer, "PNG", new File(finalFile) );
			
			System.out.println("Tile " + fileName + " done.");
    	}   
        
        System.out.println("Done.");
		
    }
	
	
	
}
