package org.emcode.samples.giscloud.server;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Text;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class BlockGroup {

	@PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private String stfid;
	
	@Persistent
	private Integer tapersons;
	
	@Persistent
	private Text wkt;
	
	public BlockGroup()
	{
		
	}

	public String getStfid() {
		return stfid;
	}

	public void setStfid(String stfid) {
		this.stfid = stfid;
	}

	public Integer getTapersons() {
		return tapersons;
	}

	public void setTapersons(Integer tapersons) {
		this.tapersons = tapersons;
	}

	public Text getWkt() {
		return wkt;
	}

	public void setWkt(Text wkt) {
		this.wkt = wkt;
	}

	@Override
	public String toString() {
		String out = "[blockgroup";
		out += "[stfid="+this.getStfid()+"]";
		out += "[tapersons="+this.getTapersons().toString()+"]";
		out += "[wkt="+this.getWkt()+"]";
		out += "]";
		return out;
	}
    
}
