/* 
 * File HTTPRequest.java, 
 * brief: Class to send HTTP requests.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Simple class to send HTTP request using both methods GET and POST. 
 * 
 * @author Rodrigo Braga <rodrigogrow@gmail.com>
 */
public class HTTPRequest {

	private String LOG_TAG = "HTTPRequest";

	private String URLServer;
	private Context context;
	private int connectionTimeout = 0; 
	private int socketTimeout = 0;

	public enum Methods {
		GET, POST
	}
	
	public HTTPRequest(Context ctx) {
		context = ctx;
	}

	/**
	 * Sets the server url.
	 * @param url an absolute of HTTP server. 
	 */
	public void setUrlServer(String url) {
		Log.i(LOG_TAG, "setUrlServer("+url+")");

		URLServer = url;
	}

	/**
	 * Sets the timeout until a connection is established. 
	 * A value of zero means the timeout is not used.
	 * 
	 * @param msec timeout in milliseconds. 
	 */
	public void setConnectionTimeOut(int msec) {
		Log.i(LOG_TAG, "setConnectionTimeOut("+msec+")");

		connectionTimeout = msec;		
	}

	/**
	 * Sets the socket timeout (timeout for waiting for data).
	 * A timeout value of zero is interpreted as an infinite timeout.
	 * 
	 * @param msec timeout in milliseconds. 
	 */
	public void setSocketTimeOut(int msec) {
		Log.i(LOG_TAG, "setSocketTimeOut("+msec+")");

		socketTimeout = msec;		
	}	

	/**
	 * Sends request to HTTP server (GET or POST) 	
	 * 	
	 * The Synchronous request only works in SDK 10 or lower. Otherwise it 
	 * throw the exception when an application attempts to perform a networking operation 
	 * on its main thread.
	 * 
	 * @param method  request method (GET or POST).
	 * @param params  set of pairs <key, value>, fields.
	 * @param isAsync true for a asynchronous request.
	 * @return a string response in synchronous request, otherwise null. 
	 */
	public String sendRequest(Methods method, final JSONObject params, boolean isAsync) {
		Log.i(LOG_TAG, "sendRequest("+method+", "+isAsync+")");

		if (isAsync == true) {
			new HTTPAsyncRequest(method, (OnHTTPRequest)context).execute(params);
			return null;
		} else {			
			return HTTPSyncRequest(method, params, (OnHTTPRequest)context);
		}
	}

	/**
	 * Converts JSON object to List (Value Pair). 
	 * 
	 * @param obj 
	 * @return the list(value pair) of parameters to POST request.
	 * @throws JSONException
	 */
	private List<NameValuePair> jsonToList(JSONObject obj) throws JSONException { 
		Log.i(LOG_TAG, "jsonToList()");

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

		Iterator<?> iter = obj.keys();
		while(iter.hasNext()){
			String key = (String)iter.next();
			String value = obj.getString(key);

			nameValuePairs.add(new BasicNameValuePair(key, value));	        
		}

		return nameValuePairs;
	}

	/**
	 * Returns the content into HTTP response. 
	 * 
	 * @param response HTTP response 
	 * @return the string with content.
	 * @throws IllegalStateException 
	 * @throws IOException
	 */	
	private String getHTTPResponseContent(HttpResponse response) throws IllegalStateException, IOException {
		Log.i(LOG_TAG, "getHTTPResponseContent()");

		InputStream instream = response.getEntity().getContent();

		BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				instream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	/**
	 * Send synchronous request to HTTP server.
	 * 
	 * @param method  request method (GET or POST).
	 * @param params set of pairs <key, value>, fields.
	 * @param listenr interface (callback) to bind to external classes.
	 * @return response of HTTP Server.
	 */
	private String HTTPSyncRequest (Methods method, JSONObject params, OnHTTPRequest listenr) {
		Log.i(LOG_TAG, "HTTPSyncRequest("+method+")");		

		List<NameValuePair> requestParams = null;
		HttpRequestBase httpRequest = null;
		OnHTTPRequest listener = listenr;		

		try {
			requestParams = jsonToList(params);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		// Set parameters of HTTP request.
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeout);
		HttpConnectionParams.setSoTimeout(httpParameters, socketTimeout);

		// Create a new HttpClient and Header
		HttpClient httpclient = new DefaultHttpClient(httpParameters);		

		if (method == Methods.GET) {
			httpRequest = new HttpGet(URLServer + "?" +  URLEncodedUtils.format(requestParams, "utf-8"));
		} else {
			httpRequest = new HttpPost(URLServer);

			// Add data to request
			try {
				((HttpPost) httpRequest).setEntity(new UrlEncodedFormEntity(requestParams));
			} catch (UnsupportedEncodingException e) {
				listener.OnResquestError(e.getMessage().toString());
				e.printStackTrace();
			}
		}				

		try {			
			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httpRequest);

			Log.i(LOG_TAG,"Code: "+response.getStatusLine().getStatusCode());

			// Response
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return getHTTPResponseContent(response);				
			} else {
				listener.OnResquestError("Server Error");
			}

		} catch (SocketTimeoutException e){
			listener.OnResquestError("Socket Timeout" + e.getMessage().toString());
			Log.e(LOG_TAG, "Socket Timeout", e);			
		} catch (ConnectTimeoutException e) {
			listener.OnResquestError("Connect Timeout" + e.getMessage().toString());
			Log.e(LOG_TAG, "Connect Timeout", e);
		} catch (ClientProtocolException e) { 
			listener.OnResquestError("HTTP Error: " + e.getMessage().toString());
			Log.e(LOG_TAG, "HTTP Error", e);
		} catch (IOException e) {
			listener.OnResquestError("Connection Error: " + e.getMessage().toString());
			Log.e(LOG_TAG, "Connection Error", e);
		}

		return null;		
	}

	/**
	 * Class to send asynchronous request to HTTP server.
	 * In constructor method, is possible to pass interface (callback) 
	 * to bind request events.
	 */
	public class HTTPAsyncRequest extends AsyncTask<Object, Object, Object> { 

		Methods method;
		private OnHTTPRequest listener;		

		public HTTPAsyncRequest(Methods method, OnHTTPRequest listener){
			this.method = method;
			this.listener=listener;			
		}

		@Override
		protected String doInBackground(Object... params) {
			Log.i(LOG_TAG, "HTTPAsyncRequest("+this.method+")");

			return HTTPSyncRequest(this.method, (JSONObject)params[0], this.listener);
		}

		@Override
		protected void onPostExecute(Object result) {

			if (result != null) {
				listener.OnResponseReceived(result.toString());
			}
		}
	}
}
