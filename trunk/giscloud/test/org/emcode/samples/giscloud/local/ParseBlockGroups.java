package org.emcode.samples.giscloud.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Logger;

import org.emcode.samples.giscloud.server.BlockGroup;
import org.junit.Test;

import com.Ostermiller.util.CSVParse;
import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import com.google.appengine.api.datastore.Text;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.io.WKTReader;


public class ParseBlockGroups {

	private static final Logger log = Logger.getLogger(ParseBlockGroups.class.getName());
	private GeometryFactory gf;
	
	
	@Test
	public void testThis()
	{
		log.info("this is a message");
	}
	
	@Test
	public void testParseBlockGroups()
	{
		// open the file
		File file = new File("/galileo/workspace/GisCloud/test/org/emcode/samples/giscloud/local/blockgroups.csv");
		log.info(file.toString());
		CSVParse csvParse = null;
		
		// set up an STRtree
		STRtree index = new STRtree();
		
		try {
			
			csvParse = new CSVParser(new FileInputStream(file));
			LabeledCSVParser parser = null;
			parser = new LabeledCSVParser(csvParse);
			String[] labels = parser.getLabels();
			
			StringBuffer buffer = new StringBuffer();
			for(String label : labels)
			{
				buffer.append(label+"\t");
			}
			System.out.println(buffer.toString());
			System.out.println("-----------------------------------------------------------");
			while(parser.getLine() != null)
			{
				buffer = new StringBuffer();
				buffer.append(parser.getValueByLabel("stfid"));
				buffer.append("\t"+parser.getValueByLabel("tapersons"));
				buffer.append("\t"+parser.getValueByLabel("wkt"));
				// System.out.println(buffer.toString());
				
				String stfid = parser.getValueByLabel("stfid");
				Integer tapersons = Integer.parseInt(parser.getValueByLabel("tapersons"));
				String wktString = parser.getValueByLabel("wkt");
				
				
				// create a new BlockGroup
				BlockGroup bg = new BlockGroup();
				bg.setStfid(stfid);
				bg.setTapersons(tapersons);
				Text wkt = new Text(wktString);
				bg.setWkt(wkt);
				
				log.info(bg.toString());
				
				// now create a polygon
				MultiPolygon bgPoly = null;
				gf = new GeometryFactory();
				WKTReader reader = new WKTReader( gf );
				bgPoly = (MultiPolygon) reader.read(wkt.getValue());
				
				log.info(bgPoly.toString());
				
				// insert th poly into the index
				index.insert(bgPoly.getEnvelopeInternal(), bgPoly);
				
			}
			
			// building the index
			log.info("building the index");
			index.build();
			
			
			// now catch the size of the index
			log.info("size of the index");
			log.info(Integer.toString(index.size()));
			
			// serialize it?
			log.info("serialize the index to disk");
			FileOutputStream fout = new FileOutputStream("/galileo/workspace/GisCloud/test/org/emcode/samples/giscloud/local/index.ser");
		    ObjectOutputStream oos = new ObjectOutputStream(fout);
		    oos.writeObject(index);
		    oos.close();

			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		
		
	}
}
