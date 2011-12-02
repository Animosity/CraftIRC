package com.ensifera.animosity.craftirc;

public interface SecuredEndPoint extends EndPoint {

	/*
	 * Unsecured: Paths can be established automatically by auto-paths (default if EndPoint or BasePoint are used)
	 * Require paths: Paths must be defined manually in config.yml, but automatic targetting can be used (as in CraftIRC v3.0)
	 * Require target: Paths must be defined manually in config.yml and messages must be specifically targetted at this endpoint.
	 */
	public enum Security {
		UNSECURED,
		REQUIRE_PATH,
		REQUIRE_TARGET
	}
	
	public Security getSecurity();
	
}
