package com.onpositive.prefeditor.dialogs;
import static com.onpositive.prefeditor.PrefConstants.CONFIGURATION_SETTINGS_PATH;
import static com.onpositive.prefeditor.PrefConstants.WORKSPACE_SETTINGS_PATH;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

import com.onpositive.prefeditor.PrefEditorPlugin;

/**
 * A simple input dialog for soliciting an input string from the user.
 * <p>
 * This concrete dialog class can be instantiated as is, or further subclassed as
 * required.
 * </p>
 */
public class FolderSelectionDialog extends Dialog {
	
	private static final String CONFIG_LOCATION_ATTR = "configLocation";
	private static final String WORKSPACE_LOC_VAR = "${workspace_loc}";
	/**
	 * Max recent pref folders to save
	 */
    private static final int MAX_SAVED = 10;
	/**
	 * Зкум ащдвукы зкуаукутсу лун
	 */
	private static final String PREV_FOLDERS_KEY = "prevFolders";

	/**
     * The title of the dialog.
     */
    private String title;

    /**
     * The message to display, or <code>null</code> if none.
     */
    private String message;

    /**
     * The input value; the empty string by default.
     */
    private String value = "";//$NON-NLS-1$

    /**
     * The input validator, or <code>null</code> if none.
     */
    private IInputValidator validator;

    /**
     * Ok button widget.
     */
    private Button okButton;

//    /**
//     * Input text widget.
//     */
//    private Text text;

    /**
     * Error message label widget.
     */
    private Text errorMessageText;
    
    /**
     * Error message string.
     */
    private String errorMessage;
    
    private List<String> prevFolders = new ArrayList<>();

    /**
     * Folder selection combo
     */
	private ComboViewer viewer; 
	
	public FolderSelectionDialog(Shell parentShell, String initialValue) {
		this(parentShell,"Open preferences","Select workspace, installation or folder to view & edit it's preferences list",initialValue,null);
	}
			

    /**
     * Creates an input dialog with OK and Cancel buttons. Note that the dialog
     * will have no visual representation (no widgets) until it is told to open.
     * <p>
     * Note that the <code>open</code> method blocks for input dialogs.
     * </p>
     * 
     * @param parentShell
     *            the parent shell, or <code>null</code> to create a top-level
     *            shell
     * @param dialogTitle
     *            the dialog title, or <code>null</code> if none
     * @param dialogMessage
     *            the dialog message, or <code>null</code> if none
     * @param initialValue
     *            the initial input value, or <code>null</code> if none
     *            (equivalent to the empty string)
     * @param validator
     *            an input validator, or <code>null</code> if none
     */
    public FolderSelectionDialog(Shell parentShell, String dialogTitle,
            String dialogMessage, String initialValue, IInputValidator validator) {
        super(parentShell);
        this.title = dialogTitle;
        message = dialogMessage;
        if (initialValue == null) {
			value = "";//$NON-NLS-1$
		} else {
			value = initialValue;
		}
        this.validator = validator;
    }
    
    protected String valueFromSelection(ISelection selection) {
    	if (selection instanceof StructuredSelection) {
	    	Object firstElement = ((IStructuredSelection) selection).getFirstElement();
	        if (firstElement != null)
	        	return firstElement.toString();
    	}
		return null;
    }

    /*
     * (non-Javadoc) Method declared on Dialog.
     */
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OK_ID) {
            value = valueFromSelection(viewer.getSelection());
            liftValueAndSave();
        } else {
            value = null;
        }
        super.buttonPressed(buttonId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (title != null) {
			shell.setText(title);
		}
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        okButton = createButton(parent, IDialogConstants.OK_ID,
                IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
        //do this here because setting the text will set enablement on the ok
        // button
        viewer.getControl().setFocus();
        loadPrevChoices();
        viewer.setInput(prevFolders.toArray(new String[0]));
        if (value != null) {
        	viewer.setSelection(new StructuredSelection(value));
        }
    }

    private void loadPrevChoices() {
		String[] prevChoices = loadChoices();
		for (String string : prevChoices) {
			prevFolders.add(string);
		}
	}

	/*
     * (non-Javadoc) Method declared on Dialog.
     */
    protected Control createDialogArea(Composite parent) {
        // create composite
        Composite composite = (Composite) super.createDialogArea(parent);
        // create message
        if (message != null) {
            Label label = new Label(composite, SWT.WRAP);
            label.setText(message);
            GridData data = new GridData(GridData.GRAB_HORIZONTAL
                    | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
                    | GridData.VERTICAL_ALIGN_CENTER);
            data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
            label.setLayoutData(data);
            label.setFont(parent.getFont());
        }
        viewer = new ComboViewer(composite);
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(
					SelectionChangedEvent event) {
				valueSelected(valueFromSelection(event.getSelection())); 
			}
		});
        viewer.getControl().setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL));
        
        createLink(composite, "From folder", ()->addFolder());
        
        createLink(composite, "From workspace prefs", ()->addWorkspace());
        
        createLink(composite, "From installation", ()->addInstallation());
        
        createLink(composite, "From launch config", ()->addLaunchCfg());
        
        errorMessageText = new Text(composite, SWT.READ_ONLY | SWT.WRAP);
        errorMessageText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL));
        errorMessageText.setBackground(errorMessageText.getDisplay()
                .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        // Set the error message text
        // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=66292
        setErrorMessage(errorMessage);

        applyDialogFont(composite);
        return composite;
    }

	private void addLaunchCfg() {
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager
					.getLaunchConfigurationType("org.eclipse.pde.ui.RuntimeWorkbench");
			ILaunchConfiguration[] lcs = manager.getLaunchConfigurations(type);
			List<ILaunchConfiguration> configList = Arrays.asList(lcs);
			List<String> configs = configList.stream().map(config -> config.getName()).collect(Collectors.toList());
			ComboDialog dialog = new ComboDialog(getShell(), true);
			dialog.setTitle("Choose launch config");
			dialog.setInfoText("Choose Eclipse launch conguration to edit it's configuration settings");
			dialog.setAllowedValues(configs);
			if (dialog.open() == OK) {
				String selectedName = dialog.getValue();
				ILaunchConfiguration selectedConfig = configList.stream().filter(config -> selectedName.equals(config.getName())).findFirst().get();
				String configLocation = getConfigLocation(selectedConfig);
				valueAdded(new File(configLocation, ".settings").getAbsolutePath());
				buttonPressed(IDialogConstants.OK_ID);
			}
		} catch (CoreException e) {
			PrefEditorPlugin.log(e);
		}
	}

	private String getConfigLocation(ILaunchConfiguration selectedConfig) {
		try {
			String location = selectedConfig.getAttribute(CONFIG_LOCATION_ATTR, "");
			if (location.startsWith(WORKSPACE_LOC_VAR)) {
				return new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString(), location.substring(WORKSPACE_LOC_VAR.length())).getAbsolutePath();
			}
			return location;
		} catch (CoreException e) {
			PrefEditorPlugin.log(e);
		}
		return "";
	}


	private String obtainDir() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		String path = dialog.open();
		return path;
	}

	private void addInstallation() {
		String dir = obtainDir();
		if (dir != null) {
			File prefDir = new File(dir,CONFIGURATION_SETTINGS_PATH);
			if (!prefDir.exists()) {
				MessageDialog.openWarning(getShell(),"Incorrect workspace", dir + " is either not valid Eclipse workspace or doesn't contain any workspace preferences");
			} else {
				valueAdded(prefDir.getAbsolutePath());
			}
		}
	}

	private void addWorkspace() {
		String dir = obtainDir();
		if (dir != null) {
			File prefDir = new File(dir,WORKSPACE_SETTINGS_PATH);
			if (!prefDir.exists()) {
				MessageDialog.openWarning(getShell(),"Incorrect workspace", dir + " is either not valid Eclipse workspace or doesn't contain any workspace preferences");
			} else {
				valueAdded(prefDir.getAbsolutePath());
			}
		}
	}

	private void addFolder() {
		String dir = obtainDir();
		if (dir != null) {
			valueAdded(dir);
		}
	}

	protected void createLink(Composite composite, String text, Runnable action) {
		Link link = new Link(composite, SWT.NONE);
		GridDataFactory.swtDefaults().applyTo(link);
		link.setText("<a>" + text + "</a>");
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				action.run();
			}
		});
	}
    
    protected void valueSelected(String newValue) {
    	value = newValue;
    }

    protected void valueAdded(String newValue) {
    	value = newValue;
    	liftValueAndSave();
    	viewer.setInput(prevFolders.toArray(new String[0]));
    	viewer.setSelection(new StructuredSelection(value));
	}

	protected void liftValueAndSave() {
		if (value != null) {
    		value = value.trim();
    		int idx = prevFolders.indexOf(value);
    		if (idx > 0) {
    			prevFolders.remove(idx);
    			prevFolders.add(0,value);
    		} else if (idx < 0) {
    			prevFolders.add(0,value);
    		}
    	}
    	saveChoices(prevFolders.stream().limit(MAX_SAVED).toArray(String[]::new));
	}

	/**
     * Returns the error message label.
     * 
     * @return the error message label
     * @deprecated use setErrorMessage(String) instead
     */
    protected Label getErrorMessageLabel() {
        return null;
    }

    /**
     * Returns the ok button.
     * 
     * @return the ok button
     */
    protected Button getOkButton() {
        return okButton;
    }
    

    /**
     * Returns the validator.
     * 
     * @return the validator
     */
    protected IInputValidator getValidator() {
        return validator;
    }

    /**
     * Returns the string typed into this input dialog.
     * 
     * @return the input string
     */
    public String getValue() {
        return value;
    }

    /**
     * Validates the input.
     * <p>
     * The default implementation of this framework method delegates the request
     * to the supplied input validator object; if it finds the input invalid,
     * the error message is displayed in the dialog's message line. This hook
     * method is called whenever the text changes in the input field.
     * </p>
     */
    protected void validateInput() {
        String errorMessage = null;
        if (validator != null) {
            errorMessage = validator.isValid(valueFromSelection(viewer.getSelection()));
        }
        // Bug 16256: important not to treat "" (blank error) the same as null
        // (no error)
        setErrorMessage(errorMessage);
    }

    /**
     * Sets or clears the error message.
     * If not <code>null</code>, the OK button is disabled.
     * 
     * @param errorMessage
     *            the error message, or <code>null</code> to clear
     * @since 3.0
     */
    public void setErrorMessage(String errorMessage) {
    	this.errorMessage = errorMessage;
    	if (errorMessageText != null && !errorMessageText.isDisposed()) {
    		errorMessageText.setText(errorMessage == null ? " \n " : errorMessage); //$NON-NLS-1$
    		// Disable the error message text control if there is no error, or
    		// no error text (empty or whitespace only).  Hide it also to avoid
    		// color change.
    		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=130281
    		boolean hasError = errorMessage != null && (StringConverter.removeWhiteSpaces(errorMessage)).length() > 0;
    		errorMessageText.setEnabled(hasError);
    		errorMessageText.setVisible(hasError);
    		errorMessageText.getParent().update();
    		// Access the ok button by id, in case clients have overridden button creation.
    		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=113643
    		Control button = getButton(IDialogConstants.OK_ID);
    		if (button != null) {
    			button.setEnabled(errorMessage == null);
    		}
    	}
    }
    
	/**
	 * Returns the style bits that should be used for the input text field.
	 * Defaults to a single line entry. Subclasses may override.
	 * 
	 * @return the integer style bits that should be used when creating the
	 *         input text
	 * 
	 * @since 3.4
	 */
	protected int getInputTextStyle() {
		return SWT.SINGLE | SWT.BORDER;
	}
	
	protected String[] loadChoices() {
		String prevFolders = InstanceScope.INSTANCE.getNode(PrefEditorPlugin.PLUGIN_ID).get(PREV_FOLDERS_KEY,"");
		String[] folders = prevFolders.split(";");
		return folders;
	}
	
	protected void saveChoices(String[] choices) {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PrefEditorPlugin.PLUGIN_ID);
		node.put(PREV_FOLDERS_KEY, String.join(";", choices));
		try {
			node.flush();
		} catch (BackingStoreException e) {
			PrefEditorPlugin.log(e);
		}
	}
}