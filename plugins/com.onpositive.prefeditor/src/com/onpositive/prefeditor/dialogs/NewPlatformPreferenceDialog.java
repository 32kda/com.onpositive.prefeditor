package com.onpositive.prefeditor.dialogs;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class NewPlatformPreferenceDialog extends NewPreferenceDialog {
	
	protected static final String DEFAULT_SCOPE = "instance";
	protected static final String[] SCOPE_VALUES = {"instance","configuration"};
	protected static final String[] SCOPE_LABELS = {"Instance","Configuration"};
	private String scope = "";
	private Button[] scopeRadios = new Button[SCOPE_VALUES.length];

	public NewPlatformPreferenceDialog(Shell parentShell, String initialParent,
			String[] possibleParents) {
		super(parentShell, processInitialParent(initialParent), processParents(possibleParents));
		this.scope = getScope(initialParent);
	}

	protected String getScope(String initialParent) {
		String[] splitted = initialParent.split("/"); //Splitting is used, since name can have form like /scope/bundle/a/b/c
		if (splitted.length > 2) {
			return splitted[1];
		}
		return "";
	}

	protected static String processInitialParent(String initialParent) {
		String[] splitted = initialParent.split("/"); //Splitting is used, since name can have form like /scope/bundle/a/b/c
		if (splitted.length > 2) {
			return splitted[2];
		}
		return initialParent;
	}

	protected static String[] processParents(String[] possibleParents) {
		Set<String> parents = new HashSet<>();
		for (int i = 0; i < possibleParents.length; i++) {
			String parent = possibleParents[i];
			String[] splitted = parent.split("/");
			if (splitted.length > 2) {
				parent = splitted[2];
			}
			parents.add(parent);
		}
		return parents.stream().sorted().collect(Collectors.toList()).toArray(new String[0]);
	}
	
	@Override
	protected void createParentControls(Composite composite) {
		Label label = new Label(composite, SWT.WRAP);
        label.setText("Preference scope:");
        Composite group = new Composite(composite, SWT.NONE);
        group.setLayout(new RowLayout());
        for (int i = 0; i < SCOPE_LABELS.length; i++) {
			Button btn = new Button(group, SWT.RADIO);
			btn.setText(SCOPE_LABELS[i]);
			btn.setData(SCOPE_VALUES[i]);
			if (SCOPE_VALUES[i].equals(scope)) {
				btn.setSelection(true);
			}
			scopeRadios[i] = btn;
		}
		super.createParentControls(composite);
	}
	
	@Override
	protected String getParentTxt() {
		String scope = DEFAULT_SCOPE;
		for (Button btn : scopeRadios) {
			if (btn.getSelection()) {
				scope = (String) btn.getData();
			}
		}
		return "/" + scope + "/" + super.getParentTxt();
	}
	
}
