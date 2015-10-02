package spider.form.html;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.Map;

import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.EventHandler;
import javafx.event.EventType;


/**
 * Class for loading form html.
 * 
 * Code example usage:
 <pre>
String cont1 = 	"D:/my/Proj_Spider/03_Workspace/99_BlankSpace/test-fx-webview/small.html";			
String cont2=
		"&lt;html&gt;\n"
		+"&lt;body&gt;\n"
		+"&lt;p id='lable'&gt;Normal&lt;/p&gt;\n"
		+"&lt;script&gt;\n"
		+"window.status = 'MY-MAGIC-VALUE';\n"
		+"window.status = '';\n"
		+"document.getElementById('lable').innerHTML='Hello ' + guest;\n"
		+"form.foutput.put('hello_from_web', 'Hello from Web view!');\n"
		+"&lt;/script&gt;\n"
		+"&lt;/body&gt;\n"
		+"&lt;/html&gt;\n"
		;				
HtmlForm form = new HtmlForm(cont1)
	.setTitle("form 1")
	.putData("guest", "Doc Truong")
	.show();
System.out.println("hello_from_web:"+form.getData("hello_from_web"));	
</pre>  	
    	
 * @author pika
 * 
 * v1.0 - 2-Oct-2015
 */

public class HtmlForm extends Application implements HtmlFormWebInf{


	private static WebView webView;
	private static WebEngine webEngine;
	private static String url_code =	"";

	private static int loadByFile = -1; //-1: uninit, 1: true, 0: false
	private static String title = null;
	/*****************************************************************************
	 * Create blank form, content loaded later by load()
	 */
	public HtmlForm() { }	
	
	/*****************************************************************************
	 * Create form from url file/html code implicitly
	 * @param content
	 */
	
	public HtmlForm(String content) {
		url_code = content;
		loadByFile = -1;
	}
	
	/*****************************************************************************
	 * Create form from url file/html code explicitly
	 * @param content
	 * @param bloadByFile -<code>true</code> will load as file,
	 * 					<code>false</code> will load as html code string
	 */
	
	public HtmlForm(String content, boolean bloadByFile) {
		url_code =content;
		loadByFile = (bloadByFile)? 1 : 0;
	}

	/*****************************************************************************
	 * Create form from url file/html code explicitly
	 * @param title
	 * @param content
	 * @param bloadByFile -<code>true</code> will load as file,
	 * 					<code>false</code> will load as html code string
	 */
	
	public HtmlForm(String title, String content, boolean bloadByFile) {
		this.title = title;
		url_code =content;
		loadByFile = (bloadByFile)? 1 : 0;
	}
	
	public HtmlForm setTitle(String title){
		this.title = title;
		return this;
	}
	/*****************************************************************************
	 * Put data to Web view before it's loaded, enable web engine using these
	 * data to compose the final view page.
	 * @param name
	 * @param obj 
	 * @return - current form object, for quick code
	 */
	public HtmlForm putData(String name, Object obj){
       finput.put(name, obj);
       return this;
	}
	
	
	/******************************************************************************
	 * get data exposed from form after it exited.
	 * @param name - id of exposed object from form loader side
	 * @return
	 */
	
	public Object getData(String name){
		
		return foutput.get(name);
	}
	
	/*****************************************************************************
	 * load content into form. Usually be used if constructed as blank form.
	 * @param content
	 * @return
	 */
	public HtmlForm load(String codeORpath){
		 
		
		String all = "";

		if(	(1 ==loadByFile) ||
				((loadByFile == -1) && isLikelyAFilePath(codeORpath))
			){
        
	        try{
	            LineNumberReader rd = new LineNumberReader(new FileReader(codeORpath	));
	            while(rd.ready()){  all += rd.readLine()+"\n";  }
	            rd.close();
	        } catch(Exception e){  	
	        	System.err.println("#Error loading html file! "+codeORpath);
	        	e.printStackTrace();
	        }
		}
		else
		{
			all = codeORpath;
		}
		
		webEngine.loadContent(all); 
		
		return this;
	}

	/*****************************************************************************
	 * break the form, force to close without default return.
	 */
	public void close() {
		// TODO Auto-generated method stub
	}
	
	/*****************************************************************************
	 * Show form, once form closed, return one key value. 
	 * @return - this form
	 */
	public HtmlForm show(){
		launch(null);
		return this;
	}
	/*****************************************************************************/
	/*    F O R   H T M L   F O R M   O N L Y                                    */
	/*****************************************************************************/

	
	@Override
    public void start(Stage primaryStage) {
		if(title==null)
			primaryStage.setTitle("Form Fx");
		else
			primaryStage.setTitle(title);
        
		webView = new WebView();
		webEngine = webView.getEngine(); 

		//ALTER1: This method run on j8 only
         webEngine.getLoadWorker().stateProperty().addListener(
                 new ChangeListener<State>(){
                      
                     public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {
                    	// System.out.println("old: "+oldState+", new: "+newState);
                         if(
                        	newState == State.RUNNING && oldState == State.SCHEDULED
                        		 ){
                             putDataInterfaces2WebEngine();
                         }
                       
                     }

                 });
         
         //ALTER2: this method run on j7,j8 but dirty
         //require JS in web content having
         //window.status = 'MY-MAGIC-VALUE';
         //window.status = '';
         webEngine.setOnStatusChanged(new EventHandler<WebEvent<String>>(){
        		int __event_status_changed_cnt = 0;
			@Override
			public void handle(WebEvent<String> ev) {
				if(ev.getEventType() ==WebEvent.STATUS_CHANGED){
					//System.out.println("status changed!");
					if(__event_status_changed_cnt == 2){
						
						putDataInterfaces2WebEngine();
					} 
					__event_status_changed_cnt++;
				}
			}
        	 
         });
         
         load(url_code);  
         
        MyBrowser myBrowser = new MyBrowser(webView);
        Scene scene = new Scene(myBrowser, 640, 480);
         
        primaryStage.setScene(scene);
        primaryStage.show();
    }
	
	
	/*****************************************************************************/
	/*    I N T E R N A L   U T I L S                                            */
	/*****************************************************************************/

	private void putDataInterfaces2WebEngine() {
		JSObject window = (JSObject)webEngine.executeScript("window");
		 
		 //put inputs
		 for (Map.Entry<String, Object> entry : finput.entrySet()) {
		    String key = entry.getKey();
		    Object value = entry.getValue();
		    window.setMember(key, value);
		 }
		 
		 //put output containter
		 window.setMember("foutput", foutput);
		 window.setMember("form", (HtmlFormWebInf)HtmlForm.this);
	}	
	
	private boolean isLikelyAFilePath(String url){
		boolean result = true;
		final char[] ILL_FILE_PATH_CHARACTERS = { '\n', '\r', '\t', '\0', '\f', '|', '\"', '<', '>'  };

		//check 1
		for(int i = 0; i< url.length(); i++){
			char exam = url.charAt(i);
			for(int j=0; j<ILL_FILE_PATH_CHARACTERS.length; j++){
				if(ILL_FILE_PATH_CHARACTERS[j] == exam){
					return false;
				}
			}
		}

		return result;
	}
	
	/*****************************************************************************/	
	

	
	/*****************************************************************************/	
	
}
