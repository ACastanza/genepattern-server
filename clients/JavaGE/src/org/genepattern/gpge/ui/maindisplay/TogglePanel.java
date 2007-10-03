/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2008) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package org.genepattern.gpge.ui.maindisplay;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TogglePanel extends JPanel {
	ToggleLabel label;

	JPanel labelPanel;

	public TogglePanel(String text, JComponent toggleComponent) {
		setLayout(new BorderLayout());
		label = new ToggleLabel(text, toggleComponent);
		add(label, BorderLayout.NORTH);
		add(toggleComponent, BorderLayout.CENTER);
	}

	public TogglePanel(String text, JComponent rightComponent,
			JComponent toggleComponent) {
		setLayout(new BorderLayout());
		label = new ToggleLabel(text, toggleComponent);
		labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		labelPanel.add(label);
		labelPanel.setBackground(getBackground());
		labelPanel.add(rightComponent);
		add(labelPanel, BorderLayout.NORTH);
		add(toggleComponent, BorderLayout.CENTER);
	}

	public void setBackground(Color c) {
		super.setBackground(c);
		if (labelPanel != null) {
			labelPanel.setBackground(c);
		}

	}

	public void setExpanded(boolean b) {
		label.setExpanded(b);
	}

	public boolean isExpanded() {
		return label.expanded;
	}

	private static class ToggleLabel extends JLabel {
		final Icon collapsedIcon = (Icon) UIManager.get("Tree.collapsedIcon");

		final Icon expandedIcon = (Icon) UIManager.get("Tree.expandedIcon");

		boolean expanded = false;

		private JComponent component;

		public void setExpanded(boolean expanded) {
			this.expanded = expanded;
			if (!expanded) {
				setIcon(collapsedIcon);
			} else {
				setIcon(expandedIcon);
			}
			component.setVisible(expanded);
		}

		public void toggleState() {
			expanded = !expanded;
			setExpanded(expanded);
		}

		public ToggleLabel(String text, JComponent c) {
			super(text);
			this.component = c;
			component.setVisible(false);
			int left = this.getIconTextGap() + expandedIcon.getIconWidth();
			component.setBorder(BorderFactory.createEmptyBorder(0, left, 0, 0));
			addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					Icon icon = getIcon();
					int width = icon.getIconWidth();
					int height = icon.getIconHeight();
					if (e.getPoint().x <= width && e.getPoint().y <= height) {
						toggleState();
					}

				}
			});
			setIcon(collapsedIcon);
		}
	}
}
