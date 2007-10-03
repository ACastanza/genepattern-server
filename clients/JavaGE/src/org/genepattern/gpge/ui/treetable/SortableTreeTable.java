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


package org.genepattern.gpge.ui.treetable;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.*;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.*;
import javax.swing.tree.TreeSelectionModel;

import java.awt.dnd.DragGestureRecognizer;
import java.awt.event.InputEvent;
import org.genepattern.gpge.ui.graphics.draggable.TransferableTreePath;
import org.genepattern.gpge.ui.table.*;

/**
 * A tree table that supports sorting the columns
 * 

 */
public class SortableTreeTable extends JTreeTable implements
		DragSourceListener, DragGestureListener {
	 
	JTree tree;

	private TreePath _pathSource;// The path being dragged

	private BufferedImage _imgGhost;// The 'drag image'

	private Point _ptOffset = new Point();// Where, in the drag image, the mouse
										  // was clicked

	SortableHeaderRenderer sortableHeaderRenderer;						
	public static int ASCENDING = SortableHeaderRenderer.ASCENDING;
	public static int DESCENDING = SortableHeaderRenderer.DESCENDING;
	
	private boolean inDrag = false;

	SortTreeTableModel model;

	DragSource dragSource;

	private boolean dragLeafsOnly = true;


	public SortableTreeTable(SortTreeTableModel m) {
		super(m);
		this.model = m;
		setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

		tree = (JTree) getDefaultRenderer(org.jdesktop.swing.treetable.TreeTableModel.class);
		tree.setCellRenderer(new ClippedTreeCellRenderer());

		dragSource = DragSource.getDefaultDragSource();

		DragGestureRecognizer dgr = dragSource
				.createDefaultDragGestureRecognizer(this,
						DnDConstants.ACTION_COPY_OR_MOVE, this);
		dgr.setSourceActions(dgr.getSourceActions() & ~InputEvent.BUTTON3_MASK);

		tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		sortableHeaderRenderer = new SortableHeaderRenderer(this, m) {
			public void setSortingStatus(int column, int status) {
				try {
					TreePath selectionPath = tree.getSelectionPath();
					List expandedPaths = new ArrayList();
					
					int rc = tree.getRowCount();
					for(int i = 0; i < rc; i++) {
						if(tree.isExpanded(i)) {
							expandedPaths.add(tree.getPathForRow(i));
						}
					}
						
					super.setSortingStatus(column,  status);
					for(int i = 0; i < expandedPaths.size(); i++) {
						TreePath path = (TreePath) expandedPaths.get(i);
						tree.expandPath(path);
					}
					if(selectionPath!=null) {
						tree.setSelectionPath(selectionPath);  
					}
				} catch(Throwable t){}
			} 
		};
		sortableHeaderRenderer.setSortingStatus(0, SortableHeaderRenderer.ASCENDING);
	}
   
	public void setSortingStatus(int column, int status) {
		if(status!=ASCENDING && status!=DESCENDING) {
			throw new IllegalArgumentException("Invalid sort order");
		}
		sortableHeaderRenderer.setSortingStatus(column, status);
	}
	
   public Object getValueAt(int row, int column) {
      try {
         return super.getValueAt(row, column);
      } catch(Throwable t) {
         return null;  
      }
   }

   public void setDragLeafsOnly(boolean b) {
	   dragLeafsOnly = b;
   }
   
	public final void dragGestureRecognized(final DragGestureEvent e) {

		final Point ptDragOrigin = e.getDragOrigin();
		int row = this.getSelectedRow();

		if (getSelectedColumn() != 0) {
			return;
		}

		final TreePath path = getPathForLocation(ptDragOrigin.x, ptDragOrigin.y);
		if (path == null) {
			return;
		}
		if (isRootPath(path)) { // Ignore user trying to drag the root node
			return;
		}

		Object comp = path.getLastPathComponent();
		if (dragLeafsOnly && !tree.getModel().isLeaf(comp)) {
			return;
		}

		// Work out the offset of the drag point from the TreePath bounding
		// rectangle origin
		final Rectangle raPath = tree.getPathBounds(path);

		_ptOffset.setLocation(raPath.x - ptDragOrigin.x, raPath.y
				- ptDragOrigin.y);

		// Get the cell renderer (which is a JLabel) for the path being dragged
		final JLabel lbl = (JLabel) tree.getCellRenderer()
				.getTreeCellRendererComponent(tree, // tree
						path.getLastPathComponent(), // value
						false, // isSelected (dont want a colored background)
						isExpanded(path), // isExpanded
						tree.getModel().isLeaf(path.getLastPathComponent()), // isLeaf
						0, // row (not important for rendering)
						false// hasFocus (dont want a focus rectangle)
				);
		lbl.setSize((int) raPath.getWidth(), (int) raPath.getHeight());// <--
																	   // The
																	   // layout
																	   // manager
																	   // would
																	   // normally
																	   // do
																	   // this

		// Get a buffered image of the selection for dragging a ghost image
		_imgGhost = new BufferedImage((int) raPath.getWidth(), (int) raPath
				.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
		final Graphics2D g2 = _imgGhost.createGraphics();

		// Ask the cell renderer to paint itself into the BufferedImage
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));// Make
																			  // the
																			  // image
																			  // ghostlike
		lbl.paint(g2);

		// Now paint a gradient UNDER the ghosted JLabel text (but not under the
		// icon if any)
		// Note: this will need tweaking if your icon is not positioned to the
		// left of the text
		final Icon icon = lbl.getIcon();
		final int nStartOfText = (icon == null) ? 0 : icon.getIconWidth()
				+ lbl.getIconTextGap();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER,
				0.5f));// Make the gradient ghostlike
		g2.setPaint(new GradientPaint(nStartOfText, 0,
				SystemColor.controlShadow, getWidth(), 0, new Color(255, 255,
						255, 0)));
		g2.fillRect(nStartOfText, 0, getWidth(), _imgGhost.getHeight());

		g2.dispose();

		tree.setSelectionPath(path);// Select this path in the tree
		// Wrap the path being transferred into a Transferable object
		final Transferable transferable = new TransferableTreePath(path);

		// Remember the path being dragged (because if it is being moved, we
		// will have to delete it later)
		_pathSource = path;

		// We pass our drag image just in case it IS supported by the platform

		_ptOffset.y = 0;
		e.startDrag(DragSource.DefaultCopyDrop, _imgGhost, _ptOffset,
				transferable, this);
	}

	// Interface: DragSourceListener
	public final void dragEnter(DragSourceDragEvent e) {
		inDrag = true;
	}

	public final void dragOver(final DragSourceDragEvent e) {
		inDrag = true;
	}

	public final void dragExit(DragSourceEvent e) {

		inDrag = false;
		java.awt.Window window = javax.swing.SwingUtilities
				.getWindowAncestor(tree);
		if (window != null) {
			window.repaint();
		}
	}

	public final void dropActionChanged(DragSourceDragEvent e) {
	}

	public final void dragDropEnd(final DragSourceDropEvent e) {
		inDrag = false;
		if (e.getDropSuccess()) {
			final int nAction = e.getDropAction();
			if (nAction == DnDConstants.ACTION_MOVE) {// The dragged item
													  // (_pathSource) has been
													  // inserted at the target
													  // selected by the user.
				// Now it is time to delete it from its original location.

				// .
				// .. ask your TreeModel to delete the node
				// .

				_pathSource = null;
			}
		}
	}

	
	private class ClippedTreeCellRenderer extends DefaultTreeCellRenderer {
            public void paint(Graphics g) {
                String fullText = super.getText();
                // getText() calls tree.convertValueToText();
                // tree.convertValueToText() should call treeModel.convertValueToText(), if possible

                String shortText = SwingUtilities.layoutCompoundLabel(
                    this, g.getFontMetrics(), fullText, getIcon(),
                    getVerticalAlignment(), getHorizontalAlignment(),
                    getVerticalTextPosition(), getHorizontalTextPosition(),
                    getItemRect(itemRect), iconRect, textRect,
                    getIconTextGap());

               
                setText(shortText); // temporarily truncate text
                super.paint(g);
                setText(fullText); // restore full text
            }

            private Rectangle getItemRect(Rectangle itemRect) {
                getBounds(itemRect);
                itemRect.width = tree.getWidth() - itemRect.x;
                return itemRect;
            }

            // Rectangles filled in by SwingUtilities.layoutCompoundLabel();
            private final Rectangle iconRect = new Rectangle();
            private final Rectangle textRect = new Rectangle();
            // Rectangle filled in by this.getItemRect();
            private final Rectangle itemRect = new Rectangle();
        }
		  
	private boolean isRootPath(TreePath path) {
		return tree.isRootVisible() && tree.getRowForPath(path) == 0;
	}
	
}
