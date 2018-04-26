package htgreader;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

public class Main {
	
	// 	https://gis.stackexchange.com/questions/43743/extracting-elevation-from-hgt-file
	private static final int SECONDS_PER_MINUTE = 60;
    
	// alter these values for different SRTM resolutions
    public static final int HGT_RES = 1; // resolution in arc seconds
    public static final int HGT_ROW_LENGTH = 3601; // number of elevation values per line
    public static final int HGT_VOID = -32768; // magic number which indicates 'void data' in HGT file	
	
    public static String filePath = "C:/Magno/DEMRJ/";
    public static String fileExt = ".hgt";
	
    private static final HashMap<String, ShortBuffer> cache = new HashMap<>();
    
	public static void main(String[] args)  throws Exception {
		
		// LatLon coord = new LatLon( -22.5, -43.9 );
		// readElevation( latLon, data);

		List<LatLon> path = new ArrayList<LatLon>(); 
		for ( double lon = -43.9; lon < -43.1; lon = lon + 0.005  ) {
			LatLon coorP = new LatLon( -22.5, lon );
			path.add( coorP );
		}	
		
		CellList cellList = new CellList(  getProfile( path ) );
		System.out.println( cellList.asFeatureCollection() );
		
        //saveAsImage( data, cells );
        
	}

	
	private static List<Cell> getProfile( List<LatLon> path ) throws Exception {
		List<Cell> cells = new ArrayList<Cell>();
		for( LatLon latLon : path ) {
			Cell cell = readElevation( latLon );
			cells.add(cell);
		}
		return cells;
	}

	
	
	private static Cell readElevation( LatLon coord ) throws Exception {
		
		String fileName = getHgtFileName( coord );
		ShortBuffer data = readHgtFile( fileName );
		
        double fLat = frac( Math.abs( coord.getLat() ) ) * SECONDS_PER_MINUTE;
        double fLon = frac( Math.abs( coord.getLon() ) ) * SECONDS_PER_MINUTE;		
        int row = (int) Math.round(fLat * SECONDS_PER_MINUTE / HGT_RES);
        int col = (int) Math.round(fLon * SECONDS_PER_MINUTE / HGT_RES);		
        row = HGT_ROW_LENGTH - row;
        int cell = (HGT_ROW_LENGTH * (row - 1)) + col;

        if ( cell < data.limit() ) {
            short ele = data.get(cell);

            if (ele == HGT_VOID) {
            	System.out.println("No Elevation - VOID");
            } else {
            	Cell res = new Cell( coord, ele, cell, row, col, fileName );
            	return res;
            }
            
        } else {
            System.out.println("No Elevation - Out of Range");
        } 
        return null;
	}
	
    private static ShortBuffer readHgtFile(String fileName) throws Exception {
		String htgFile = filePath + fileName + fileExt;

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
	
	
    public static String getHgtFileName( LatLon coord ) {
        int lat = (int) coord.getLat();
        int lon = (int) coord.getLon();
        String latPref = "N";
        if (lat < 0) latPref = "S";

        String lonPref = "E";
        if (lon < 0) {
            lonPref = "W";
        }
        String lonT = String.valueOf(lon).replace("-", "");
        if ( lonT.length() == 2 ) {
        	lonT = "0" + lonT;
        }
        String ret = latPref + lat + lonPref + lonT;
        return ret.replace("-", "");
    }	
	
    
    public static double frac(double d) {
        long iPart;
        double fPart;
        iPart = (long) d;
        fPart = d - iPart;
        return fPart;
    }    
    
    
    public static void saveAsImage( ShortBuffer data, List<Cell> cells ) throws Exception {
    	BufferedImage bufferedImage = new BufferedImage(HGT_ROW_LENGTH, HGT_ROW_LENGTH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bufferedImage.createGraphics();
    	
		g2d.setColor(Color.white);
		g2d.fillRect(0, 0, HGT_ROW_LENGTH, HGT_ROW_LENGTH);		
		
		
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
    	

		Font font = new Font("Courier New", Font.PLAIN, 13);    
		AffineTransform affineTransform = new AffineTransform();
		affineTransform.rotate(Math.toRadians(45), 0, 0);
		Font rotatedFont = font.deriveFont(affineTransform);
		g2d.setFont(rotatedFont);
		
    	g2d.setColor( Color.RED );
    	for ( Cell cl : cells ) {
    		g2d.drawLine( cl.getCol(), cl.getRow(), cl.getCol() , cl.getRow() - cl.getEle() );
            String s = String.valueOf( cl.getEle() );
            g2d.drawString( s, cl.getCol(), cl.getRow() );  
            
    	}
    		
		g2d.dispose();
		File file = new File( filePath + "/image.png");
		ImageIO.write(bufferedImage, "png", file);    	
    	
		
    }
    
    
	
}
