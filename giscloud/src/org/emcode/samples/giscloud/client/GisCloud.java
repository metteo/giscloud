package org.emcode.samples.giscloud.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.maps.client.MapType;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.control.LargeMapControl3D;
import com.google.gwt.maps.client.control.MapTypeControl;
import com.google.gwt.maps.client.event.MapClickHandler;
import com.google.gwt.maps.client.event.MapDoubleClickHandler;
import com.google.gwt.maps.client.event.MapDragEndHandler;
import com.google.gwt.maps.client.event.MapZoomEndHandler;
import com.google.gwt.maps.client.event.MapClickHandler.MapClickEvent;
import com.google.gwt.maps.client.geocode.Geocoder;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.LatLngBounds;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class GisCloud implements EntryPoint {
	
	/**
	 * Map elements in the UI
	 */
	private MapWidget map;
	private Geocoder geocoder = new Geocoder();
	private SimplePanel mapWrapper = new SimplePanel();
	
	/**
	 * Other UI elements
	 */
	private static final String applicationTitle = "GIS in the Cloud (App Engine + JTS)";
	private VerticalPanel vp = new VerticalPanel();
	private DialogBox dialogBox;
	private HTML serverResponseLabel;
	private Button closeButton;
	private HTML title;
	private HTML instructions;
	private Label textToServerLabel = new Label();
	
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network "
			+ "connection and try again.";

	/**
	 * Create a remote service proxy to talk to the server-side Greeting service.
	 */
	private final GisCloudServiceAsync gisCloudService = GWT
			.create(GisCloudService.class);
	private HTML codeRepository;
	private HTML poweredBy;

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {

		// an in-page title
		title = new HTML(applicationTitle);
		Window.setTitle(applicationTitle);
		
		title.setStylePrimaryName("title");
		vp.add(title);
		vp.setCellHorizontalAlignment(title, HasHorizontalAlignment.ALIGN_CENTER);
		
		LatLng saltLakeCountyCentroid = LatLng.newInstance(40.649387,-111.928711);
		
	    map = new MapWidget(saltLakeCountyCentroid, 10);
	    map.addMapType(MapType.getHybridMap());
	    map.addMapType(MapType.getSatelliteMap());
	    map.addMapType(MapType.getNormalMap());
	    map.setCurrentMapType(MapType.getNormalMap());
	    map.addControl(new MapTypeControl());

	    map.setSize("700px", "450px");
	    map.addControl(new LargeMapControl3D());
	    
	    /*
	     * Disable double-click for zoom so we can 
	     * use double-click handler for other fun things
	     */
	    map.setDoubleClickZoom(false);
	    
	    /*
	     * Single click on map will send the map's 
	     * center point for WITHIN query on the server
	     */
	    map.addMapClickHandler(new MapClickHandler(){

			@Override
			public void onClick(MapClickEvent event) {
				LatLng point = event.getLatLng();
				map.panTo(point);
				
				// now send point to server as "lng,lat" string
				double lat = point.getLatitude();
				double lng = point.getLongitude();
				
				String lngLat = Double.toString(lng) + "," + Double.toString(lat);
				textToServerLabel.setText(lngLat);
				sendMapCenter(lngLat);
				
			}
	    	
	    });
	    
		/*
		 * Zooming in close enough will put the map viewport
		 * within Salt Lake County. Zooming out too far will
		 * makes the map viewport larger than the county.
		 */
	    map.addMapZoomEndHandler(new MapZoomEndHandler(){

			@Override
			public void onZoomEnd(MapZoomEndEvent event) {
				
				/*
				 * We want a string to make a box, like this:
				 * "lng1 lat1, lng2 lat2, lng3 lat3, lng4 lat4, lng1 lat1"
				 */
				LatLngBounds bounds = map.getBounds();
				LatLng ne = bounds.getNorthEast();
				LatLng sw = bounds.getSouthWest();
				
				double nLat = ne.getLatitude();
				double eLng = ne.getLongitude();
				double sLat = sw.getLatitude();
				double wLng = sw.getLongitude();
				
				/*
				 * Start at ne and go clockwise
				 */
				String nePair = Double.toString(eLng) + " " + Double.toString(nLat);
				String sePair = Double.toString(eLng) + " " + Double.toString(sLat);
				String swPair = Double.toString(wLng) + " " + Double.toString(sLat);
				String nwPair = Double.toString(wLng) + " " + Double.toString(nLat);
				
				/*
				 * Finish bbox back at ne
				 */
				String bbox = nePair + "," + sePair + "," + swPair + "," + nwPair + "," + nePair;
				
				textToServerLabel.setText(bbox);
				sendMapBounds(bbox);
			}
	    	
	    });
	    
	    /*
	     * Let's repeat the zoom handler funtionality as a drag end handler. If
	     * the user is zoomed in, and is within the county, we want to warn them
	     * if they drag the map beyond the edge of the county too.
	     */
	    map.addMapDragEndHandler(new MapDragEndHandler(){

			@Override
			public void onDragEnd(MapDragEndEvent event) {
				
				/*
				 * We want a string to make a box, like this:
				 * "lng1 lat1, lng2 lat2, lng3 lat3, lng4 lat4, lng1 lat1"
				 */
				LatLngBounds bounds = map.getBounds();
				LatLng ne = bounds.getNorthEast();
				LatLng sw = bounds.getSouthWest();
				
				double nLat = ne.getLatitude();
				double eLng = ne.getLongitude();
				double sLat = sw.getLatitude();
				double wLng = sw.getLongitude();
				
				/*
				 * Start at ne and go clockwise
				 */
				String nePair = Double.toString(eLng) + " " + Double.toString(nLat);
				String sePair = Double.toString(eLng) + " " + Double.toString(sLat);
				String swPair = Double.toString(wLng) + " " + Double.toString(sLat);
				String nwPair = Double.toString(wLng) + " " + Double.toString(nLat);
				
				/*
				 * Finish bbox back at ne
				 */
				String bbox = nePair + "," + sePair + "," + swPair + "," + nwPair + "," + nePair;
				
				textToServerLabel.setText(bbox);
				sendMapBounds(bbox);
			}
	    	
	    });
	    
	    
	    // map goes in simple panel with some styling
	    mapWrapper.add(map);
	    mapWrapper.setStylePrimaryName("map");
	    vp.add(mapWrapper);
	    vp.setCellHorizontalAlignment(mapWrapper, HasHorizontalAlignment.ALIGN_CENTER);
	    
	    // style the vp
	    vp.setWidth("100%");
	    vp.setStylePrimaryName("vp");
	    
	    // Create the popup dialog box
		dialogBox = new DialogBox();
		dialogBox.setText("Remote Procedure Call");
		dialogBox.setAnimationEnabled(true);
		closeButton = new Button("Close");
		
		// We can set the id of a widget by accessing its Element
		closeButton.getElement().setId("closeButton");
		textToServerLabel = new Label();
		serverResponseLabel = new HTML();
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Sending coordinates to the server:</b>"));
		dialogVPanel.add(textToServerLabel);
		dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
		dialogVPanel.add(serverResponseLabel);
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
		dialogVPanel.add(closeButton);
		dialogBox.setWidget(dialogVPanel);

		// Add a handler to close the DialogBox
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});
		
		// instructions
		instructions = new HTML("Click to test point-in-polygon. Zoom or drag to test polygon-in-polygon.");
		vp.add(instructions);
		vp.setCellHorizontalAlignment(instructions, HasHorizontalAlignment.ALIGN_CENTER);
		
		// code repository link
		codeRepository = new HTML("Source available at: <a target=\"_blank\" href=\"http://giscloud.googlecode.com\">http://giscloud.googlecode.com</a>");
		vp.add(codeRepository);
		vp.setCellHorizontalAlignment(codeRepository, HasHorizontalAlignment.ALIGN_CENTER);
		
		// powered by logo
		poweredBy = new HTML("<p/><br/><img src=\"http://code.google.com/appengine/images/appengine-silver-120x30.gif\" alt=\"Powered by Google App Engine\" />");
		vp.add(poweredBy);
		vp.setCellHorizontalAlignment(poweredBy, HasHorizontalAlignment.ALIGN_CENTER);
		
		
	    RootPanel.get().add(vp);
	    
	}
	
	protected void sendMapBounds(String bbox) {
		gisCloudService.polygonInPolygon(bbox,
				new AsyncCallback<String>() {
					public void onFailure(Throwable caught) {
						// Show the RPC error message to the user
						dialogBox
								.setText("Remote Procedure Call - Failure");
						serverResponseLabel
								.addStyleName("serverResponseLabelError");
						serverResponseLabel.setHTML(SERVER_ERROR);
						dialogBox.center();
						closeButton.setFocus(true);
					}

					public void onSuccess(String result) {
						dialogBox.setText("Java Topology Suite on AppEngine Says...");
						serverResponseLabel
								.removeStyleName("serverResponseLabelError");
						serverResponseLabel.setHTML(result);
						dialogBox.center();
						closeButton.setFocus(true);
					}
				});
	}

	protected void sendMapCenter(String lngLat) {
		gisCloudService.pointInPolygon(lngLat,
				new AsyncCallback<String>() {
					public void onFailure(Throwable caught) {
						// Show the RPC error message to the user
						dialogBox
								.setText("Remote Procedure Call - Failure");
						serverResponseLabel
								.addStyleName("serverResponseLabelError");
						serverResponseLabel.setHTML(SERVER_ERROR);
						dialogBox.center();
						closeButton.setFocus(true);
					}

					public void onSuccess(String result) {
						dialogBox.setText("Java Topology Suite on AppEngine Says...");
						serverResponseLabel
								.removeStyleName("serverResponseLabelError");
						serverResponseLabel.setHTML(result);
						dialogBox.center();
						closeButton.setFocus(true);
					}
				});
	}
}
