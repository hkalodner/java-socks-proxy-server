package org.princeton.btsocks.discovery;

public enum TrackerAction {
	CONNECT(0), ANNOUNCE(1), SCRAPE(2);
	
	private final int numericValue;
	
	TrackerAction(int value) {
		this.numericValue = value;
	}
	
	public int getNumericValue() {
		return this.numericValue;
	}
	
	public static TrackerAction valueOf(int value) {
	    switch(value) {
	        case 0: return CONNECT;
	        case 1: return ANNOUNCE;
	        case 2: return SCRAPE;
	        default: return CONNECT;
	    }
	}
}
