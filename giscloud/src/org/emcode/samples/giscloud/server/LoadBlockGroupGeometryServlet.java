package org.emcode.samples.giscloud.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Text;
import com.vividsolutions.jts.geom.GeometryFactory;

import com.Ostermiller.util.CSVParse;
import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;

public class LoadBlockGroupGeometryServlet extends HttpServlet {

	private static final long serialVersionUID = 6415720682795847282L;
	private GeometryFactory gf = new GeometryFactory();
	
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
    	
    	resp.getWriter().println("\nLoading block groups from CSV");
    	File file = new File("WEB-INF/blockgroups.csv");
    	FileInputStream fis = new FileInputStream(file);
    	CSVParse csvParse = null;
    	
    	try
    	{
    		csvParse = new CSVParser(fis);
    		LabeledCSVParser parser = null;
			parser = new LabeledCSVParser(csvParse);
			
			StringBuffer buffer = new StringBuffer();
			
			while(parser.getLine() != null)
			{
				// pull out the salient information
				String stfid = parser.getValueByLabel("stfid");
				Integer tapersons = Integer.parseInt(parser.getValueByLabel("tapersons"));
				String wktString = parser.getValueByLabel("wkt");
				
				// create a new Block Group
				BlockGroup blockGroup = new BlockGroup();
				blockGroup.setStfid("BG" + stfid);
				blockGroup.setTapersons(tapersons);
				Text wkt = new Text(wktString);
				blockGroup.setWkt(wkt);
				
				resp.getWriter().println(blockGroup);
				
				// now, make it persistent
				PersistenceManager pm = PMF.get().getPersistenceManager();
				try {
				    pm.makePersistent(blockGroup);
				} finally {
				    pm.close();
				}				
				
			}
			
    	} catch (Exception e) {
    		resp.getWriter().println(e.toString());
		}
    	
    	
    	resp.getWriter().println("Stopped at: " + Long.toString(System.currentTimeMillis()));
		
    }
}
