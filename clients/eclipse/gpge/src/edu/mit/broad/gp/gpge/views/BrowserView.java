/*
 * Created on Jul 20, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;


/**
 * The Browser view.  This consists of a <code>Browser</code> control, and an
 * address bar consisting of a <code>Label</code> and a <code>Text</code> 
 * control.  This registers handling actions for the retargetable actions added 
 * by <code>BrowserActionBuilder</code> (Back, Forward, Stop, Refresh).  
 * This also hooks listeners on the Browser control for status and progress
 * messages, and redirects these to the status line.
 * 
 * @since 3.0
 */
public class BrowserView extends ViewPart {
	public static String ID_VIEW = "edu.mit.broad.gp.gpge.views.BrowserView";
	/**
	 * Debug flag.  When true, status and progress messages are sent to the
	 * console in addition to the status line.
	 */
	private static final boolean DEBUG = false;
	
	private Browser browser;
	private Text location;
	private String initialUrl = null; //$NON-NLS-1$
	
	private Action backAction = new Action("Back") {
		public void run() {
			browser.back();
		}
	};
	
	private Action homeAction = new Action("Home") {
		public void run() {
			browser.setUrl(initialUrl);
		}
	};
	private Action forwardAction = new Action("Forward") {
		public void run() {
			browser.forward();
		}
	};

	private Action stopAction = new Action("Stop") {
		public void run() {
			browser.stop();
			// cancel any partial progress.
			getViewSite().getActionBars().getStatusLineManager().getProgressMonitor().done();
		}
	};

	private Action refreshAction = new Action("Refresh") {
		public void run() {
			browser.refresh();
		}
	};
	
	/**
	 * Constructs a new <code>BrowserView</code>.
	 */
	public BrowserView() {
		// do nothing
	}
	
    public void init(IViewSite site, IMemento memento) throws PartInitException {
        super.init(site);
        if (memento != null) {
	        String u = memento.getString("url"); //$NON-NLS-1$
	        if (u != null) {
	            initialUrl = u;
	        }
        }
    }
    
    public void saveState(IMemento memento) {
        memento.putString("url", browser.getUrl());
    }
    
	public void createPartControl(Composite parent) {
		browser = createBrowser(parent, getViewSite().getActionBars());
		//browser.setUrl(initialUrl);
	}

	public void setFocus() {
		if (browser != null && !browser.isDisposed()) {
			browser.setFocus();
		}
	}
	
	public void setUrl(String url){
		initialUrl = url;
		browser.setUrl(initialUrl);
	}
	public String getUrl(){
		return initialUrl;
	}
	
	private Browser createBrowser(Composite parent, final IActionBars actionBars) {
		
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		parent.setLayout(gridLayout);
		
		Label labelAddress = new Label(parent, SWT.NONE);
		labelAddress.setText("A&ddress");
		
		location = new Text(parent, SWT.BORDER);
		GridData data = new GridData();
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		location.setLayoutData(data);

		browser = new Browser(parent, SWT.NONE);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		browser.setLayoutData(data);

		browser.addProgressListener(new ProgressAdapter() {
			IProgressMonitor monitor = actionBars.getStatusLineManager().getProgressMonitor();
			boolean working = false;
			int workedSoFar;
			public void changed(ProgressEvent event) {
				if (DEBUG) {
					System.out.println("changed: " + event.current + "/" + event.total);
				}
				if (event.total == 0) return;
				if (!working) {
					if (event.current == event.total) return;
					monitor.beginTask("", event.total); //$NON-NLS-1$
					workedSoFar = 0;
					working = true;
				}
				monitor.worked(event.current - workedSoFar);
				workedSoFar = event.current;
			}
			public void completed(ProgressEvent event) {
				if (DEBUG) {
					System.out.println("completed: " + event.current + "/" + event.total);
				}
				monitor.done();
				working = false;
			}
		});
		browser.addStatusTextListener(new StatusTextListener() {
			IStatusLineManager status = actionBars.getStatusLineManager(); 
			public void changed(StatusTextEvent event) {
				if (DEBUG) {
					System.out.println("status: " + event.text);
				}
				status.setMessage(event.text);
			}
		});
		browser.addLocationListener(new LocationAdapter() {
			public void changed(LocationEvent event) {
				location.setText(event.location);
			}
		});
		browser.addTitleListener(new TitleListener() {
            public void changed(TitleEvent event) {
                setPartName(event.title);
            }
        });
		location.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				browser.setUrl(location.getText());
			}
		});
		
		actionBars.setGlobalActionHandler("back", backAction); //$NON-NLS-1$
		actionBars.setGlobalActionHandler("forward", forwardAction); //$NON-NLS-1$
		actionBars.setGlobalActionHandler("stop", stopAction); //$NON-NLS-1$
		actionBars.setGlobalActionHandler("refresh", refreshAction); //$NON-NLS-1$
		fillLocalToolBar(actionBars.getToolBarManager());
		return browser;
	}

	  private void fillLocalToolBar(IToolBarManager manager) {
	        manager.add(backAction);
	        manager.add(forwardAction);
		    manager.add(stopAction);
	        manager.add(refreshAction);
	        manager.add(homeAction);
		    manager.add(new Separator());
	    }
	
}
