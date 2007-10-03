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


package org.genepattern.gpge.ui.table;

public class SortEvent extends java.util.EventObject {
	private int column;

	private boolean ascending;

	public SortEvent(Object source, int column, boolean ascending) {
		super(source);
		this.column = column;
		this.ascending = ascending;
	}

	public int getColumn() {
		return column;
	}

	public boolean isAscending() {
		return ascending;
	}
}
