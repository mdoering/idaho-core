/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uka.ipd.idaho.gamta.util;


import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Set;

import de.uka.ipd.idaho.gamta.QueriableAnnotation;

/**
 * An InputStream implementation similar to StringInputStream, but using a
 * QueriableAnnotation as its source instead of a String. The basis is an
 * AnnotationReader converting the Annotation to character data, this class
 * deals with encoding the chars to bytes.
 * 
 * @author sautter
 */
public class AnnotationInputStream extends InputStream {
	
	private Charset encoding;
	
	private Reader annotationReader;
	
	private ByteBuffer byteBuffer;
	int bbPosition = 0;
	
	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param charset the name of the charset to use for encoding the chars to
	 *            bytes
	 */
	public AnnotationInputStream(QueriableAnnotation source, String charset) {
		this(source, false, null, charset, null, null);
	}
	
	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param outputIDs include annotation IDs in the output?
	 * @param charset the name of the charset to use for encoding the chars to
	 *            bytes
	 */
	public AnnotationInputStream(QueriableAnnotation source, boolean outputIDs, String charset) {
		this(source, outputIDs, null, charset, null, null);
	}

	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param charset the charset to use for encoding the chars to bytes
	 */
	public AnnotationInputStream(QueriableAnnotation source, Charset charset) {
		this(source, false, null, charset, null, null);
	}

	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param outputIDs include annotation IDs in the output?
	 * @param charset the charset to use for encoding the chars to bytes
	 */
	public AnnotationInputStream(QueriableAnnotation source, boolean outputIDs, Charset charset) {
		this(source, outputIDs, null, charset, null, null);
	}

	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param indent the String to insert for each level of indentation
	 *            (specifying null will result in no indentation)
	 * @param charset the name of the charset to use for encoding the chars to
	 *            bytes
	 */
	public AnnotationInputStream(QueriableAnnotation source, String indent, String charset) {
		this(source, false, indent, charset, null, null);
	}

	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param outputIDs include annotation IDs in the output?
	 * @param indent the String to insert for each level of indentation
	 *            (specifying null will result in no indentation)
	 * @param charset the name of the charset to use for encoding the chars to
	 *            bytes
	 */
	public AnnotationInputStream(QueriableAnnotation source, boolean outputIDs, String indent, String charset) {
		this(source, outputIDs, indent, charset, null, null);
	}

	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param indent the String to insert for each level of indentation
	 *            (specifying null will result in no indentation)
	 * @param charset the charset to use for encoding the chars to bytes
	 */
	public AnnotationInputStream(QueriableAnnotation source, String indent, Charset charset) {
		this(source, false, indent, charset, null, null);
	}

	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param outputIDs include annotation IDs in the output?
	 * @param indent the String to insert for each level of indentation
	 *            (specifying null will result in no indentation)
	 * @param charset the charset to use for encoding the chars to bytes
	 */
	public AnnotationInputStream(QueriableAnnotation source, boolean outputIDs, String indent, Charset charset) {
		this(source, outputIDs, indent, charset, null, null);
	}

	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param charset the name of the charset to use for encoding the chars to
	 *            bytes
	 * @param typeFilter a set containing the types of the annotations to
	 *            include in the output (specifying null will include all
	 *            attributes)
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tags (specifying null will include all
	 *            attributes)
	 */
	public AnnotationInputStream(QueriableAnnotation source, String charset, Set typeFilter, Set attributeFilter) {
		this(source, false, null, charset, typeFilter, attributeFilter);
	}

	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param outputIDs include annotation IDs in the output?
	 * @param charset the name of the charset to use for encoding the chars to
	 *            bytes
	 * @param typeFilter a set containing the types of the annotations to
	 *            include in the output (specifying null will include all
	 *            attributes)
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tags (specifying null will include all
	 *            attributes)
	 */
	public AnnotationInputStream(QueriableAnnotation source, boolean outputIDs, String charset, Set typeFilter, Set attributeFilter) {
		this(source, outputIDs, null, charset, typeFilter, attributeFilter);
	}

	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param charset the charset to use for encoding the chars to bytes
	 * @param typeFilter a set containing the types of the annotations to
	 *            include in the output (specifying null will include all
	 *            attributes)
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tags (specifying null will include all
	 *            attributes)
	 */
	public AnnotationInputStream(QueriableAnnotation source, Charset charset, Set typeFilter, Set attributeFilter) {
		this(source, false, null, charset, typeFilter, attributeFilter);
	}

	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param outputIDs include annotation IDs in the output?
	 * @param charset the charset to use for encoding the chars to bytes
	 * @param typeFilter a set containing the types of the annotations to
	 *            include in the output (specifying null will include all
	 *            attributes)
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tags (specifying null will include all
	 *            attributes)
	 */
	public AnnotationInputStream(QueriableAnnotation source, boolean outputIDs, Charset charset, Set typeFilter, Set attributeFilter) {
		this(source, outputIDs, null, charset, typeFilter, attributeFilter);
	}

	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param indent the String to insert for each level of indentation
	 *            (specifying null will result in no indentation)
	 * @param charset the name of the charset to use for encoding the chars to
	 *            bytes
	 * @param typeFilter a set containing the types of the annotations to
	 *            include in the output (specifying null will include all
	 *            attributes)
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tags (specifying null will include all
	 *            attributes)
	 */
	public AnnotationInputStream(QueriableAnnotation source, String indent, String charset, Set typeFilter, Set attributeFilter) {
		this(source, false, indent, ((charset == null) ? Charset.defaultCharset() : Charset.forName(charset)), typeFilter, attributeFilter);
	}

	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param outputIDs include annotation IDs in the output?
	 * @param indent the String to insert for each level of indentation
	 *            (specifying null will result in no indentation)
	 * @param charset the name of the charset to use for encoding the chars to
	 *            bytes
	 * @param typeFilter a set containing the types of the annotations to
	 *            include in the output (specifying null will include all
	 *            attributes)
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tags (specifying null will include all
	 *            attributes)
	 */
	public AnnotationInputStream(QueriableAnnotation source, boolean outputIDs, String indent, String charset, Set typeFilter, Set attributeFilter) {
		this(source, outputIDs, indent, ((charset == null) ? Charset.defaultCharset() : Charset.forName(charset)), typeFilter, attributeFilter);
	}
	
	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param indent the String to insert for each level of indentation
	 *            (specifying null will result in no indentation)
	 * @param charset the charset to use for encoding the chars to bytes
	 * @param typeFilter a set containing the types of the annotations to
	 *            include in the output (specifying null will include all
	 *            attributes)
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tags (specifying null will include all
	 *            attributes)
	 */
	public AnnotationInputStream(QueriableAnnotation source, String indent, Charset charset, Set typeFilter, Set attributeFilter) {
		this(source, false, indent, charset, typeFilter, attributeFilter);
	}

	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param outputIDs include annotation IDs in the output?
	 * @param indent the String to insert for each level of indentation
	 *            (specifying null will result in no indentation)
	 * @param charset the charset to use for encoding the chars to bytes
	 * @param typeFilter a set containing the types of the annotations to
	 *            include in the output (specifying null will include all
	 *            attributes)
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tags (specifying null will include all
	 *            attributes)
	 */
	public AnnotationInputStream(QueriableAnnotation source, boolean outputIDs, String indent, Charset charset, Set typeFilter, Set attributeFilter) {
		this.annotationReader = new AnnotationReader(source, outputIDs, indent, typeFilter, attributeFilter);
		this.encoding = ((charset == null) ? Charset.defaultCharset() : charset);
		this.byteBuffer = this.encoding.encode(CharBuffer.wrap("<?xml version=\"1.0\" encoding=\"" + this.encoding.name() + "\"?>\n"));
	}
	
	/* 
	 * @see java.io.Reader#close()
	 */
	public void close() throws IOException {
		this.annotationReader.close();
	}
	
	/*
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		if ((this.byteBuffer == null) || (this.bbPosition == this.byteBuffer.limit())) {
			char[] ch = new char[1024];
			
			int read = this.annotationReader.read(ch, 0, 1024);
			if (read == -1)
				return -1;
			
			this.byteBuffer = this.encoding.encode(CharBuffer.wrap(ch, 0, read));
			this.bbPosition = 0;
		}
		if ((this.byteBuffer == null) || (this.bbPosition == this.byteBuffer.limit()))
			return -1;
		else return this.byteBuffer.get(this.bbPosition++);
	}
}
