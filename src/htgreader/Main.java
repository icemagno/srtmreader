package htgreader;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
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
	
    private static int maxElevation = 0;
    
	public static void main(String[] args)  throws Exception {
		// START : -22.53666758280344, -42.05795827779525 
		// MEIO  : -22.53952149577536, -42.03100744161361
		// FIM   : -22.53381361084161, -42.00954976949447 
		
		double coorLat = -22.5;
		double coorLon = -43.9;
		
		//String fileName = getHgtFileName(coorLat, coorLon);
		
		String fileName = "S23W044.hgt";
		
		String htgFile = filePath + fileName;
		System.out.println( htgFile );
		
		ShortBuffer data = readHgtFile( htgFile );
		
		//readElevation(coorLat, coorLon, data);
        
		List<Cell> cells = getProfile( -22.5, -43.9, -22.5, -43.1, data );
        saveAsImage( data, cells );
        
        System.out.println("Max Elevation found : " + maxElevation );
        
	}

	
	private static List<Cell> getProfile( double coorLatS, double coorLonS, double coorLatF, double coorLonF, ShortBuffer data ) throws Exception {
		List<Cell> cells = new ArrayList<Cell>();
		
		System.out.println("From: " + coorLonS + "  To: " + coorLonF);
		
		for ( double lon = coorLonS; lon < coorLonF; lon = lon + 0.005  ) {
			Cell cell = readElevation( coorLatS, lon, data );
			cells.add(cell);
		}
		
		return cells;
		
	}

	
	
	private static Cell readElevation(double coorLat, double coorLon, ShortBuffer data ) throws Exception {
		
        double fLat = frac( Math.abs(coorLat) ) * SECONDS_PER_MINUTE;
        double fLon = frac( Math.abs(coorLon) ) * SECONDS_PER_MINUTE;		
        int row = (int) Math.round(fLat * SECONDS_PER_MINUTE / HGT_RES);
        int col = (int) Math.round(fLon * SECONDS_PER_MINUTE / HGT_RES);		
        row = HGT_ROW_LENGTH - row;
        int cell = (HGT_ROW_LENGTH * (row - 1)) + col;

        if ( cell < data.limit() ) {
            short ele = data.get(cell);

            if (ele == HGT_VOID) {
            	System.out.println("No Elevation - VOID");
            } else {
            	Cell res = new Cell(coorLat, coorLon, ele, cell, row, col );
            	
            	if ( ele > maxElevation ) {
            		maxElevation = ele;
            	}
            	
            	System.out.println(coorLat + ";" + coorLon + ";" + ele);
            	
            	return res;
            }
            
        } else {
            System.out.println("No Elevation - Out of Range");
        } 
        return null;
	}
	
    private static ShortBuffer readHgtFile(String file) throws Exception {

        FileChannel fc = null;
        ShortBuffer sb = null;
        try {
            // Eclipse complains here about resource leak on 'fc' - even with 'finally' clause???
            fc = new FileInputStream(file).getChannel();
            // choose the right endianness

            ByteBuffer bb = ByteBuffer.allocateDirect((int) fc.size());
            while (bb.remaining() > 0) fc.read(bb);

            bb.flip();
            //sb = bb.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            sb = bb.order(ByteOrder.BIG_ENDIAN).asShortBuffer();
        } finally {
            if (fc != null) fc.close();
        }

        return sb;
    }	
	
	
    public static String getHgtFileName(double latD, double lonD) {
        int lat = (int) latD;
        int lon = (int) lonD;
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
        String ret = latPref + lat + lonPref + lonT + ".hgt";
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
