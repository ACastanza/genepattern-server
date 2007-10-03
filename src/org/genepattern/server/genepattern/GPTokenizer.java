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


package org.genepattern.server.genepattern;

import org.apache.lucene.analysis.*;
import java.io.Reader;

public class GPTokenizer extends CharTokenizer {
	/** Construct a new GPTokenizer. */
	public GPTokenizer(Reader in) {
		super(in);
	}

	/**
	 * Collects only characters which do not satisfy
	 * {@link Character#isWhitespace(char)}.
	 */
	protected boolean isTokenChar(char c) {
		return !Character.isWhitespace(c);
	}

	/**
	 * Collects only characters which satisfy {@link Character#isLetter(char)}.
	 */
	protected char normalize(char c) {
		return Character.toLowerCase(c);
	}
}
