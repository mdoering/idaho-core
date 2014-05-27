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
package de.uka.ipd.idaho.gamta.util.constants;


import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.MutableAnnotation;

/**
 * This interface provides constants for annotation types, attribute names, and
 * document property names that are useful in literature analysis. For
 * convenience, it also includes the dedicated annotation type constants
 * declared in the MutableAnnotation and DocumentRoot interfaces.
 * 
 * @author sautter
 */
public interface LiteratureConstants {
	
	/**	the annotation type of a DocumentRoot, the generic root element of all Gamta documents*/
	public static final String DOCUMENT_TYPE = DocumentRoot.DOCUMENT_TYPE;
	
	/**	the attribute or document property holding the ID of a document*/
	public static final String DOCUMENT_ID_ATTRIBUTE = DocumentRoot.DOCUMENT_ID_ATTRIBUTE;
	
	/**	the attribute or document property holding the name of a document, e.g. the name of the file it was loaded from*/
	public static final String DOCUMENT_NAME_ATTRIBUTE = "docName";
	
	/** the attribute or document property holding the title of a document*/
	public static final String DOCUMENT_TITLE_ATTRIBUTE = "docTitle";
	
	/** the attribute or document property holding the author of a document*/
	public static final String DOCUMENT_AUTHOR_ATTRIBUTE = "docAuthor";
	
	/** the attribute or document property holding the creation date of a document*/
	public static final String DOCUMENT_DATE_ATTRIBUTE = "docDate";
	
	/** the attribute holding the URL of the original representation of a document, which may be a pdf, html page, or something else*/
	public static final String DOCUMENT_SOURCE_LINK_ATTRIBUTE = "docSource";
	
	/** the attribute or document property holding the origin of a document (the publisher in case of a book, the journal or newspaper name in case of an article)*/
	public static final String DOCUMENT_ORIGIN_ATTRIBUTE = "docOrigin";
	
	
	/**	the annotation type for marking a first level section in a document*/
	public static final String SECTION_TYPE = MutableAnnotation.SECTION_TYPE;
	
	/**	the annotation type for marking a second level section (sub section) in a document*/
	public static final String SUB_SECTION_TYPE = MutableAnnotation.SUB_SECTION_TYPE;
	
	/**	the annotation type for marking a third level section (sub sub section) in a document*/
	public static final String SUB_SUB_SECTION_TYPE = MutableAnnotation.SUB_SUB_SECTION_TYPE;
	
	/**	the annotation type for marking a paragraph in a document*/
	public static final String PARAGRAPH_TYPE = MutableAnnotation.PARAGRAPH_TYPE;
	
	/**	the annotation type for marking a sentence in the text of a document*/
	public static final String SENTENCE_TYPE = MutableAnnotation.SENTENCE_TYPE;
	
	/** the attribute for further specifying the type of a section, sub section, etc., for instance 'abstract', 'acknowledgements', or 'conclusions'*/
	public static final String TYPE_ATTRIBUTE = "type";
	
	
	/**	the annotation type for marking a footnote in a document*/
	public static final String FOOTNOTE_TYPE = "footnote";
	
	/**	the annotation type for marking a caption in a document (captions of a figure, table, or something else)*/
	public static final String CAPTION_TYPE = "caption";
	
	/**	the annotation type for marking a citation in a document */
	public static final String CITATION_TYPE = "citation";
	
	/**	the annotation type for marking a bibliographic reference in a document*/
	public static final String BIBLIOGRAPHIC_REFERENCE_TYPE = "bibRef";
	
	/**	the annotation type for marking a group of bibliographic references in a document*/
	public static final String BIBLIOGRAPHIC_REFERENCE_GROUP_TYPE = "bibRefGroup";
	
	/** the attribute for a citation or bib_ref holding the link to to the cited document*/
	public static final String CITED_PUBLICATION_URL_ATTRIBUTE = "publicationUrl";
	
	
	/**	the annotation type for marking a page in a print layouted document*/
	public static final String PAGE_TYPE = "page";
	
	/**	the annotation type for marking a page number in a print layouted document*/
	public static final String PAGE_NUMBER_TYPE = "pageNumber";
	
	/**	the annotation type for marking tokens that originates from a page other than the one immediately preceding them in a print layouted document*/
	public static final String PAGE_BREAK_TOKEN_TYPE = "pageBreakToken";
	
	/**	the annotation type for marking a page title in a print layouted document*/
	public static final String PAGE_TITLE_TYPE = "pageTitle";
	
	/**	the annotation type for marking a the representation of the border between two pages in a print layouted document*/
	public static final String PAGE_BORDER_TYPE = "pageBorder";
	
	/**	the string for representing the border between two pages in a print layouted document*/
	public static final String PAGE_BORDER_MARKER = "-------------------------------- page break --------------------------------";
	
	/** the attribute indicating that a page break token marks the beginning of a page in a print layouted document*/
	public static final String PAGE_START_ATTRIBUTE = "start";
	
	/** the attribute holding the ID (zero-based sequential number) of the page some annotation starts on in a print layouted document*/
	public static final String PAGE_ID_ATTRIBUTE = "pageId";
	
	/** the attribute holding the ID (zero-based sequential number) of the page some annotation ends on in a print layouted document*/
	public static final String LAST_PAGE_ID_ATTRIBUTE = "lastPageId";
	
	/** the attribute holding the number of the page some annotation starts on in a print layouted document*/
	public static final String PAGE_NUMBER_ATTRIBUTE = "pageNumber";
	
	/** the attribute holding the number of the page some annotation ends on in a print layouted document*/
	public static final String LAST_PAGE_NUMBER_ATTRIBUTE = "lastPageNumber";
}
