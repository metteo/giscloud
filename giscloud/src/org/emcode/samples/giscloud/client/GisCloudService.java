package org.emcode.samples.giscloud.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("greet")
public interface GisCloudService extends RemoteService {
	String pointInPolygon(String lngLat);
	String polygonInPolygon(String bbox);
}
