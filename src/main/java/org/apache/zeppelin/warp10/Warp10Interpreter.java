//
//   Copyright 2016  Cityzen Data
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package org.apache.zeppelin.warp10;

import org.json.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.apache.zeppelin.display.AngularObject;
import org.apache.zeppelin.display.AngularObjectRegistry;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterPropertyBuilder;
import org.apache.zeppelin.interpreter.InterpreterResult;
/**
 * Interpreter Warp10 for Zeppelin
 * 
 */
public class Warp10Interpreter extends Interpreter
{
  
  //
  // Private Pair class
  //
  
  private class Pair<F, S> {
    private F first;
    private S second;
    
    public Pair(F first, S second) {
      super();
      this.first = first;
      this.second = second;
    }
    
  }
  static final String DEFAULT_URL = "http://localhost:8080/api/v0";
  static final String URL_KEY = "warp10.url";
  String current_Url;

  private final HashMap<String, Properties> propertiesMap;

  public Warp10Interpreter(Properties property) {
    super(property);
    //
    propertiesMap = new HashMap<>();
  }

  public HashMap<String, Properties> getPropertiesMap() {
    return propertiesMap;
  }

  static {
    Interpreter.register("warpscript", Warp10Interpreter.class.getName());
    new InterpreterPropertyBuilder().add(URL_KEY, 
	DEFAULT_URL, "The URL for Warp10.");
  }

  @Override
  public void cancel(InterpreterContext arg0) {
    //

  }

  @Override
  public void close() {

  }

  @Override
  public List<String> completion(String arg0, int arg1) {
    //
    return null;
  }

  @Override
  public FormType getFormType() {
    //
    return FormType.SIMPLE;
  }

  //@Override
  public int getProgress(InterpreterContext arg0) {
    //
    return 0;
  }

  //@Override
  //
  // When the result of the Warpscript contains a Map on top of the stack
  // then all the tuples key,value are saved in Angular variables.
  // To ensure a Map on top of the stack the Warpscipt function EXPORT can be used
  // When using Angular to save variable NaN and Infinity are transformed in String !
  //
  //TODO add something to deactivate the SAVE
  public InterpreterResult interpret(String body, InterpreterContext context) {

    //
    // Load the Angular Registry to load all globals variable already defined
    // Create a Map associating their name with their value
    //
    
    AngularObjectRegistry registry = context.getAngularObjectRegistry();
    List<AngularObject> liAngularObjects = registry.getAllWithGlobal(context.getNoteId());
    Map<String, Object> mapAngularObjects = new HashMap<>();
    for (AngularObject angularObject : liAngularObjects) {
      mapAngularObjects.put(angularObject.getName(),angularObject.get());
    }
    
    //
    // Initialize an empty String and check if the first body line contains //import
    //
    
    String toSend = "";
    String bodyLine[] = body.split("\n");
    if(bodyLine.length >= 1) {
      if (bodyLine[0].startsWith("//import"))
      {
	String[] varToImport = bodyLine[0].split("\\s+"); 
	
	//
	// In this case load all variable contained in this line and add store them in Warpscript
	// 
	
	for (String variable : varToImport) {
	  if(mapAngularObjects.containsKey(variable)){
	    String value = getVariable(mapAngularObjects.get(variable));
	    String warpscript = value + "'" + variable + "' " + "STORE ";
	    toSend += warpscript + "\n";
	  }
	}
      }
    }
    
    //
    // Append wrpscript header with body
    //
    
    toSend += body;
    try {
      
      // 
      // Execute the request on Warpscipt if 200 then first equals SUCCESS else ERROR, second equals message body
      //
      
      Pair<InterpreterResult.Code, String> pairResult = this.execRequest(toSend);
      //int i = -1;
      
      //
      // If request is a Success, and first element of the result a Json Map
      //
      if(pairResult.first == InterpreterResult.Code.SUCCESS) {
	JSONArray result = new JSONArray(pairResult.second);
	if (result.length() >= 1) {
	  String firstItem = result.get(0).toString();
	  if (isMapJSONValid(firstItem)) {
	    JSONObject object = new JSONObject(firstItem);
	    Set<String> keys = object.keySet();
	    
	    //
	    // Add all key value of this map in the angular registry
	    //
	    
	    for (String key : keys) {
	      
	      registry.add(key, object.get(key), context.getNoteId(), null);
	      }
	  }
	}
	//JSONArray arr = getJSONArray(pairResult.second);
      }   
      
      //
      // Return the expected result
      //
      
      return new InterpreterResult(pairResult.first, pairResult.second);
    } catch (Exception e) {
      
      //
      // If an exception append return its message an ERROR code 
      //
      
      return new InterpreterResult(InterpreterResult.Code.ERROR, e.getMessage());
    }
    //return this.current_Url;
  }

  private String getVariable(Object object) {
    System.out.println(object.getClass().toString());
    if (object instanceof String) {
      return "'" + object + "' ";
    } else if (object instanceof JSONArray){
      //JSONArray json = new JSONArray(object); 
      //List<Object> l = (List<Object>) object;
      return "'" + object.toString() + "'" +  " JSON-> ";
    } else if (object instanceof JSONObject){
      //JSONObject json = new JSONObject(object); 
      //List<Object> l = (List<Object>) object;
      return "'" + object.toString() + "'" +  " JSON-> ";
    } else {
      return object + " ";
    }
  }

  /**
   * Function to test if a String is a Valid JSON Map
   * @param test String to test
   * @return
   */
  public boolean isMapJSONValid(String test) {
    try {
        new JSONObject(test);
    } catch (JSONException ex) {
        return false;
    }
    return true;
}
  
  /**
   * Function to test if a String is a Valid JSON List
   * @param test String to test
   * @return
   */
  public boolean isListJSONValid(String test) {
    try {
        new JSONArray(test);
    } catch (JSONException ex) {
        return false;
    }
    return true;
}
  //@Override
  public void open() {
  
    //
    // Load the property URL KEY, if defined reach this URL otherwise default URL
    //
    
    final String keyValue = getProperty(URL_KEY);
    if (null != keyValue) {
      this.current_Url = keyValue;
    } else {
      this.current_Url = DEFAULT_URL;
    }
    /*for (String propertyKey : property.stringPropertyNames()) {
      logger.debug("propertyKey: {}", propertyKey);
      String[] keyValue = propertyKey.split("\\.", 2);
      if (2 == keyValue.length) {
        Properties prefixProperties;
        if (propertiesMap.containsKey(keyValue[0])) {
          prefixProperties = propertiesMap.get(keyValue[0]);
        } else {
          prefixProperties = new Properties();
          propertiesMap.put(keyValue[0], prefixProperties);
        }
        prefixProperties.put(keyValue[1], property.getProperty(propertyKey));
      }
    }*/
  }

  public Pair<InterpreterResult.Code, String> execRequest(String body) throws Exception {
    
    //
    // Execute the request on current url defined
    //
    
    String url = this.current_Url;
    url += "/exec";
    URL obj = new URL(url);
    HttpURLConnection con = null;
    
    //
    // If HTTPS execute an HTTPS connection
    //
    
    if (url.startsWith("https")) {
      con = (HttpsURLConnection) obj.openConnection();
    } else {
      con = (HttpURLConnection) obj.openConnection();
    }

    //add request header
    con.setDoOutput(true);
    con.setDoInput(true);
    con.setRequestMethod("POST");
    con.setChunkedStreamingMode(16384);
    con.connect();

    //
    // Write the body in the request
    //
    
    OutputStream os = con.getOutputStream();
    //GZIPOutputStream out = new GZIPOutputStream(os);
    PrintWriter pw = new PrintWriter(os);  
    pw.println(body);
    pw.close();

    StringBuffer response = new StringBuffer();
    Pair<InterpreterResult.Code, String> resultPair = null;
    
    //
    // If answer equals 200 parse result stream, otherwise error Stream
    //
    
    if (200 == con.getResponseCode()) {
      BufferedReader in = new BufferedReader(
	  new InputStreamReader(con.getInputStream()));
      String inputLine;

      while ((inputLine = in.readLine()) != null) {
	response.append(inputLine);
      }
      resultPair = new Pair<InterpreterResult.Code, String>(InterpreterResult.Code.SUCCESS, response.toString());
      in.close();
      con.disconnect();
    } else {
      String errorLine = "\"Error-Line\":" + con.getHeaderField("X-Warp10-Error-Line");
      String errorMsg = "\"Error-Message\":\"" + con.getHeaderField("X-Warp10-Error-Message")+ "\"";
      response.append("[{");
      response.append(errorLine + ",");
      response.append(errorMsg);
      boolean getBody = (null == con.getContentType());
      if(!getBody && ! con.getContentType().startsWith("text/html")) {
	getBody = true;
      }
      if (getBody) {
	response.append(",\"Body\":\"");
        BufferedReader in = new BufferedReader(
  	  new InputStreamReader(con.getErrorStream()));
        String inputLine;
  
        while ((inputLine = in.readLine()) != null) {
  	response.append(inputLine);
        }
        in.close();
        response.append("\"");
      }
      response.append("}]");
      resultPair = new Pair<InterpreterResult.Code, String>(InterpreterResult.Code.ERROR, response.toString());
      con.disconnect();
    }
    
    //
    // Return the body message with its associated code (SUCESS or ERROR)
    //
    
    return resultPair;
  }
}
