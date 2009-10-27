import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.logging.Logger;

import org.junit.Test;

import com.vividsolutions.jts.index.strtree.STRtree;

import junit.framework.TestCase;


public class SerializeSTRTree extends TestCase {

	private static final Logger log = Logger.getLogger(SerializeSTRTree.class.getName());

	
	@Test
	public void testSTRtree()
	{
		STRtree strTree = new STRtree();
		log.info(strTree.toString());
		
	}
	
	@Test
	public void testSerializeSTRtree()
	{
		STRtree strTree = new STRtree();
		log.info(strTree.toString());
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		log.info("bos=" + bos);
		
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			log.info("oos=" + oos);
			
			oos.writeObject(strTree);
			
		} catch (NotSerializableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		
		
	}
	
}
