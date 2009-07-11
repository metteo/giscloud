package org.emcode.samples.giscloud.server;

import java.util.ArrayList;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Text;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class County {

	@PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private String name;
	
	@Persistent(defaultFetchGroup="true")
	private ArrayList<String> tracts = new ArrayList<String>();
	
	@Persistent
	private Text wkt;
	
	public County()
	{
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Text getWkt() {
		return wkt;
	}

	public void setWkt(Text wkt) {
		this.wkt = wkt;
	}

	public ArrayList<String> getTracts() {
		return tracts;
	}

	public void setTracts(ArrayList<String> tracts) {
		this.tracts = tracts;
	}
    
}
