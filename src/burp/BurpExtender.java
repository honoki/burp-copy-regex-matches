package burp;

import java.io.PrintWriter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.awt.Frame;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class BurpExtender extends JDialog implements IBurpExtender, IExtensionStateListener, IContextMenuFactory  {

    private IExtensionHelpers helpers;
	private PrintWriter stdout;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        callbacks.setExtensionName("Regex Copy");
		this.stdout = new PrintWriter(callbacks.getStdout(), true);
        this.helpers = callbacks.getHelpers();
		
        stdout.println("Regex Copy extension initialized.");

		// unload resources when this extension is removed;
		callbacks.registerExtensionStateListener(this);

        // register the right-click menu:
		callbacks.registerContextMenuFactory(this);
    
    }

    private void copyRegex(IHttpRequestResponse[] selected, boolean reqs, boolean resps) {
        JFrame theFrame = BurpExtender.getBurpFrame();
        String lookfor = JOptionPane.showInputDialog(theFrame, "Search for regex:");

        ArrayList<String> matches = new ArrayList<String>();
        
        try  {
            for(IHttpRequestResponse req : selected) {
                if(reqs == (req.getRequest() == null) && resps == (req.getResponse() == null)) continue;
                String request = "";
                String response = "";
                if(reqs && req.getRequest() != null) request = helpers.bytesToString(req.getRequest());
                if(resps && req.getResponse() != null) response = helpers.bytesToString(req.getResponse());
                Matcher m = Pattern.compile(lookfor).matcher(request+response);
                while (m.find()) {
                  matches.add( m.group() );
                }
            }
        } catch (PatternSyntaxException err) {
            JOptionPane.showInputDialog(theFrame, "Invalid regex entered.\nSearch for regex:", lookfor);
        }
        
        String clippy = "";
        for(String s : matches) {
            clippy += s + "\n";
        }
        StringSelection selection = new StringSelection(clippy);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);

        JOptionPane.showMessageDialog(theFrame, matches.size() + " matches copied to clipboard");
    }

    @Override
	public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
		ArrayList<JMenuItem> menu = new ArrayList<JMenuItem>();
		
		IHttpRequestResponse[] selection = invocation.getSelectedMessages();
        if(selection.length < 1) {
            return null; 
        }
		
        JMenuItem copyRegexReq = new JMenuItem("Copy regex matches (" + selection.length + " requests)");
        JMenuItem copyRegexResp = new JMenuItem("Copy regex matches (" + selection.length + " responses)");
        JMenuItem copyRegexBoth = new JMenuItem("Copy regex matches");

		copyRegexReq.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copyRegex(selection, true, false);
			}
		});

        copyRegexResp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
                copyRegex(selection, false, true);
            }
		});

        copyRegexBoth.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copyRegex(selection, true, true);
			}
		});
		
			
        menu.add(copyRegexReq);
        menu.add(copyRegexResp);
        menu.add(copyRegexBoth);
		
		return menu;
	}

    @Override
    public void extensionUnloaded() {
        // do something
    }
    
    static JFrame getBurpFrame()
    {
        for(Frame f : Frame.getFrames())
        {
            if(f.isVisible() && f.getTitle().startsWith(("Burp Suite")))
            {
                return (JFrame) f;
            }
        }
        return null;
    }
}