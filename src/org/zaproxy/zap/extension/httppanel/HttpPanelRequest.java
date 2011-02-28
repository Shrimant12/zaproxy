package org.zaproxy.zap.extension.httppanel;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;
import javax.xml.ws.Service.Mode;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.Extension;
import org.parosproxy.paros.extension.history.ManualRequestEditorDialog;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.network.HttpMalformedHeaderException;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpRequestHeader;
import org.parosproxy.paros.view.HttpPanel;
import org.zaproxy.zap.extension.search.SearchMatch;
import org.zaproxy.zap.httputils.RequestUtils;

/*
 *
 */

public class HttpPanelRequest extends HttpPanel implements ActionListener {
	private static final long serialVersionUID = 1L;
	
	// ZAP: Added logger
    private static Log log = LogFactory.getLog(ManualRequestEditorDialog.class);

    private JComboBox jComboView;
    
	private JButton btnSplit;
	private JButton btnAll;
	private JButton btnGet;
	private JButton btnPost;
	private JButton btnCookies;
	private JButton btnHeader;

	private HttpPanelTextUi httpPanelTextUi;
	private HttpPanelTableUi httpPanelTableUi;
	private HttpPanelHexUi httpPanelHexUi;
	private HttpPanelSplitUi httpPanelSplitUi;
	
	private HttpDataModelReqAll httpDataModelAll;
	private HttpDataModelReqGet httpDataModelGet;
	private HttpDataModelReqHeader httpDataModelHeader;
	private HttpDataModelReqPost httpDataModelPost;
	private HttpDataModelReqCookies httpDataModelCookies;
	private HttpDataModelReqSplit httpDataModelSplit;
	
	private View currentView = View.ALL;
	private HttpDataModel currentHttpDataModel;
	
	private enum View {
		SPLIT,
		ALL,
		GET,
		POST,
		COOKIES,
		HEADER
	}
	
	private enum ViewMode {
		TEXT,
		TABLE,
		HEX
	}
	
	public HttpPanelRequest() {
		super();
		init();
	}
	
	public HttpPanelRequest(boolean isEditable) {
		super(isEditable);
		init();
	}
		
	public HttpPanelRequest(boolean isEditable, Extension extension) {
		super(isEditable, extension);
		init();
	}
		
	private void init() {
		initHeader();
		initContent();

		
		if (isEditable()) {
		}
		
		// Set initial window
		changeCard();
		changeView(View.ALL);
	}
	
	private void initHeader() {
		btnSplit = new JButton("Split");
		btnAll = new JButton("All");
		btnGet = new JButton("Get");
		btnPost = new JButton("Post");
		btnCookies = new JButton("Cookies");
		btnHeader = new JButton("Header");
		
		btnSplit.addActionListener(this);
		btnAll.addActionListener(this);
		btnGet.addActionListener(this);
		btnPost.addActionListener(this);
		btnHeader.addActionListener(this);
		btnCookies.addActionListener(this);
		
		getPanelHeader().add(btnSplit);
		getPanelHeader().add(btnAll);
		
		if (Model.getSingleton().getOptionsParam().getViewParam().getAdvancedViewOption() > 0) {
			getPanelHeader().add(btnGet);
			getPanelHeader().add(btnPost);
			getPanelHeader().add(btnCookies);
			getPanelHeader().add(btnHeader);
		}
		
		jComboView = new JComboBox();
		jComboView.setSelectedIndex(-1);
		jComboView.addItem("Text");
		jComboView.addItem("Table");
		jComboView.addItem("Hex");
		jComboView.addActionListener(this);
		
		getPanelHeader().add(jComboView);

	}
	
	private void initContent() {
		httpPanelTextUi = new HttpPanelTextUi();
		httpPanelTableUi = new HttpPanelTableUi();
		httpPanelHexUi = new HttpPanelHexUi();
		httpPanelSplitUi = new HttpPanelSplitUi(isEditable());
		
		getPanelContent().setLayout(new CardLayout());
		
		getPanelContent().add(httpPanelTextUi, "text");
		getPanelContent().add(httpPanelTableUi, "table");
		getPanelContent().add(httpPanelHexUi, "hex");
		getPanelContent().add(httpPanelSplitUi, "split");
		
		
		httpDataModelAll = new HttpDataModelReqAll(this, httpPanelHexUi, httpPanelTableUi, httpPanelTextUi);
		httpDataModelGet = new HttpDataModelReqGet(this, httpPanelHexUi, httpPanelTableUi, httpPanelTextUi);
		httpDataModelHeader = new HttpDataModelReqHeader(this, httpPanelHexUi, httpPanelTableUi, httpPanelTextUi);
		httpDataModelPost = new HttpDataModelReqPost(this, httpPanelHexUi, httpPanelTableUi, httpPanelTextUi);
		httpDataModelCookies = new HttpDataModelReqCookies(this, httpPanelHexUi, httpPanelTableUi, httpPanelTextUi);
		httpDataModelSplit = new HttpDataModelReqSplit(this, httpPanelSplitUi);
	}
	
	// For sending it
	public void getMessage(HttpMessage msg, boolean isRequest) {
		System.out.println("fail get message");
		saveData();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		saveData();
		
		if (e.getSource() == btnSplit) {
			// Split is special
			CardLayout cl = (CardLayout)(getPanelContent().getLayout());
			cl.show(getPanelContent(), "split");
			currentView = View.SPLIT;
		} else if (e.getSource() == btnAll) {
			changeView(View.ALL);
		} else if (e.getSource() == btnGet) {
			changeView(View.GET);
		} else if (e.getSource() == btnPost) {
			changeView(View.POST);
		} else if (e.getSource() == btnHeader) {
			changeView(View.HEADER);
		} else if (e.getSource() == btnCookies) {
			changeView(View.COOKIES);
		} else if (e.getSource() == jComboView) {
			changeCard();
		}
		
		loadData();
	}
	
	private void changeCard() {
		ViewMode viewMode = getViewMode();
	    CardLayout cl = (CardLayout)(getPanelContent().getLayout());
	    
		if (viewMode.equals(ViewMode.TEXT)) {
			cl.show(getPanelContent(), "text");
		} else if (viewMode.equals(ViewMode.TABLE)) {
			cl.show(getPanelContent(), "table");
		} else if (viewMode.equals(ViewMode.HEX)) {
			cl.show(getPanelContent(), "hex");
		}
	}

	// New HttpMessage was set
	public void updateContent() {
		loadData();
	}
	
	private void loadData() {
		if (getHttpMessage() == null) {
			return;
		}
		
		ViewMode view = getViewMode();
		if (currentView.equals(View.SPLIT)) {
			// Split is special
			httpDataModelSplit.loadData();
		} else if (view.equals(ViewMode.TEXT)) {
			currentHttpDataModel.textDataToView();
		} else if (view.equals(ViewMode.TABLE)) {
			currentHttpDataModel.tableDataToView();
		} else if (view.equals(ViewMode.HEX)) {
			currentHttpDataModel.hexDataToView();
		}
	}
	
	public void saveData() {
		if (getHttpMessage() == null) {
			return;
		}
		
		ViewMode view = getViewMode();
		if (currentView.equals(View.SPLIT)) {
			// Split is special
			httpDataModelSplit.saveData();
		} else if (view.equals(ViewMode.TEXT)) {
			currentHttpDataModel.textDataFromView();
		} else if (view.equals(ViewMode.TABLE)) {
			currentHttpDataModel.tableDataFromView();
		} else if (view.equals(ViewMode.HEX)) {
			currentHttpDataModel.hexDataFromView();
		}
	}
	
	private void changeView(View view) {
		this.currentView = view;
		
		if (view.equals(View.ALL)) {
			currentHttpDataModel = httpDataModelAll;
		} else if (view.equals(View.GET)) {
			currentHttpDataModel = httpDataModelGet;
		} else if (view.equals(View.POST)) {
			currentHttpDataModel = httpDataModelPost;
		} else if (view.equals(View.HEADER)) {
			currentHttpDataModel = httpDataModelHeader;
		} else if (view.equals(View.COOKIES)) {
			currentHttpDataModel = httpDataModelCookies;
		}
	}
	
	private ViewMode getViewMode() {
		String item = (String) jComboView.getSelectedItem();

		if (item.equals("Text")) {
			return ViewMode.TEXT;
		} else if (item.equals("Table")) {
			return ViewMode.TABLE;
		} else if (item.equals("Hex")) {
			return ViewMode.HEX;
		}
		
		// Default
		return ViewMode.TEXT;
	}
	
	public void clearView(boolean enableViewSelect) {
		
	}
	
	
	/*** Search Functions - for SearchPanel and SearchResult 
	 * We'll only use the Text card for finding and displaying search results. 
	 * highlight* and *Search belong together.
	 ***/

	public void highlightHeader(SearchMatch sm) {
		changeView(View.HEADER);
		httpPanelTextUi.highlight(sm);
		
	}

	public void highlightBody(SearchMatch sm) {
		changeView(View.POST);
		httpPanelTextUi.highlight(sm);
	}

	@Override
	public void headerSearch(Pattern p, List<SearchMatch> matches) {
		httpDataModelHeader.search(p, matches);
	}

	@Override
	public void bodySearch(Pattern p, List<SearchMatch> matches) {
		httpDataModelPost.search(p, matches);
	}
	
}
