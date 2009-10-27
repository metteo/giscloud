package org.emcode.samples.giscloud.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
import javax.jdo.PersistenceManager;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.emcode.samples.giscloud.client.GisCloudService;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.Text;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GisCloudServiceImpl extends RemoteServiceServlet implements
		GisCloudService {

	private static STRtree index = null;
	private static final Logger log = Logger.getLogger(GisCloudServiceImpl.class.getName());
	private GeometryFactory gf = new GeometryFactory();
	Cache cache;
	
	public void init(ServletConfig config) throws ServletException
    {
		
		super.init(config);

		try
		{
			
			log.info("Loading on startup...");
	        
			// first, connect to memcache
			CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
	        cache = cacheFactory.createCache(Collections.emptyMap());

	        // ask for the spatial index in memory
	        log.info("Fetching SLC index from memory");
	        index = (STRtree) cache.get("saltlakecountyindex");
	        //
			
	        // if it was not in the cache, fetch it from the filesystem
	        if(index == null)
	        {
	        	log.info("SLC index was not in memory, fetching from file");
	        	long s1 = System.currentTimeMillis();
		    	log.info("Fetch serialized index for Salt Lake County");
		        File file = new File("WEB-INF/saltlakecounty.ser");
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
				
				// now put the index in memcache
				log.info("Putting index in cache");
				cache.put("saltlakecountyindex", index);
				
				// take it out again to see what it is
				STRtree test = (STRtree) cache.get("saltlakecountyindex");
				log.info("test of index:" + test);
		        
	        } else
	        {
	        	log.info("Index existed in cache");
	        }
	        
			
			// i wonder if forcing a hit on one of the spatial methods
			// would speed things up a bit on the first click
			//log.info("Let's hit point in polygon at start up");
			//pointInPolygon("0,0"); // middle of the ocean, but who cares.
			
		} catch (Exception e) {
			throw new ServletException("Servlet Exception: " + e.getLocalizedMessage());
		}
    }
	

	public String pointInPolygon(String lngLat) {
		
		log.info("Executing pointInPolygon test with bbox=" + lngLat);
		
		String geoGreeting = "Sorry. You didn't click <i>within</i> the boundary of Salt Lake County. Try again.";
		
		try
		{

			/*
			 * Create a point from incoming lngLat string
			 */
			String[] coords = lngLat.split( ",\\s*" ); // split on commas
			double lat = Double.parseDouble(coords[0]);
			double lng = Double.parseDouble(coords[1]);
			Coordinate coord = new Coordinate(lat, lng);
			Point point = gf.createPoint( coord );
			
			
			// query the index for the point
			List<Polygon> items = index.query(point.getEnvelopeInternal());
			
			// this should bring out either 1 or none
			Polygon saltLakeCounty = null;
			if( !items.isEmpty() )
			{
				saltLakeCounty = items.get(0);
			}
			
			// if we have a county, then check for contains
			if( saltLakeCounty != null )
			{
				if( saltLakeCounty.contains(point) )
				{
					geoGreeting = "Congratulations. You clicked <i>within</i> the boundary of Salt Lake County.";
				}
			}		
			
		} catch (Exception e)
		{
			log.log(Level.WARNING, e.getLocalizedMessage());
		}
		
		return geoGreeting;
	
	}
	
	public String polygonInPolygon(String bbox) {
		
		log.info("Executing polygonInPolygon test with bbox=" + bbox);
		String geoGreeting = "Sorry. The map bounds <i>expand beyond</i> the boundary of Salt Lake County.";
		
		try
		{
			// build a polygon from the map bounding box
			String bboxWKT = "POLYGON(("+bbox+"))";
			WKTReader reader = new WKTReader( gf );
			Polygon mapBoundingBox = (Polygon) reader.read(bboxWKT);
			
			// query the index for the bounding box
			List<Polygon> items = index.query(mapBoundingBox.getEnvelopeInternal());
			
			// this should bring out either 1 or none
			Polygon saltLakeCounty = null;
			if( !items.isEmpty() )
			{
				saltLakeCounty = items.get(0);
			}
			
			// if we have a county, then check for contains
			if( saltLakeCounty != null )
			{
				if( saltLakeCounty.contains(mapBoundingBox) )
				{
					geoGreeting = "Congratulations. The map bounds <i>are contained within</i> the boundary of Salt Lake County.";
				}
			}		
			
		} catch (Exception e)
		{
			log.log(Level.WARNING, e.getLocalizedMessage());
		}
		
		return geoGreeting;
	
	}

}
