**UPDATE: The performance issues outlined below were solved by making the index a static member, and doing the heavy lifting in the `HTTPServlet.init()` method. That first hit still takes a few seconds, but subsequent hits drop right back down to a few dozen milliseconds or less.**

## Introduction ##

We are trying to store STRtrees as serialized files on App Engines file system in our WAR's WEB-INF/ directory. The idea is that a spatial query runs faster against an index, rather than collecting all possible entities and querying each one individually.

Unfortunately, the deserializing process is taking a long time in production

## Logic ##

Here is the basic logic:
  * Create a spatial index (STRtree) offline - our example has 509 discrete multipolygons built with the JTS package
  * Serialize it to disk - we modified JTS to be able to serialize the JTS STRtree index
  * Upload the index as a serialized file as part of WEB-INF/ - the serialized index is 1.26MB
  * In a test servlet, grab the serialized index and deserialize it
  * Create a point known to be in the spatial extent covered by polygons in the index
  * Query the point against the index
  * Report on the polygon in which the point lies

## Timing ##

Some basic timing tests were made for the code running in development and on production.

### Development ###

In development (Mac OSX, Google Plugin for Eclipse 3.5, Google App Engine SDK 1.2.5, Google Web Toolkit SDK 1.7.1), our servlet ran in a reasonable amount of time:

```
Begin request

Fetch serialized index
	/galileo/workspace/GisCloud/war/WEB-INF/index.ser
	Creating FileInputStream
	Creating ObjectInputStream
	Elapsed milliseconds to get file: 1

Deserialize index
	Elapsed milliseconds to deserialize index: 1010

Create test point
	Elapsed milliseconds to create test point: 3
	POINT (-111.848953 40.710544)

Query index for point
	Elapsed milliseconds to query index: 1

REPORT:
	The test point was inside multipolygon: 
        MULTIPOLYGON (((-111.843755361566 40.7104458570532, -111.843740604643 40.7098396898185, ... -111.843755361566 40.7104458570532)))
```

Summary for development timing:
  * 1 millisecond to get the serialized file
  * 1 second to deserialize it
  * 3 milliseconds to create a JTS point geometry to test with
  * 1 millisecond to query for the matching polygon


### Development ###

In production our servlet took an inordinate amount of time:

```
Begin request

Fetch serialized index
	/base/data/home/apps/giscloud/beta08.336621203837849506/WEB-INF/index.ser
	Creating FileInputStream
	Creating ObjectInputStream
	Elapsed milliseconds to get file: 29

Deserialize index
	Elapsed milliseconds to deserialize index: 13561

Create test point
	Elapsed milliseconds to create test point: 3
	POINT (-111.848953 40.710544)

Query index for point
	Elapsed milliseconds to query index: 0

REPORT:
	The test point was inside multipolygon: 
        MULTIPOLYGON (((-111.843755361566 40.7104458570532, -111.843740604643 40.7098396898185, ... -111.843755361566 40.7104458570532)))
```

Summary for development timing:
  * 29 milliseconds to get the serialized file (almost 30x as slow)
  * 13.5 seconds to deserialize it (almost 14x as slow)
  * 3 milliseconds to create a JTS point geometry to test with (same as development)
  * <1 millisecond to query for the matching polygon (faster than development)

## Question ##

Why is file access and deserialzing on App Engine so much slower? It doesn't speed up either if I repeatedly hit the same servlet.