package spider.form.html;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JDialog;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import javafx.scene.Scene;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;

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
 * v1.2 - 3-Oct-2015 - allowed invoke form more than once
 * v1.0 - 2-Oct-2015
 */

public class HtmlForm {
	public Map<String, Object> finput = new HashMap<String, Object>();
	public Map<String,Object> foutput = new HashMap<String, Object>();
	public JDialog frame = new JDialog();

	//on fx thread
	private WebView webView;
	private WebEngine webEngine;
	private String url_code =	"";

	//on swing thread

	 private JFXPanel fxPanel;
	
	private int loadByFile = -1; //-1: uninit, 1: true, 0: false

	/*****************************************************************************
	 * Create blank form, content loaded later by load()
	 */
	public HtmlForm() { 
		this(null);
	}	
	
	/*****************************************************************************
	 * Create form from url file/html code implicitly
	 * @param content
	 */
	
	public HtmlForm(String content) {
		this(content, false);
		loadByFile = -1 ; //known content is url or html code
	}
	
	/*****************************************************************************
	 * Create form from url file/html code explicitly
	 * @param content
	 * @param bloadByFile -<code>true</code> will load as file,
	 * 					<code>false</code> will load as html code string
	 */
	
	public HtmlForm(String content, boolean bloadByFile) {
		this("Form Fx", content, bloadByFile);
	}

	/*****************************************************************************
	 * Create form from url file/html code explicitly
	 * @param title
	 * @param content
	 * @param bloadByFile -<code>true</code> will load as file,
	 * 					<code>false</code> will load as html code string
	 */
	
	public HtmlForm(String title, String content, boolean bloadByFile) {
		url_code =content;
		loadByFile = (bloadByFile)? 1 : 0;
		
		frame.setTitle(title);
		frame.setModal(true);
		fxPanel = new JFXPanel();
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(fxPanel);
		frame.setSize(300, 200);
	}
	
	public HtmlForm setTitle(String title){
		frame.setTitle(title);
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
		//TODO Platform.runLater ??
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
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFX(fxPanel);
            }
       });
        
        frame.setVisible(true);
		return this;
	}
	
	/*****************************************************************************
	 * load content into form. Usually be used if constructed as blank form.<p>
	 * Warn: not safe function.
	 * @param content
	 * @return
	 */
	@Deprecated
	public HtmlForm load(String codeORpath){
		url_code = codeORpath;
		//if webview's running, load dynamically 
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                fxload(url_code);
            }
       });		
		return this;
	}
	
	
	/*****************************************************************************/
	/*    F X   T H R E A D   F U N C T I O N S                                  */
	/*****************************************************************************/

	protected void initFX(JFXPanel fxPanel2) {

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
        
         
        MyBrowser myBrowser = new MyBrowser(webView);
        Scene scene = new Scene(myBrowser, 640, 480);
         
        fxPanel2.setScene(scene);
        fxload(url_code);  
    }
	
	double pwidth,pheight;
	
	/*****************************************************************************
	 * load content into form. Usually be used if constructed as blank form.
	 * @param content
	 * @return
	 */
	private HtmlForm fxload(String codeORpath){
		 
		
		String all = "";

		if(	(1 ==loadByFile) ||
				((loadByFile == -1) && isLikelyAFilePath(codeORpath)))
		{
			File f = new File(codeORpath);
			try {
				webEngine.load(f.toURI().toURL().toString());
			} catch (MalformedURLException e) {
	        	System.err.println("#Error loading html file! "+codeORpath);
	        	e.printStackTrace();
			}
		}
		else
		{
			all = codeORpath;
			webEngine.loadContent(all); 
		}

		return this;
	}
		
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
		 window.setMember("form", HtmlForm.this);
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
	
}
