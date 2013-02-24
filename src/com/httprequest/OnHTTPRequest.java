package com.httprequest;

public interface OnHTTPRequest{
	
    void OnResponseReceived(String str);
    
    void OnResquestError(String str);
}
