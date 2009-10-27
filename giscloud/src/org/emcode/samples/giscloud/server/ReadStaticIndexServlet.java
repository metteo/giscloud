package org.emcode.samples.giscloud.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;

public class ReadStaticIndexServlet extends HttpServlet {
	
	private static STRtree index = null;
	private GeometryFactory gf = new GeometryFactory();
	private static final Logger log = Logger.getLogger(ReadStaticIndexServlet.class.getName());
	
	public void init(ServletConfig config) throws ServletException
    {
		try
		{
			long s1 = System.currentTimeMillis();
	    	log.info("Fetch serialized index");
	        File file = new File("WEB-INF/index.ser");
	        log.info(file.getAbsolutePath());
	        log.info("Creating FileInputStream");
	        FileInputStream fis = new FileInputStream(file);
	        log.info("Creating ObjectInputStream");
	        ObjectInputStream in = new ObjectInputStream(fis);
	        long t1 = System.currentTimeMillis();
	        long e1 = t1 - s1;
	        log.info("Elapsed milliseconds to get file: " + Long.toString(e1));
	        
	        log.info("Deserialize index");
	        
			long s2 = System.currentTimeMillis();
	    	index = (STRtree) in.readObject();
			long t2 = System.currentTimeMillis();
			long e2 = t2 - s2;
			log.info("Elapsed milliseconds to deserialize index: " + Long.toString(e2));
			
		} catch (Exception e) {
			throw new ServletException("Servlet Exception: " + e.getLocalizedMessage());
		}
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
    	
    	
    	long start = System.currentTimeMillis();
    	log.log(Level.FINE, "Begin request\n");
    	resp.setContentType("text/plain");
    	
		// ok, now I have a spatial index, how do I use it?
		
		// create a point (like, our old house in SLC)
		// 40.710544,-111.848953
		log.log(Level.FINE,"\nCreate test point");
		long s3 = System.currentTimeMillis();
		double lng = Double.parseDouble("-111.848953");
		double lat = Double.parseDouble("40.710544");
		Coordinate coord = new Coordinate(lng, lat);
		Point point = gf.createPoint( coord );
		long t3 = System.currentTimeMillis();
		long e3 = t3 - s3;
		log.log(Level.FINE,"\tElapsed milliseconds to create test point: " + Long.toString(e3));
		log.log(Level.FINE,"\t" + point.toString());

		// ask if the point intersects any multipolygons in the index
		// (i know they are multipolygons because that is what i put in there)
		log.log(Level.FINE,"\nQuery index for point");
		long s4 = System.currentTimeMillis();
		List<MultiPolygon> items = index.query(point.getEnvelopeInternal());
		long t4 = System.currentTimeMillis();
		long e4 = t4 - s4;
		log.log(Level.FINE,"\tElapsed milliseconds to query index: " + Long.toString(e4));
		
        
		// since i have a point, I should only get one (unless there is
		// overlap in the blockgroup multipolygons, which i hope there isn't
		MultiPolygon match = items.get(0);
		log.log(Level.FINE,"\nREPORT:");
        log.log(Level.FINE,"\tThe test point was inside multipolygon: \n" + match.toText());
        log.log(Level.FINE,"Total time: " + Long.toString(System.currentTimeMillis() - start));
        
        resp.getWriter().println("Total time: " + Long.toString(System.currentTimeMillis() - start));
        
    }
    

}
