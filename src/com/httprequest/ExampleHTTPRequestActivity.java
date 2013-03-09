/* 
 * File ExampleHTTPRequestActivity.java, 
 * brief: Example how to use HTTP request on Android.
 *
 * Copyright (C) 2013  Rodrigo de Souza Braga <rodrigogrow@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.httprequest;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;

public class ExampleHTTPRequestActivity extends Activity implements OnHTTPRequest{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_example_httprequest);
		
		exampleHTTPGETAsync();
	}
	
	private void exampleHTTPGETAsync() {
		
		JSONObject params = null;
		
		try {
		params = new JSONObject();
		params.put("hl", "pt-BR");
		params.put("output", "search");
		params.put("q", "teste");
		} catch (JSONException e) {
            e.printStackTrace();
        }
		
		HTTPRequest hc;
		
		// Create new instace, need to pass the current context.
		hc = new HTTPRequest(this);
		
		// Set HTTP Server url.
		hc.setUrlServer("http://www.google.com");
		
		// Send HTTP GET request in asynchonous way.
		hc.sendRequest(HTTPRequest.Methods.GET, params, true);
	}

	@Override
	public void OnResponseReceived(String str) {
		
		Log.i("Handle Async: ", str );
	}

	@Override
	public void OnResquestError(String str) {
		
		Log.i("Handle Error Request: ", str );
	}
	
}
