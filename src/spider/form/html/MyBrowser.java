package spider.form.html;

import javafx.scene.layout.Region;
import javafx.scene.web.WebView;

public class MyBrowser extends Region{
	public MyBrowser(WebView webView){
		getChildren().add(webView);
	}
}