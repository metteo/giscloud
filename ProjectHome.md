The **GIS in the Cloud (App Engine + JTS)** reference project is now available at http://giscloud.appspot.com

### Updates ###

**NEW! Beta 0.9** The spatial index is stored in Memcache and re-used from there rather than going to disk each time and deserializing it.

**Beta 0.8** Spatial indexes are working in GIS in the Cloud beta 0.8. See SerializedSpatialIndexes. Beta 0.8 uses deserializes a modified JTS STRtree from disk for faster spatial queries.

### Motivation ###

While many GIS/spatial App Engine discussions center on the python SDK (with pre-computing grids or geocells and heavy lifting done with set membership in the datastore), this reference project uses the Java Topology Suite (http://www.vividsolutions.com/jts/jtshome.htm) within the App Engine server to demonstrate point-in-polygon and polygon-in-polygon spatial queries.

The **GIS in the Cloud** app is pure Java and was built with

  * Google App Engine SDK for Java
  * Google Plugin for Eclipse
  * Google Web Toolkit
  * Google Web Toolkit API Libraries - Google Maps 1.0 Library
  * Java Topology Suite

The Java Topology Suite was ported to C++ and became GEOS, which was embedded in PostgreSQL to become PostGIS, allowing users access to spatial functions within SQL. While App Engine does not give us spatial functions in GQL, the **GIS in the Cloud** app will demonstrate a GQL/JTS combination to accomplish the same effect.

The current version of the application demonstrates use of the datastore and employs JTS to query point-in-polygon and polygon-in-polygon. Single clicks on the map trigger the point-in-polygon test (the point is the map center and the polygon is Salt Lake County). With zoom or drag, the polygon-in-polygon test is triggered. The first polygon is the map bounds and the second is Salt Lake County.
