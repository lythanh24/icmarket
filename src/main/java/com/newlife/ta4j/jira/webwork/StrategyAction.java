package com.newlife.ta4j.jira.webwork;

import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.opensymphony.util.TextUtils;

@SupportedMethods(RequestMethod.GET)
public class StrategyAction extends JiraWebActionSupport
{
    private static final Logger log = LoggerFactory.getLogger(StrategyAction.class);
    
    private IssueService issueService;
	private SubTaskManager subTaskManager;
	private JiraAuthenticationContext authenticationContext;
	
	private String strategyName;
    
    
    
    public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}
	
	public void init() {
		issueService = ComponentAccessor.getIssueService();
		authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
		subTaskManager = ComponentAccessor.getSubTaskManager();
	}

    @Override
    public String execute() throws Exception {
    	
    	init();
    	log.info("Strategy actin started...");

    	return SUCCESS;
    }    
        
    public StrategyAction() {
    }

    /**
     * Validate the parameters in the
     * javax.servlet.http.HttpServletRequest request. The HTML form
     * may or may not have been submitted yet since doValidation() is
     * always called when this Action's .jspa URL is invoked.
     *
     * If an error message is set and no input view exists,
     * then doExecute is not called and the view element named "error" in 
     * atlassian-plugin.xml is used. 
     *
     * If an error message is set and an input view does exist, then
     * the input view is used instead of the error view.
     *
     * The URL displayed in the browser doesn't change for an error,
     * just the view.
     *
     * No exceptions are thrown, instead errors and error messages are set.
     */
   
    /**
     * This method is always called when this Action's .jspa URL is
     * invoked if there were no errors in doValidation().
     */
    protected String doExecute() throws Exception {
        log.debug("Entering doExecute");
        for (Enumeration e =  request.getParameterNames(); e.hasMoreElements() ;) {
            String n = (String)e.nextElement();
            String[] vals = request.getParameterValues(n);
            log.debug("name " + n + ": " + vals[0]);
        }

        return SUCCESS;
    }

    /**
     * Set up default values, if any. If you don't have default
     * values, this is not needed.
     *
     * If you want to have default values in your form's fields when it
     * is loaded, then first call this method (or one with some other
     * such name as doInit) and set the local variables. Then return
     * "input" to use the input form view, and in the form use
     * ${myfirstparameter} to call getMyfirstparameter() to load the
     * local variables.
     */
    public String doDefault() throws Exception {
        log.debug("Entering doDefault");        

        // If any of these parameter names match public set methods for local
        // variables, then the local variable will have been set before this
        // method is entered.
        for (Enumeration e =  request.getParameterNames(); e.hasMoreElements() ;) {
            String n = (String)e.nextElement();
            String[] vals = request.getParameterValues(n);
            log.debug("Parameter " + n + "=" + vals[0]);
        }

        // You could also set a local variable to have a different value
        // every time the Action is invoked here, e.g. a timestamp.

        // This should be "input". If no input view exists, that's an error.
        log.info("Preparing to recieve inputs");
        String result = super.doDefault();
        log.debug("Exiting doDefault with a result of: " + result);
		return INPUT;
    }

    //
    // Start of local variables and their get and set methods
    //
    // Note: booleans should use isVariableName() not getVariableName()
    //

    /**
     * An example of a local variable that is set from HTTP request parameters.
     */
    private String aStringVariable = "a default value";

    /**
     * This method is automatically discovered and called by JSP and Webwork
     * if the name matches the id of a parameter passed in an HTML form.
     * The class of the parameter (String) has to match, and the
     * method has to be public or it is silently ignored.
     */
    public void setMyfirstparameter(String value) {
        log.debug("Setting aStringVariable to: " + value);
        this.aStringVariable = value;
    }

    /**
     * This is how the local variable is always accessed, since only this
     * action knows that its name isn't really "myfirstparameter".
     */
    public String getMyfirstparameter() {
        log.debug("Getting aStringVariable");
        return aStringVariable;
    }
}
