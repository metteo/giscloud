# Introduction #

**UPDATED 28 Sep 2009** The reference application now uses a serialized STRtree spatial index containing the JTS geometry for Salt Lake County. This data retrieval method is now live on the reference application instead of the WKT process outlined below

The [GisCloud](http://giscloud.appspot.com) reference application is currently storing the polygon for Salt Lake County as Well-Known Text (WKT). For the existing application where we are simply testing a single point-in-polygon and polygon-in-polygon, this is acceptable. However, if we need to do this for many geometries (e.g. if we were testing a point in many tracts instead of a point in a single county) then fetching each WKT, converting to geometry, and either looping through each to test for inclusion or building a spatial index, just takes too long -- especially if the user is clicking to generate the activity.

However, since we don't have access to a file system on App Engine we must store spatial indexes as binary objects (blobs) in the datastore. And all objects must be serializable to be persisted. Thus, the fix we need in JTS is for spatial indexes to be serializable.

# Details #

While it is easy enough to add `implements Serializable` to classes and `extends Serializable` to interfaces in the `com.vividsolutions.jts.index.*` packages, it took a little digging to see that the `STRtree` (the only class we have tested for now) implements a few comparators that need some work.

## Current comparator code (found in STRtree) ##

```
    // a comparator for the x coordinate
    private Comparator xComparator =
    new Comparator() {
      public int compare(Object o1, Object o2) {
        return compareDoubles(
            centreX((Envelope)((Boundable)o1).getBounds()),
            centreX((Envelope)((Boundable)o2).getBounds()));
      }
    };

    // a comparator for the y coordinate
    private Comparator yComparator =
    new Comparator() {
      public int compare(Object o1, Object o2) {
        return compareDoubles(
            centreY((Envelope)((Boundable)o1).getBounds()),
            centreY((Envelope)((Boundable)o2).getBounds()));
      }
    };
```

## New comparator code (our modification of STRtree) ##

The current inline comparators are replaced with two new classes:

```
    private BoundableXComparator xComparator = new BoundableXComparator();
    private BoundableYComparator yComparator = new BoundableYComparator();
```

which look like this:

```
package com.vividsolutions.jts.index.strtree;

import com.vividsolutions.jts.geom.Envelope;

public class BoundableXComparator extends BoundableComparator {

	public int compare(Boundable o1, Boundable o2) {
        return compareDoubles(
                centreX((Envelope)((Boundable)o1).getBounds()),
                centreX((Envelope)((Boundable)o2).getBounds()));
	}
	
}
```

and

```
package com.vividsolutions.jts.index.strtree;

import com.vividsolutions.jts.geom.Envelope;

public class BoundableYComparator extends BoundableComparator {

	public int compare(Boundable o1, Boundable o2) {
        return compareDoubles(
                centreY((Envelope)((Boundable)o1).getBounds()),
                centreY((Envelope)((Boundable)o2).getBounds()));
	}

}
```

Both the X and Y comparators inherit from:

```
package com.vividsolutions.jts.index.strtree;

import java.io.Serializable;

/**
 * A spatial object in an AbstractSTRtree.
 *
 * @version 1.7
 */
public interface Boundable extends Serializable {
  /**
   * Returns a representation of space that encloses this Boundable, preferably
   * not much bigger than this Boundable's boundary yet fast to test for intersection
   * with the bounds of other Boundables. The class of object returned depends
   * on the subclass of AbstractSTRtree.
   * @return an Envelope (for STRtrees), an Interval (for SIRtrees), or other object
   * (for other subclasses of AbstractSTRtree)
   * @see AbstractSTRtree.IntersectsOp
   */
  Object getBounds();
}
```

which existed before in the JTS code base, but which now extends Serializable.

## Implementation in App Engine ##

Since an STRtree is binary, we can only store it as a `Blob` in App Engine. But the Blob class is final, so we don't get to extend it to make our own Blob type. The workaround is to create a wrapper, which we call `SpatialIndexBlob`:

```
package org.emcode.samples.giscloud.server;

import java.io.Serializable;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Blob;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class SpatialIndexBlob {

	@PrimaryKey
	@Persistent
	private String name;
	
	@Persistent
	private Blob blob;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Blob getBlob() {
		return blob;
	}
	public void setBlob(Blob blob) {
		this.blob = blob;
	}
	
}
```

Now, when we want to create an STRtree with geometry that is originally WKT then store it in App Engine, we follow this idea:

```

// create geometry from well known text
slcountyWKT = "...." // query from database
WKTReader reader = new WKTReader( gf );
Polygon slcounty = null;
try {
	slcounty = (Polygon) reader.read(slcountyWKT);
} catch (ParseException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}

// create a tree
STRtree strTree = new STRtree();

// insert the polygon (in this case, only one, but obviously it can hold more)
strTree.insert(slcounty.getEnvelopeInternal(), slcounty);

// build the index
strTree.build();

// set up the blob for storage
ByteArrayOutputStream bos = new ByteArrayOutputStream();
ObjectOutputStream oos = new ObjectOutputStream(bos);
oos.writeObject(index);
byte[] bArray = bos.toByteArray();
Blob blob = new Blob(bArray); // Blob is an App Engine type for binary objects
SpatialIndexBlob sBlob = new SpatialIndexBlob(); // this is our custom wrapper
sBlob.setName("SLCOUNTYINDEX"); // make it easy to fetch it again
sBlob.setBlob(blob);

// store our spatial index blob
PersistenceManager pm = PMF.get().getPersistenceManager();
try {
    pm.makePersistent(sBlob);
} finally {
    pm.close();
}

```

# TODO #

This modification permits us to save spatial indexes in the App Engine datastore, and we have just tested reading them back out and using them in a spatial query. It's really just a start. Other tasks that we need to do are:

  * Test indexes with more elements. Observe size and read performance.

  * Create indexes with TaskQueue (e.g. query all counties, fetch their WKT, make polygons, shove the polygons in an STRtree, build the index, store the index in the datastore, all in the background with an App Engine TaskQueue). In beta 0.8 the serialized index was created offline and uploaded in WEB-INF/ as a resource file.

  * Consider splitting large indexes across multiple `SpatialIndexBlob`s. It is likely that some indexes created with a TaskQueue will be larger than the 10MB blob limit in App Engine. With a bit of work we can make a `SpatialIndexBlob` know what order it is in a list, and just create multiple `SpatialIndexBlob`s with the same name. When we query for all them, we suck out their byte arrays and stitch them back together. Remember we used to split .zip files across multiple floppy disks? Hopefully Google will catch on and take us out of the "floppy disk" limit for blobs and into something more appealing (like 100MB Zip(R) drives... or 128M USB drives...)

  * Implement serialization for the other spatial indexes in JTS