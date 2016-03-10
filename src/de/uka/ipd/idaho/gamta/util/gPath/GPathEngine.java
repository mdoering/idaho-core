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
package de.uka.ipd.idaho.gamta.util.gPath;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.PatternSyntaxException;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.MutableTokenSequence.TokenSequenceEvent;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceListener;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.defaultImplementation.PlainTokenSequence;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.InvalidArgumentsException;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.UndefinedFunctionException;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.UndefinedOperatorException;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.VariableNotBoundException;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathAnnotationSet;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathBoolean;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathNumber;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathString;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * Default implementation of a GamtaPath engine.
 * 
 * @author sautter
 */
public class GPathEngine implements GPathConstants {
	
	private final boolean isDefaultEngine;
	
	/**	Constructor
	 */
	public GPathEngine() {
		this(false);
	}
	
	/**	Constructor
	 */
	GPathEngine(boolean isDefaultEngine) {
		this.isDefaultEngine = isDefaultEngine;
	}
	
	/**
	 * Evaluate a GPath query on a document.
	 * @param context the document to evaluate the query on
	 * @param path a String representing the GPath to evaluate
	 * @param variableBindings the variable bindings which are currently valid
	 * @return an array containing the Annotations resulting form the evaluation
	 * @throws GPathException
	 */
	public QueriableAnnotation[] evaluatePath(QueriableAnnotation context, String path, GPathVariableResolver variableBindings) throws GPathException {
		return this.evaluatePath(context, GPathParser.parsePath(path), variableBindings);
	}
		
	/**
	 * Evaluate a GPath query on a document.
	 * @param context the document to evaluate the query on
	 * @param path the GPath to evaluate
	 * @param variableBindings the variable bindings which are currently valid
	 * @return an array containing the Annotations resulting form the evaluation
	 * @throws GPathException
	 */
	public QueriableAnnotation[] evaluatePath(QueriableAnnotation context, GPath path, GPathVariableResolver variableBindings) throws GPathException {
		
		//	check resolver
		if (variableBindings == null)
			variableBindings = GPath.getDummyVariableResolver();
		
		//	wrap query context DocumentPart
		GPathDocument wrappedContext = new GPathDocument(context);
		
		//	execute query
		GPathAnnotationSet resultSet = new GPathAnnotationSet();
		resultSet.add(wrappedContext);
		resultSet = this.evaluatePath(wrappedContext, path, resultSet, variableBindings);
		
		//	get result annotation IDs, wrap results that are not real annotations
		HashSet resultIDs = new HashSet();
		ArrayList nonTextResultAnnotations = new ArrayList();
		for (int a = 0; a < resultSet.size(); a++) {
			Annotation resultAnnotation = resultSet.get(a);
			
			if (resultAnnotation instanceof GPathTokenAnnotation)
				nonTextResultAnnotations.add(new TokenAnnotation(context, ((GPathTokenAnnotation) resultAnnotation).getAbsoluteStartIndex()));
			
			else if (resultAnnotation instanceof GPathAttributeAnnotation)
				nonTextResultAnnotations.add(resultAnnotation);
			
			else resultIDs.add(resultAnnotation.getAnnotationID());
		}
		
		//	collect result annotations by ID
		ArrayList resultList = new ArrayList();
		if (resultIDs.remove(context.getAnnotationID())) // use remove() to avoid duplicates
			resultList.add(context);
		QueriableAnnotation[] contextAnnotations = context.getAnnotations();
		for (int c = 0; c < contextAnnotations.length; c++)
			if (resultIDs.remove(contextAnnotations[c].getAnnotationID()))
				resultList.add(contextAnnotations[c]);
		resultList.addAll(nonTextResultAnnotations);
		Collections.sort(resultList, AnnotationUtils.getComparator(context.getAnnotationNestingOrder()));
		
		//	return result
		return ((QueriableAnnotation[]) resultList.toArray(new QueriableAnnotation[resultList.size()]));
	}
	
	private GPathAnnotationSet evaluatePath(GPathDocument document, GPath path, GPathAnnotation startAnnotation, GPathVariableResolver variableBindings) throws GPathException {
		GPathAnnotationSet result = new GPathAnnotationSet();
		result.add(startAnnotation);
		return this.evaluatePath(document, path, result, variableBindings);
	}
	
	private GPathAnnotationSet evaluatePath(GPathDocument document, GPath path, GPathAnnotationSet startAnnotations, GPathVariableResolver variableBindings) throws GPathException {
		
		//	empty path
		if (path.steps.length == 0) return startAnnotations;
		
		//	variable to resolve
		if (path.steps[0].annotationTest.startsWith("$")) {
			GPathObject vv = variableBindings.getVariable(path.steps[0].annotationTest);
			if (vv == null) throw new VariableNotBoundException("The variable '" + path.steps[0].annotationTest + "' has been referenced, but is not bound to a value.");
			else if (vv instanceof GPathAnnotationSet) {
				
				//	make sure all the annotations are properly wrapped
				GPathAnnotationSet gpas = ((GPathAnnotationSet) vv);
				GPathAnnotationSet gpas2 = new GPathAnnotationSet(gpas.isReverseDocOrder);
				for (int a = 0; a < gpas.size(); a++) {
					QueriableAnnotation an = gpas.get(a);
					if (an instanceof GPathAnnotation)
						gpas2.add(an);
					else gpas2.add(new GPathAnnotationView(an, document, document));
				}
				
				//	evaluate remaining steps
				GPathAnnotationSet result = gpas2;
				for (int s = 1; s < path.steps.length; s++)
					result = this.evaluateStep(document, path.steps[s], result, variableBindings);
				return result;
			}
			else throw new InvalidArgumentsException("The variable '" + path.steps[0].annotationTest + "' has been referenced as the start of a path expression, but is not bound to an annotation set.");
		}
		
		//	document reference (path starts with a slash)
		else if (("descendant-or-self".equals(path.steps[0].axis) && ("annotation()".equals(path.steps[0].annotationTest) || "*".equals(path.steps[0].annotationTest))) || ("child".equals(path.steps[0].axis) && DocumentRoot.DOCUMENT_TYPE.equals(path.steps[0].annotationTest))) {
			GPathAnnotationSet result = document.getPathResult(path.toString());
			if (result == null) {
				result = new GPathAnnotationSet();
				result.add(document);
				if (path.steps[0].predicates != null) {
					for (int p = 0; p < path.steps[0].predicates.length; p++)
						result = this.applyPredicate(document, path.steps[0].predicates[p], result, variableBindings);
				}
				for (int s = 1; s < path.steps.length; s++)
					result = this.evaluateStep(document, path.steps[s], result, variableBindings);
				document.cachePathResult(path.toString(), result);
			}
			return result;
		}
		
		//	regular path
		else {
			GPathAnnotationSet result = startAnnotations;
			for (int s = 0; s < path.steps.length; s++)
				result = this.evaluateStep(document, path.steps[s], result, variableBindings);
			return result;
		}
	}
	
	private GPathAnnotationSet evaluateStep(GPathDocument document, GPathStep step, GPathAnnotationSet startAnnotations, GPathVariableResolver variableBindings) throws GPathException {
		if (step.axis == null) step.axis = "child";
		GPathAnnotationSet result = new GPathAnnotationSet();
		
		String filterType;
		if ("annotation()".equals(step.annotationTest) || "*".equals(step.annotationTest))
			filterType = null;
		else filterType = step.annotationTest;
		
		for (int a = 0; a < startAnnotations.size(); a++) {
			GPathAnnotationSet annotationResult = new GPathAnnotationSet();
			QueriableAnnotation annotation = startAnnotations.get(a);
			
			//	evaluate axis and annotation test
			if (step.axis.startsWith("descendant")) {
				QueriableAnnotation[] annotations = annotation.getAnnotations(filterType);
				for (int an = 0; an < annotations.length; an++)
					if (!annotation.getAnnotationID().equals(annotations[an].getAnnotationID()))
						annotationResult.add(annotations[an]);
			}
			
			else if (step.axis.startsWith("preceding-sibling"))
				annotationResult = getPrecedingSibling(document, annotation, filterType);
			
			else if (step.axis.startsWith("following-sibling"))
				annotationResult = getFollowingSibling(document, annotation, filterType);
				
			else if (step.axis.startsWith("interleaving-"))
				annotationResult = getInterleavingSibling(document, annotation, filterType, !step.axis.endsWith("right"), !step.axis.endsWith("left"));
				
			else if ("child".equals(step.axis)) {
				QueriableAnnotation[] annotations = annotation.getAnnotations(filterType);
				for (int an = 0; an < annotations.length; an++)
					if (!annotation.getAnnotationID().equals(annotations[an].getAnnotationID()))
						annotationResult.add(annotations[an]);
			}
			
			else if ("attribute".equals(step.axis)) {
				if (filterType == null) {
					String[] attributeNames = annotation.getAttributeNames();
					for (int an = 0; an < attributeNames.length; an++) {
						Object attribute = annotation.getAttribute(attributeNames[an]);
						if ((attribute != null) && (attribute instanceof String)) {
							TokenSequence attributeTokens = annotation.getTokenizer().tokenize(attribute.toString());
							annotationResult.add(new GPathAttributeAnnotation(annotation, attributeNames[an], attributeTokens));
						}
					}
				}
				else {
					Object attribute = annotation.getAttribute(filterType);
					if ((attribute != null) && (attribute instanceof String)) {
						TokenSequence attributeTokens = annotation.getTokenizer().tokenize(attribute.toString());
						annotationResult.add(new GPathAttributeAnnotation(annotation, filterType, attributeTokens));
					}
				}
			}
			
			else if (Token.TOKEN_ANNOTATION_TYPE.equals(step.axis)) {
				if (filterType == null) {
					for (int t = 0; t < annotation.size(); t++)
						annotationResult.add(new GPathTokenAnnotation(annotation, t));
				}
				else if ("first".equals(filterType))
					annotationResult.add(new GPathTokenAnnotation(annotation, 0));
				
				else if ("last".equals(filterType))
					annotationResult.add(new GPathTokenAnnotation(annotation, (annotation.size() - 1)));
				
				else if ("text".equals(filterType)) {
					for (int t = 0; t < annotation.size(); t++)
						if (Gamta.isWord(annotation.tokenAt(t)) || Gamta.isNumber(annotation.tokenAt(t)))
							annotationResult.add(new GPathTokenAnnotation(annotation, t));
				}
				else if ("word".equals(filterType)) {
					for (int t = 0; t < annotation.size(); t++)
						if (Gamta.isWord(annotation.tokenAt(t)))
							annotationResult.add(new GPathTokenAnnotation(annotation, t));
				}
				else if ("number".equals(filterType)) {
					for (int t = 0; t < annotation.size(); t++)
						if (Gamta.isNumber(annotation.tokenAt(t)))
							annotationResult.add(new GPathTokenAnnotation(annotation, t));
				}
				else if ("punctuation".equals(filterType)) {
					for (int t = 0; t < annotation.size(); t++)
						if (Gamta.isPunctuation(annotation.tokenAt(t)))
							annotationResult.add(new GPathTokenAnnotation(annotation, t));
				}
				else if ("sentenceEnd".equals(filterType)) {
					for (int t = 0; t < annotation.size(); t++)
						if (Gamta.isSentenceEnd(annotation.tokenAt(t)))
							annotationResult.add(new GPathTokenAnnotation(annotation, t));
				}
				else if ("bracket".equals(filterType)) {
					for (int t = 0; t < annotation.size(); t++)
						if (Gamta.isBracket(annotation.tokenAt(t)))
							annotationResult.add(new GPathTokenAnnotation(annotation, t));
				}
				else if ("openingBracket".equals(filterType)) {
					for (int t = 0; t < annotation.size(); t++)
						if (Gamta.isOpeningBracket(annotation.tokenAt(t)))
							annotationResult.add(new GPathTokenAnnotation(annotation, t));
				}
				else if ("closingBracket".equals(filterType)) {
					for (int t = 0; t < annotation.size(); t++)
						if (Gamta.isClosingBracket(annotation.tokenAt(t)))
							annotationResult.add(new GPathTokenAnnotation(annotation, t));
				}
			}
			
			if (step.axis.endsWith("self"))
				annotationResult.add(annotation);
			else if ("parent".equals(step.axis)) {
				GPathAnnotation parent = ((GPathAnnotation) annotation).getParent();
				if (parent != null)
					annotationResult.add(parent);
			}
			
			//	don't filter by type on Token axis
			if (!Token.TOKEN_ANNOTATION_TYPE.equals(step.axis))
				annotationResult = annotationResult.filterByType(filterType);
			
			//	apply predicates
			if (step.predicates != null) {
				for (int p = 0; p < step.predicates.length; p++)
					annotationResult = this.applyPredicate(document, step.predicates[p], annotationResult, variableBindings);
			}
			
			//	copy annotations to result
			result.addAll(annotationResult);
		}
		
		return result;
	}
	
	private static GPathAnnotationSet getPrecedingSibling(GPathAnnotation document, QueriableAnnotation startAnnotation, String filterType) {
		GPathAnnotationSet result = new GPathAnnotationSet(true);
		if (startAnnotation instanceof GPathAnnotation) {
			QueriableAnnotation parent = ((GPathAnnotation) startAnnotation).getParent();
			if (parent == null)
				return result;
			QueriableAnnotation[] annotations = parent.getAnnotations(filterType);
			for (int a = 0; a < annotations.length; a++) {
				if (annotations[a].getEndIndex() <= startAnnotation.getStartIndex())
					result.add(annotations[a]);
				else if (startAnnotation.getStartIndex() <= annotations[a].getStartIndex())
					break;
			}
		}
		return result;
	}
	
	private static GPathAnnotationSet getFollowingSibling(GPathAnnotation document, QueriableAnnotation startAnnotation, String filterType) {
		GPathAnnotationSet result = new GPathAnnotationSet(false);
		if (startAnnotation instanceof GPathAnnotation) {
			QueriableAnnotation parent = ((GPathAnnotation) startAnnotation).getParent();
			if (parent == null)
				return result;
			QueriableAnnotation[] annotations = parent.getAnnotations(filterType);
			for (int a = 0; a < annotations.length; a++) {
				if (startAnnotation.getEndIndex() <= annotations[a].getStartIndex())
					result.add(annotations[a]);
			}
		}
		return result;
	}
	
	private static GPathAnnotationSet getInterleavingSibling(GPathAnnotation document, QueriableAnnotation startAnnotation, String filterType, boolean includeLeft, boolean includeRight) {
		GPathAnnotationSet result = new GPathAnnotationSet(false);
		if (startAnnotation instanceof GPathAnnotation) {
			QueriableAnnotation parent = ((GPathAnnotation) startAnnotation).getParent();
			if (parent == null)
				return result;
			QueriableAnnotation[] annotations = parent.getAnnotations(filterType);
			for (int a = 0; a < annotations.length; a++) {
				if (annotations[a].getEndIndex() <= startAnnotation.getStartIndex())
					continue;
				if (startAnnotation.getEndIndex() <= annotations[a].getStartIndex())
					break;
				if (includeLeft && (annotations[a].getStartIndex() < startAnnotation.getStartIndex()) && (annotations[a].getEndIndex() < startAnnotation.getEndIndex()))
					result.add(annotations[a]);
				else if (includeRight && (startAnnotation.getStartIndex() < annotations[a].getStartIndex()) && (startAnnotation.getEndIndex() < annotations[a].getEndIndex()))
					result.add(annotations[a]);
			}
		}
		return result;
	}
	
	private GPathAnnotationSet applyPredicate(GPathDocument document, GPathPredicate predicate, GPathAnnotationSet annotationSet, GPathVariableResolver variableBindings) throws GPathException {
		if (predicate.expression == null) return annotationSet;
		GPathAnnotationSet resultAnnotationSet = new GPathAnnotationSet();
		int size = annotationSet.size();
		for (int n = 0; n < size; n++) {
			QueriableAnnotation annotation = annotationSet.get(n);
			if (annotation instanceof GPathAnnotation) {
				GPathObject xpo = this.evaluateExpression(document, predicate.expression, ((GPathAnnotation) annotation), (n + 1), size, variableBindings);
				if (xpo instanceof GPathNumber) {
					if (xpo.asNumber().value == (n + 1))
						resultAnnotationSet.add(annotation);
				}
				else if (xpo.asBoolean().value)
					resultAnnotationSet.add(annotation);
			}
		}
		return resultAnnotationSet;
	}
	
	/**
	 * Evaluate a GPath expression on a document.
	 * @param context the document to evaluate the expression on
	 * @param expression the GPath expression to evaluate
	 * @param variableBindings the variable bindings which are currently valid
	 * @return the resulting GPathObject
	 */
	public GPathObject evaluateExpression(GPathExpression expression, QueriableAnnotation context, GPathVariableResolver variableBindings) throws GPathException {
		
		//	check resolver
		if (variableBindings == null)
			variableBindings = GPath.getDummyVariableResolver();
		
		//	wrap query context DocumentPart
		GPathDocument wrappedContext = new GPathDocument(context);
		
		//	execute query
		return this.evaluateExpression(wrappedContext, expression, wrappedContext, 1, 1, variableBindings);
	}
	
	private GPathObject evaluateExpression(GPathDocument document, GPathExpression expression, GPathAnnotation contextAnnotation, int contextPosition, int contextSize, GPathVariableResolver variableBindings) throws GPathException {
		if (expression instanceof GPathUnaryExpression)
			return this.evaluateUnaryExpression(document, ((GPathUnaryExpression) expression), contextAnnotation, contextPosition, contextSize, variableBindings);
		else if (expression instanceof GPathBinaryExpression)
			return this.evaluateBinaryExpression(document, ((GPathBinaryExpression) expression), contextAnnotation, contextPosition, contextSize, variableBindings);
		else return new GPathBoolean(false);
	}
	
	private GPathObject evaluateUnaryExpression(GPathDocument document, GPathUnaryExpression expression, GPathAnnotation contextAnnotation, int contextPosition, int contextSize, GPathVariableResolver variableBindings) throws GPathException {
		if (expression.literal != null) return expression.literal;
		if (expression.number != null) return (expression.isNegative ? new GPathNumber(-expression.number.value) : expression.number);
		if (expression.variableName != null) {
			GPathObject vv = variableBindings.getVariable(expression.variableName);
			if (vv == null) throw new VariableNotBoundException("The variable '" + expression.variableName + "' has been referenced, but is not bound to a value.");
			else return vv;
		}
		if (expression.enclosedExpression != null) {
			GPathObject result = this.evaluateExpression(document, expression.enclosedExpression, contextAnnotation, contextPosition, contextSize, variableBindings);
			if (result instanceof GPathNumber)
				return (expression.isNegative ? new GPathNumber(-result.asNumber().value) : result);
			else return result;//evaluateExpression(expression.enclosedExpression, contextAnnotation, contextPosition, contextSize, variableBindings);
		}
		if (expression.functionName != null) {
			
			GPathObject[] args = new GPathObject[expression.functionArgs.length];
			for (int a = 0; a < args.length; a++)
				args[a] = this.evaluateExpression(document, expression.functionArgs[a], contextAnnotation, contextPosition, contextSize, variableBindings);
			GPathObject xpo = this.executeFunction(expression.functionName, contextAnnotation, contextPosition, contextSize, args);
			
			if (xpo instanceof GPathNumber)
				return (expression.isNegative ? new GPathNumber(-xpo.asNumber().value) : xpo);
			
			if ((expression.predicates != null) && (expression.predicates.length != 0)) {
				if (!(xpo instanceof GPathAnnotationSet)) throw new InvalidArgumentsException("Predicates are applicable only for annotationSets.");
				GPathAnnotationSet annotationSet = ((GPathAnnotationSet) xpo);
				for (int p = 0; p < expression.predicates.length; p++)
					annotationSet = this.applyPredicate(document, expression.predicates[p], annotationSet, variableBindings);
				xpo = annotationSet;
			}
			
			if ((expression.pathExpression != null) && (expression.pathExpression.steps != null) && (expression.pathExpression.steps.length != 0)) {
				if (!(xpo instanceof GPathAnnotationSet)) throw new InvalidArgumentsException("Path expressions are applicable only for annotationSets.");
				GPathAnnotationSet annotationSet = ((GPathAnnotationSet) xpo);
				return this.evaluatePath(document, expression.pathExpression, annotationSet, variableBindings);
			}
			
			return xpo;
		}
		if (expression.pathExpression != null) return this.evaluatePath(document, expression.pathExpression, contextAnnotation, variableBindings);
		if ((expression.partExpressions != null) && (expression.partExpressions.length != 0)) {
			GPathAnnotationSet result = new GPathAnnotationSet();
			for (int p = 1; p < expression.partExpressions.length; p++) {
				GPathObject xpo = this.evaluateExpression(document, expression.partExpressions[p], contextAnnotation, contextPosition, contextSize, variableBindings);
				if (!(xpo instanceof GPathAnnotationSet)) throw new InvalidArgumentsException("Union expressions are applicable only for annotationSets.");
				GPathAnnotationSet annotationSet = ((GPathAnnotationSet) xpo);
				for (int n = 0; n < expression.predicates.length; n++)
					result.add(annotationSet.get(n));
			}
			return result;
		}
		return new GPathBoolean(false);
	}
	
	private GPathObject evaluateBinaryExpression(GPathDocument document, GPathBinaryExpression expression, GPathAnnotation contextAnnotation, int contextPosition, int contextSize, GPathVariableResolver variableBindings) throws GPathException {
		if (expression.leftExpression == null) return new GPathBoolean(true);
		
		//	get left result
		GPathObject left = this.evaluateExpression(document, expression.leftExpression, contextAnnotation, contextPosition, contextSize, variableBindings);
		if (expression.rightExpression == null) return left;
		
		//	evaluate OR and AND operator
		if ("or".equals(expression.operator)) {
			boolean res = left.asBoolean().value;
			if (res) return new GPathBoolean(true);
			GPathObject right = this.evaluateExpression(document, expression.rightExpression, contextAnnotation, contextPosition, contextSize, variableBindings);
			return new GPathBoolean(right.asBoolean().value);
		}
		else if ("and".equals(expression.operator)) {
			boolean res = left.asBoolean().value;
			if (!res) return new GPathBoolean(false);
			GPathObject right = this.evaluateExpression(document, expression.rightExpression, contextAnnotation, contextPosition, contextSize, variableBindings);
			return new GPathBoolean(right.asBoolean().value);
		}
		
		//	get right result
		GPathObject right = this.evaluateExpression(document, expression.rightExpression, contextAnnotation, contextPosition, contextSize, variableBindings);
		
		//	check if annotationSets involved
		boolean leftIsSet = (left instanceof GPathAnnotationSet);
		boolean rightIsSet = (right instanceof GPathAnnotationSet);
		
		//	evaluate = and != operators
		if ("!=".equals(expression.operator)) {
			if (leftIsSet && rightIsSet) {
				GPathString[] leftStrings = stringValues((GPathAnnotationSet) left);
				GPathString[] rightStrings = stringValues((GPathAnnotationSet) right);
				
				if ((leftStrings.length * rightStrings.length) == 0)
					return new GPathBoolean(false);
				
				HashSet leftSet = new HashSet();
				for (int l = 0; l < leftStrings.length; l++)
					leftSet.add(leftStrings[l].value);
				HashSet rightSet = new HashSet();
				for (int r = 0; r < rightStrings.length; r++)
					rightSet.add(rightStrings[r].value);
				
				return new GPathBoolean(
							((leftSet.size() * rightSet.size()) > 1) // both sets not empty, and at least one has more than one element
							||
							(leftSet.addAll(rightSet) || rightSet.addAll(leftSet)) // one set contains an element not contained in the other one
						);
			}
			else if (leftIsSet) {
				if (right instanceof GPathBoolean) return new GPathBoolean(right.asBoolean().value != left.asBoolean().value);
				GPathString[] leftStrings = stringValues((GPathAnnotationSet) left);
				if (right instanceof GPathNumber) {
					for (int l = 0; l < leftStrings.length; l++)
						if (right.asNumber().value != leftStrings[l].asNumber().value)
							return new GPathBoolean(true);
					return new GPathBoolean(false);
				}
				else {
					for (int l = 0; l < leftStrings.length; l++)
						if (!right.asString().value.equals(leftStrings[l].value))
							return new GPathBoolean(true);
					return new GPathBoolean(false);
				}
			}
			else if (rightIsSet) {
				if (left instanceof GPathBoolean) return new GPathBoolean(left.asBoolean().value != right.asBoolean().value);
				GPathString[] rightStrings = stringValues((GPathAnnotationSet) right);
				if (left instanceof GPathNumber) {
					for (int r = 0; r < rightStrings.length; r++)
						if (left.asNumber().value != rightStrings[r].asNumber().value)
							return new GPathBoolean(true);
					return new GPathBoolean(false);
				}
				else {
					for (int r = 0; r < rightStrings.length; r++)
						if (!left.asString().value.equals(rightStrings[r].value))
							return new GPathBoolean(true);
					return new GPathBoolean(false);
				}
			}
			else {
				if ((left instanceof GPathBoolean) || (right instanceof GPathBoolean))
					return new GPathBoolean(left.asBoolean().value != right.asBoolean().value);
				else if ((left instanceof GPathNumber) || (right instanceof GPathNumber))
					return new GPathBoolean(left.asNumber().value != right.asNumber().value);
				else return new GPathBoolean(!left.asString().value.equals(right.asString().value));
			}
		}
		else if ("=".equals(expression.operator)) {
			if (leftIsSet && rightIsSet) {
				GPathString[] leftStrings = stringValues((GPathAnnotationSet) left);
				GPathString[] rightStrings = stringValues((GPathAnnotationSet) right);
				
				if ((leftStrings.length * rightStrings.length) == 0)
					return new GPathBoolean(false);
				
				HashSet leftSet = new HashSet();
				for (int l = 0; l < leftStrings.length; l++)
					leftSet.add(leftStrings[l].value);
				for (int r = 0; r < rightStrings.length; r++)
					if (leftSet.contains(rightStrings[r].value))
						return new GPathBoolean(true);
				return new GPathBoolean(false);
			}
			else if (leftIsSet) {
				if (right instanceof GPathBoolean) return new GPathBoolean(right.asBoolean().value == left.asBoolean().value);
				GPathString[] leftStrings = stringValues((GPathAnnotationSet) left);
				if (right instanceof GPathNumber) {
					for (int l = 0; l < leftStrings.length; l++)
						if (right.asNumber().value == leftStrings[l].asNumber().value)
							return new GPathBoolean(true);
					return new GPathBoolean(false);
				}
				else {
					for (int l = 0; l < leftStrings.length; l++)
						if (right.asString().value.equals(leftStrings[l].value))
							return new GPathBoolean(true);
					return new GPathBoolean(false);
				}
			}
			else if (rightIsSet) {
				if (left instanceof GPathBoolean) return new GPathBoolean(left.asBoolean().value == right.asBoolean().value);
				GPathString[] rightStrings = stringValues((GPathAnnotationSet) right);
				if (left instanceof GPathNumber) {
					for (int r = 0; r < rightStrings.length; r++)
						if (left.asNumber().value == rightStrings[r].asNumber().value)
							return new GPathBoolean(true);
					return new GPathBoolean(false);
				}
				else {
					for (int r = 0; r < rightStrings.length; r++)
						if (left.asString().value.equals(rightStrings[r].value))
							return new GPathBoolean(true);
					return new GPathBoolean(false);
				}
			}
			else {
				if ((left instanceof GPathBoolean) || (right instanceof GPathBoolean))
					return new GPathBoolean(left.asBoolean().value == right.asBoolean().value);
				else if ((left instanceof GPathNumber) || (right instanceof GPathNumber))
					return new GPathBoolean(left.asNumber().value == right.asNumber().value);
				else return new GPathBoolean(left.asString().value.equals(right.asString().value));
			}
		}
		
		//	evaluate <, <=, >= and > operators
		if ("<".equals(expression.operator)) {
			if (leftIsSet && rightIsSet) {
				GPathString[] leftStrings = stringValues((GPathAnnotationSet) left);
				GPathString[] rightStrings = stringValues((GPathAnnotationSet) right);
				
				if ((leftStrings.length * rightStrings.length) == 0)
					return new GPathBoolean(false);
				
				double leftMin = Double.MAX_VALUE;
				for (int l = 0; l < leftStrings.length; l++)
					leftMin = Math.min(leftMin, leftStrings[l].asNumber().value);
				for (int r = 0; r < rightStrings.length; r++)
					if (leftMin < rightStrings[r].asNumber().value)
						return new GPathBoolean(true);
				return new GPathBoolean(false);
			}
			else if (leftIsSet) {
				GPathString[] leftStrings = stringValues((GPathAnnotationSet) left);
				for (int l = 0; l < leftStrings.length; l++)
					if (leftStrings[l].asNumber().value < right.asNumber().value)
						return new GPathBoolean(true);
				return new GPathBoolean(false);
			}
			else if (rightIsSet) {
				GPathString[] rightStrings = stringValues((GPathAnnotationSet) right);
				for (int r = 0; r < rightStrings.length; r++)
					if (left.asNumber().value < rightStrings[r].asNumber().value)
						return new GPathBoolean(true);
				return new GPathBoolean(false);
			}
			else return new GPathBoolean(left.asNumber().value < right.asNumber().value);
		}
		else if ("<=".equals(expression.operator)) {
			if (leftIsSet && rightIsSet) {
				GPathString[] leftStrings = stringValues((GPathAnnotationSet) left);
				GPathString[] rightStrings = stringValues((GPathAnnotationSet) right);
				
				if ((leftStrings.length * rightStrings.length) == 0)
					return new GPathBoolean(false);
				
				double leftMin = Double.MAX_VALUE;
				for (int l = 0; l < leftStrings.length; l++)
					leftMin = Math.min(leftMin, leftStrings[l].asNumber().value);
				for (int r = 0; r < rightStrings.length; r++)
					if (leftMin <= rightStrings[r].asNumber().value)
						return new GPathBoolean(true);
				return new GPathBoolean(false);
			}
			else if (leftIsSet) {
				GPathString[] leftStrings = stringValues((GPathAnnotationSet) left);
				for (int l = 0; l < leftStrings.length; l++)
					if (leftStrings[l].asNumber().value <= right.asNumber().value)
						return new GPathBoolean(true);
				return new GPathBoolean(false);
			}
			else if (rightIsSet) {
				GPathString[] rightStrings = stringValues((GPathAnnotationSet) right);
				for (int r = 0; r < rightStrings.length; r++)
					if (left.asNumber().value <= rightStrings[r].asNumber().value)
						return new GPathBoolean(true);
				return new GPathBoolean(false);
			}
			else return new GPathBoolean(left.asNumber().value <= right.asNumber().value);
		}
		else if (">=".equals(expression.operator)) {
			if (leftIsSet && rightIsSet) {
				GPathString[] leftStrings = stringValues((GPathAnnotationSet) left);
				GPathString[] rightStrings = stringValues((GPathAnnotationSet) right);
				
				if ((leftStrings.length * rightStrings.length) == 0)
					return new GPathBoolean(false);
				
				double rightMin = Double.MAX_VALUE;
				for (int r = 0; r < rightStrings.length; r++)
					rightMin = Math.min(rightMin, rightStrings[r].asNumber().value);
				for (int l = 0; l < leftStrings.length; l++)
					if (leftStrings[l].asNumber().value >= rightMin)
						return new GPathBoolean(true);
				return new GPathBoolean(false);
			}
			else if (leftIsSet) {
				GPathString[] leftStrings = stringValues((GPathAnnotationSet) left);
				for (int l = 0; l < leftStrings.length; l++)
					if (leftStrings[l].asNumber().value >= right.asNumber().value)
						return new GPathBoolean(true);
				return new GPathBoolean(false);
			}
			else if (rightIsSet) {
				GPathString[] rigthStrings = stringValues((GPathAnnotationSet) right);
				for (int r = 0; r < rigthStrings.length; r++)
					if (left.asNumber().value >= rigthStrings[r].asNumber().value)
						return new GPathBoolean(true);
				return new GPathBoolean(false);
			}
			else return new GPathBoolean(left.asNumber().value >= right.asNumber().value);
		}
		else if (">".equals(expression.operator)) {
			if (leftIsSet && rightIsSet) {
				GPathString[] leftStrings = stringValues((GPathAnnotationSet) left);
				GPathString[] rightStrings = stringValues((GPathAnnotationSet) right);
				
				if ((leftStrings.length * rightStrings.length) == 0)
					return new GPathBoolean(false);
				double rightMin = Double.MAX_VALUE;
				for (int r = 0; r < rightStrings.length; r++)
					rightMin = Math.min(rightMin, rightStrings[r].asNumber().value);
				for (int l = 0; l < leftStrings.length; l++)
					if (leftStrings[l].asNumber().value > rightMin)
						return new GPathBoolean(true);
				return new GPathBoolean(false);
			}
			else if (leftIsSet) {
				GPathString[] leftStrings = stringValues((GPathAnnotationSet) left);
				for (int l = 0; l < leftStrings.length; l++)
					if (leftStrings[l].asNumber().value > right.asNumber().value)
						return new GPathBoolean(true);
				return new GPathBoolean(false);
			}
			else if (rightIsSet) {
				GPathString[] rightStrings = stringValues((GPathAnnotationSet) right);
				for (int r = 0; r < rightStrings.length; r++)
					if (left.asNumber().value > rightStrings[r].asNumber().value)
						return new GPathBoolean(true);
				return new GPathBoolean(false);
			}
			else return new GPathBoolean(left.asNumber().value > right.asNumber().value);
		}
		
		//	evaluate + and - operators
		if ("+".equals(expression.operator))
			return new GPathNumber(left.asNumber().value + right.asNumber().value);
		
		else if ("-".equals(expression.operator))
			return new GPathNumber(left.asNumber().value - right.asNumber().value);
		
		
		//	evaluate *, MOD and DIV operators
		if ("*".equals(expression.operator))
			return new GPathNumber(left.asNumber().value * right.asNumber().value);
		
		else if ("mod".equals(expression.operator))
			return new GPathNumber(left.asNumber().value % right.asNumber().value);
		
		else if ("div".equals(expression.operator))
			return new GPathNumber(left.asNumber().value / right.asNumber().value);
		
		throw new UndefinedOperatorException("The operator '" + expression.operator + "' is not defined.");
	}
	
	private static GPathString stringValue(Annotation annotation) {
		if (annotation == null) return new GPathString("");
		return new GPathString(annotation.getValue());
	}
	
	private static GPathString[] stringValues(GPathAnnotationSet annotationSet) {
		if (annotationSet == null) return new GPathString[0];
		GPathString[] strings = new GPathString[annotationSet.size()];
		for (int n = 0; n < annotationSet.size(); n++)
			strings[n] = stringValue(annotationSet.get(n));
		return strings;
	}
	
	private GPathObject executeFunction(String functionName, GPathAnnotation contextAnnotation, int contextPosition, int contextSize, GPathObject[] args) throws GPathException {
		
		//	execute custom function if there is one for the specified name
		if (this.customFunctions.containsKey(functionName)) try {
			GPathFunction function = ((GPathFunction) this.customFunctions.get(functionName));
			return function.execute(contextAnnotation, contextPosition, contextSize, args);
		} catch (Exception e) {}
		
		//	boolean functions
		if ("boolean".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'boolean' requires 1 argument(s) of type(s) GPathObject.");
			return args[0].asBoolean();
		}
		else if ("true".equalsIgnoreCase(functionName)) {
			if (args.length != 0)
				throw new InvalidArgumentsException("The function 'true' requires 0 argument(s).");
			return new GPathBoolean(true);
		}
		else if ("false".equalsIgnoreCase(functionName)) {
			if (args.length != 0)
				throw new InvalidArgumentsException("The function 'false' requires 0 argument(s).");
			return new GPathBoolean(false);
		}
		
		//	negator function
		else if ("not".equalsIgnoreCase(functionName))
			return new GPathBoolean(!args[0].asBoolean().value);
		
		//	GAMTA string classifier functions
		else if ("isWord".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'isWord' requires 1 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.isWord(args[0].asString().value));
		}
		else if ("isLowerCaseWord".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'isLowerCase' requires 1 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.isLowerCaseWord(args[0].asString().value));
		}
		else if ("isFirstLetterUpWord".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'isFirstLetterUpWord' requires 1 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.isFirstLetterUpWord(args[0].asString().value));
		}
		else if ("isCapitalizedeWord".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'isCapitalizedWord' requires 1 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.isCapitalizedWord(args[0].asString().value));
		}
		else if ("isUpperCaseWord".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'isUpperCaseWord' requires 1 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.isUpperCaseWord(args[0].asString().value));
		}
		else if ("isNumber".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'isNumber' requires 1 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.isNumber(args[0].asString().value));
		}
		else if ("isPunctuation".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'isPunctuation' requires 1 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.isPunctuation(args[0].asString().value));
		}
		else if ("isBracket".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'isBracket' requires 1 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.isBracket(args[0].asString().value));
		}
		else if ("isOpeningBracket".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'isOpeningBracket' requires 1 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.isOpeningBracket(args[0].asString().value));
		}
		else if ("isClosingBracket".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'isClosingBracket' requires 1 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.isClosingBracket(args[0].asString().value));
		}
		else if ("closes".equalsIgnoreCase(functionName)) {
			if (args.length != 2)
				throw new InvalidArgumentsException("The function 'closes' requires 2 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.closes(args[0].asString().value, args[1].asString().value));
		}
		else if ("opens".equalsIgnoreCase(functionName)) {
			if (args.length != 2)
				throw new InvalidArgumentsException("The function 'closes' requires 2 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.opens(args[0].asString().value, args[1].asString().value));
		}
		else if ("isSentencePunctuation".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'isSentencePunctuation' requires 1 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.isSentencePunctuation(args[0].asString().value));
		}
		else if ("isSentenceEnd".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'isSentenceEnd' requires 1 argument(s) of type(s) GPathString.");
			return new GPathBoolean(Gamta.isSentenceEnd(args[0].asString().value));
		}
		else if ("lang".equalsIgnoreCase(functionName)) {
			if ((args.length != 1) || !(args[0] instanceof GPathString))
				throw new InvalidArgumentsException("The function 'lang' requires 1 argument(s) of type(s) GPathString.");
			return new GPathBoolean(true);
		}
		
		//	number functions
		else if ("number".equalsIgnoreCase(functionName)) {
			if (args.length == 0) {
				GPathAnnotationSet annotationSet = new GPathAnnotationSet();
				annotationSet.add(contextAnnotation);
				return annotationSet.asNumber();
			}
			else if (args.length == 1)
				return args[0].asNumber();
			else throw new InvalidArgumentsException("The function 'number' requires 0 argument(s) or 1 argument(s) of type(s) GPathObject.");
		}
		else if ("floor".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'floor' requires 1 argument(s) of type(s) GPathNumber.");
			return new GPathNumber(Math.floor(args[0].asNumber().value));
		}
		else if ("ceiling".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'ceiling' requires 1 argument(s) of type(s) GPathNumber.");
			return new GPathNumber(Math.ceil(args[0].asNumber().value));
		}
		else if ("round".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'round' requires 1 argument(s) of type(s) GPathNumber.");
			return new GPathNumber(Math.round(args[0].asNumber().value));
		}
		else if ("abs".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'abs' requires 1 argument(s) of type(s) GPathNumber.");
			return new GPathNumber(Math.abs(args[0].asNumber().value));
		}
		
		//	aggregation functions
		else if ("count".equalsIgnoreCase(functionName)) {
			if ((args.length == 1) && (args[0] instanceof GPathAnnotationSet))
				return new GPathNumber(((GPathAnnotationSet) args[0]).size());
			throw new InvalidArgumentsException("The function 'count' requires 1 argument(s) of type(s) GPathAnnotationSet.");
		}
		else if ("sum".equalsIgnoreCase(functionName)) {
			if ((args.length != 1) || !(args[0] instanceof GPathAnnotationSet))
				throw new InvalidArgumentsException("The function 'sum' requires 1 argument(s) of type(s) GPathAnnotationSet.");
			
			GPathAnnotationSet annotationSet = ((GPathAnnotationSet) args[0]);
			double sum = 0;
			for (int s = 0; s < annotationSet.size(); s++)
				sum += new GPathString(annotationSet.get(s).getValue()).asNumber().value;
			return new GPathNumber(sum);
		}
		
		//	positional functions
		else if ("last".equalsIgnoreCase(functionName)) {
			if (args.length != 0)
				throw new InvalidArgumentsException("The function 'last' requires 0 argument(s).");
			return new GPathNumber(contextSize);
		}
		else if ("position".equalsIgnoreCase(functionName)) {
			if (args.length != 0)
				throw new InvalidArgumentsException("The function 'position' requires 0 argument(s).");
			return new GPathNumber(contextPosition);
		}
		
		//	annotation type functions
		else if ("local-name".equalsIgnoreCase(functionName)) {
			if (args.length == 0)
				return new GPathString(contextAnnotation.getType());
			else if ((args.length == 1) && (args[0] instanceof GPathAnnotationSet)) {
				GPathAnnotationSet gpas = ((GPathAnnotationSet) args[0]);
				return new GPathString(gpas.isEmpty() ? "" : gpas.getFirst().getType());
			}
			throw new InvalidArgumentsException("The function 'local-name' requires 0 argument(s) or 1 argument(s) of type(s) GPathAnnotationSet.");
		}
		else if ("name".equalsIgnoreCase(functionName)) {
			if (args.length == 0)
				return new GPathString(contextAnnotation.getType());
			else if ((args.length == 1) && (args[0] instanceof GPathAnnotationSet)) {
				GPathAnnotationSet gpas = ((GPathAnnotationSet) args[0]);
				return new GPathString(gpas.isEmpty() ? "" : gpas.getFirst().getType());
			}
			else throw new InvalidArgumentsException("The function 'name' requires 0 argument(s) or 1 argument(s) of type(s) GPathAnnotationSet.");
		}
		else if ("namespace-uri".equalsIgnoreCase(functionName)) {
			if (args.length == 0)
				return new GPathString("This implementation uses generic namespaces.");
			else if (args[0] instanceof GPathAnnotationSet)
				return new GPathString("This implementation uses generic namespaces.");
			else throw new InvalidArgumentsException("The function 'namespace-uri' requires 0 argument(s) or 1 argument(s) of type(s) GPathAnnotationSet.");
		}
		
		//	attribute function with default
		else if ("getAttribute".equalsIgnoreCase(functionName)) {
			if ((args.length < 1) || (args.length > 2))
				throw new InvalidArgumentsException("The function 'getAttribute' requires 1 or 2 argument(s) of type(s) GPathString.");
			Object attributeValue = contextAnnotation.getAttribute(args[0].asString().value);
			if (attributeValue == null)
				return new GPathString((args.length == 1) ? "" : args[1].asString().value);
			else return new GPathString(attributeValue.toString());
		}
		
		//	string functions
		else if ("string".equalsIgnoreCase(functionName)) {
			if (args.length == 0) {
				GPathAnnotationSet annotationSet = new GPathAnnotationSet();
				annotationSet.add(contextAnnotation);
				return annotationSet.asString();
			}
			else if (args.length == 1)
				return args[0].asString();
			else throw new InvalidArgumentsException("The function 'string' requires 0 argument(s) or 1 argument(s) of type(s) GPathObject.");
		}
		else if ("string-length".equalsIgnoreCase(functionName)) {
			if (args.length == 0) {
				GPathAnnotationSet annotationSet = new GPathAnnotationSet();
				annotationSet.add(contextAnnotation);
				return new GPathNumber(annotationSet.asString().value.length());
			}
			else if (args.length == 1)
				return new GPathNumber(args[0].asString().value.length());
			else throw new InvalidArgumentsException("The function 'string-length' requires 0 argument(s) or 1 argument(s) of type(s) GPathString.");
		}
		else if ("contains".equalsIgnoreCase(functionName)) {
			if (args.length != 2)
				throw new InvalidArgumentsException("The function 'contains' requires 2 argument(s) of type(s) GPathString.");
			return new GPathBoolean(args[0].asString().value.indexOf(args[1].asString().value) != -1);
		}
		else if ("starts-with".equalsIgnoreCase(functionName)) {
			if (args.length != 2)
				throw new InvalidArgumentsException("The function 'starts-with' requires 2 argument(s) of type(s) GPathString.");
			return new GPathBoolean(args[0].asString().value.startsWith(args[1].asString().value));
		}
		else if ("ends-with".equalsIgnoreCase(functionName)) {
			if (args.length != 2)
				throw new InvalidArgumentsException("The function 'ends-with' requires 2 argument(s) of type(s) GPathString.");
			return new GPathBoolean(args[0].asString().value.endsWith(args[1].asString().value));
		}
		else if ("matches".equalsIgnoreCase(functionName)) {
			if (args.length != 2) throw new InvalidArgumentsException("The function 'matches' requires 2 argument(s) of type(s) GPathString.");
			try {
				return new GPathBoolean(args[0].asString().value.matches(args[1].asString().value));
			}
			catch (PatternSyntaxException pse) {
				throw new InvalidArgumentsException("The function 'matches' requires a GPathString representing a valid pattern as it's second argument.");
			}
		}
		else if ("concat".equalsIgnoreCase(functionName)) {
			if (args.length < 2)
				throw new InvalidArgumentsException("The function 'concat' requires 2 or more argument(s) of type(s) GPathString.");
			
			StringBuffer assembler = new StringBuffer("");
			for (int a = 0; a < args.length; a++)
				assembler.append(((a == 0) ? "" : " ") + args[a].asString().value);
			return new GPathString(assembler.toString());
		}
		else if ("normalize-space".equalsIgnoreCase(functionName)) {
			String toNormalize;
			if (args.length == 0) {
				GPathAnnotationSet annotationSet = new GPathAnnotationSet();
				annotationSet.add(contextAnnotation);
				toNormalize = annotationSet.asString().value;
			}
			else if (args.length == 1)
				toNormalize = args[0].asString().value;
			else throw new InvalidArgumentsException("The function 'normalize-space' requires 0 argument(s) or 1 argument(s) of type(s) GPathString.");
			
			boolean lastWasWhitespace = false;
			StringBuffer assembler = new StringBuffer();
			for (int c = 0; c < toNormalize.length(); c++) {
				char ch = toNormalize.charAt(c);
				if (ch > 32) {
					assembler.append(ch);
					lastWasWhitespace = false;
				}
				else if (!lastWasWhitespace) {
					assembler.append(' ');
					lastWasWhitespace = true;
				}
			}
			
			return new GPathString(assembler.toString().trim());
		}
		else if ("normalize-chars".equalsIgnoreCase(functionName)) {
			String toNormalize;
			if (args.length == 0) {
				GPathAnnotationSet annotationSet = new GPathAnnotationSet();
				annotationSet.add(contextAnnotation);
				toNormalize = annotationSet.asString().value;
			}
			else if (args.length == 1)
				toNormalize = args[0].asString().value;
			else throw new InvalidArgumentsException("The function 'normalize-chars' requires 0 argument(s) or 1 argument(s) of type(s) GPathString.");
			
			boolean lastWasWhitespace = false;
			StringBuffer assembler = new StringBuffer();
			for (int s = 0; s < toNormalize.length(); s++) {
				char ch = toNormalize.charAt(s);
				if (ch > 32) {
					assembler.append(StringUtils.getNormalForm(ch));
					lastWasWhitespace = false;
				}
				else if (!lastWasWhitespace) {
					assembler.append(' ');
					lastWasWhitespace = true;
				}
			}
			
			return new GPathString(assembler.toString().trim());
		}
		else if ("upper-case".equals(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'upper-case' requires 1 argument(s) of type(s) GPathString.");
			return new GPathString(args[0].asString().value.toUpperCase());
		}
		else if ("lower-case".equals(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'lower-case' requires 1 argument(s) of type(s) GPathString.");
			return new GPathString(args[0].asString().value.toLowerCase());
		}
		else if ("substring".equalsIgnoreCase(functionName)) {
			if ((args.length != 2) && (args.length != 3))
				throw new InvalidArgumentsException("The function 'substring' requires 2 or 3 argument(s) of type(s) GPathString, GPathNumber" + ((args.length > 3) ? ", GPathNumber" : "") + ".");
			if (!(args[1] instanceof GPathNumber))
				throw new InvalidArgumentsException("The function 'substring' requires 2 or 3 argument(s) of type(s) GPathString, GPathNumber" + ((args.length == 3) ? ", GPathNumber" : "") + ".");
			if ((args.length == 3) && !(args[2] instanceof GPathNumber))
				throw new InvalidArgumentsException("The function 'substring' requires 2 or 3 argument(s) of type(s) GPathString, GPathNumber" + ((args.length == 3) ? ", GPathNumber" : "") + ".");
			
			int s = 0;
			int l = args[0].asString().value.length();
			try {
				s = ((int) args[1].asNumber().value);
				if (s < 0) s = 0;
				if (s > l) return new GPathString("");
			} catch (Exception e) {}
			if (args.length == 3) try {
				l = ((int) args[2].asNumber().value);
				if (l < 1) return new GPathString("");
			} catch (Exception e) {}
			if ((s + l) > args[0].asString().value.length()) l = args[0].asString().value.length() - s;
			return new GPathString(args[0].asString().value.substring(s, (s + l)));
		}
		else if ("substring-after".equalsIgnoreCase(functionName)) {
			if (args.length != 2)
				throw new InvalidArgumentsException("The function 'substring-after' requires 2 argument(s) of type(s) GPathString.");
			
			int s = args[0].asString().value.indexOf(args[1].asString().value);
			if (s == -1)
				return new GPathString("");
			s += args[1].asString().value.length();
			if (s >= args[0].asString().value.length())
				return new GPathString("");
			return new GPathString(args[0].asString().value.substring(s));
		}
		else if ("substring-before".equalsIgnoreCase(functionName)) {
			if (args.length != 2)
				throw new InvalidArgumentsException("The function 'substring-before' requires 2 argument(s) of type(s) GPathString.");
			
			int l = args[0].asString().value.indexOf(args[1].asString().value);
			if (l == -1)
				return new GPathString("");
			return new GPathString(args[0].asString().value.substring(0, l));
		}
		else if ("replace".equalsIgnoreCase(functionName)) {
			if (args.length != 3)
				throw new InvalidArgumentsException("The function 'replace' requires 3 argument(s) of type(s) GPathString.");
			String string = args[0].asString().value;
			String toReplace = args[1].asString().value;
			int index = string.indexOf(toReplace);
			if (index == -1)
				return new GPathString(string);
			else return new GPathString(string.substring(0, index) + args[2].asString().value + string.substring(index + toReplace.length()));
		}
		else if ("replace-all".equalsIgnoreCase(functionName)) {
			if (args.length != 3)
				throw new InvalidArgumentsException("The function 'replace' requires 3 argument(s) of type(s) GPathString.");
			String string = args[0].asString().value;
			String toReplace = args[1].asString().value;
			String replacement = args[2].asString().value;
			int index = string.indexOf(toReplace);
			while (index != -1) {
				string = (string.substring(0, index) + replacement + string.substring(index + toReplace.length()));
				index = string.indexOf(toReplace, (index + replacement.length()));
			}
			return new GPathString(string);
		}
		else if ("translate".equalsIgnoreCase(functionName)) {
			if (args.length != 3)
				throw new InvalidArgumentsException("The function 'translate' requires 3 argument(s) of type(s) GPathString.");
			
			String originals = args[1].asString().value;
			String translations = args[2].asString().value;
			Properties dictionary = new Properties();
			for (int i = 0; i < originals.length(); i++) {
				String o = originals.substring(i, (i + 1));
				String t = ((i < translations.length()) ? translations.substring(i, (i + 1)) : "");
				dictionary.setProperty(o, t);
			}
			
			String string = args[0].asString().value;
			StringBuffer assembler = new StringBuffer();
			for (int s = 0; s < string.length(); s++) {
				String o = string.substring(s, (s + 1));
				assembler.append(dictionary.getProperty(o, o));
			}
			
			return new GPathString(assembler.toString());
		}
		
		//	date and time functions
		else if ("date".equalsIgnoreCase(functionName)) {
			if (args.length > 1)
				throw new InvalidArgumentsException("The function 'date' requires no or 1 argument(s) of type GPathNumber.");
			return new GPathString(dateDateFormat.format(new Date((args.length == 0) ? System.currentTimeMillis() : Long.parseLong(args[0].asString().value))));
		}
		else if ("dateUTC".equalsIgnoreCase(functionName)) {
			if (args.length > 1)
				throw new InvalidArgumentsException("The function 'dateUTC' requires no or 1 argument(s) of type GPathNumber.");
			return new GPathString(dateDateFormatUTC.format(new Date((args.length == 0) ? System.currentTimeMillis() : Long.parseLong(args[0].asString().value))));
		}
		else if ("time".equalsIgnoreCase(functionName)) {
			if (args.length > 1)
				throw new InvalidArgumentsException("The function 'time' requires no or 1 argument(s) of type GPathNumber.");
			return new GPathString(timeDateFormat.format(new Date((args.length == 0) ? System.currentTimeMillis() : Long.parseLong(args[0].asString().value))));
		}
		else if ("timeUTC".equalsIgnoreCase(functionName)) {
			if (args.length > 1)
				throw new InvalidArgumentsException("The function 'timeUTC' requires no or 1 argument(s) of type GPathNumber.");
			return new GPathString(timeDateFormatUTC.format(new Date((args.length == 0) ? System.currentTimeMillis() : Long.parseLong(args[0].asString().value))));
		}
		else if ("dateTime".equalsIgnoreCase(functionName)) {
			if (args.length > 1)
				throw new InvalidArgumentsException("The function 'dateTime' requires no or 1 argument(s) of type GPathNumber.");
			return new GPathString(dateTimeDateFormat.format(new Date((args.length == 0) ? System.currentTimeMillis() : Long.parseLong(args[0].asString().value))));
		}
		else if ("dateTimeUTC".equalsIgnoreCase(functionName)) {
			if (args.length > 1)
				throw new InvalidArgumentsException("The function 'dateTimeUTC' requires no or 1 argument(s) of type GPathNumber.");
			return new GPathString(dateTimeDateFormatUTC.format(new Date((args.length == 0) ? System.currentTimeMillis() : Long.parseLong(args[0].asString().value))));
		}
		
		//	introspective functions
		else if ("isFunctionAvailable".equalsIgnoreCase(functionName)) {
			if (args.length != 1)
				throw new InvalidArgumentsException("The function 'isFunctionAvailable' requires 1 argument of type GPathString.");
			if (this.customFunctions.containsKey(args[0].asString().value))
				return new GPathBoolean(true);
			else if (!this.isDefaultEngine && GPath.DEFAULT_ENGINE.customFunctions.containsKey(args[0].asString().value))
				return new GPathBoolean(true);
			else return new GPathBoolean(false);
		}
		
		if (this.isDefaultEngine) throw new UndefinedFunctionException("The function '" + functionName + "' is not defined.");
		
		return GPath.DEFAULT_ENGINE.executeFunction(functionName, contextAnnotation, contextPosition, contextSize, args);
	}
	
	private static final DateFormat dateDateFormat;
	private static final DateFormat dateDateFormatUTC;
	private static final DateFormat timeDateFormat;
	private static final DateFormat timeDateFormatUTC;
	private static final DateFormat dateTimeDateFormat;
	private static final DateFormat dateTimeDateFormatUTC;
	static {
		dateDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateDateFormatUTC = new SimpleDateFormat("yyyy-MM-dd");
		dateDateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
		timeDateFormat = new SimpleDateFormat("HH:mm:ss");
		timeDateFormatUTC = new SimpleDateFormat("HH:mm:ss");
		timeDateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
		dateTimeDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateTimeDateFormatUTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateTimeDateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	//	register for custom functions
	private final TreeMap customFunctions = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	
	/**	add a custom function to the function library of this GPathEngine (will overwrite the original function if it has the same name, the same is true for equally named custom functions in the default function library)
	 * @param	functionName	the name the function will be invokable with in GPath queries
	 * @param	function		the GPathFunction to be added
	 */
	public void addFunction(String functionName, GPathFunction function) {
		if ((functionName != null) && (function != null))
			this.customFunctions.put(functionName, function);
	}
	
	/**	remove a custom function from the function library of this GPathEngine
	 * @param	functionName	the name of the function to be removed
	 * @return the function that was just removed, or null if there was no function with the specified name
	 */
	public GPathFunction removeFunction(String functionName) {
		return ((GPathFunction) this.customFunctions.remove(functionName));
	}
	
//	//	!!! TEST ONLY !!!
//	public static void main(String[] args) throws Exception {
//		MutableAnnotation doc = Gamta.newDocument(Gamta.newTokenSequence("A B C D E F G", null));
//		doc.addAnnotation("a", 0, 3);
//		doc.addAnnotation("b", 1, 3);
//		doc.addAnnotation("a", 2, 3);
//		doc.addAnnotation("a", 0, 5);
//		doc.addAnnotation("b", 0, 6);
//		Annotation[] res;
//		res = GPath.evaluatePath(doc, "a/interleaving-sibling::b", null);
//		for (int r = 0; r < res.length; r++)
//			System.out.println("1 - " + res[r].toXML());
//		res = GPath.evaluatePath(doc, "a/interleaving-sibling::a", null);
//		for (int r = 0; r < res.length; r++)
//			System.out.println("2 - " + res[r].toXML());
//		res = GPath.evaluatePath(doc, "a/interleaving-left::a", null);
//		for (int r = 0; r < res.length; r++)
//			System.out.println("2a - " + res[r].toXML());
//		res = GPath.evaluatePath(doc, "a/interleaving-right::a", null);
//		for (int r = 0; r < res.length; r++)
//			System.out.println("2b - " + res[r].toXML());
//		res = GPath.evaluatePath(doc, "b/interleaving-sibling::b", null);
//		for (int r = 0; r < res.length; r++)
//			System.out.println("3 - " + res[r].toXML());
//		res = GPath.evaluatePath(doc, "b/interleaving-sibling::a", null);
//		for (int r = 0; r < res.length; r++)
//			System.out.println("4 - " + res[r].toXML());
//	}
//	
	private static abstract class GPathAnnotation implements QueriableAnnotation {
		
		protected QueriableAnnotation source;
		
		/**	Constructor
		 * @param	source			the Tokens of this Document's text
		 * @param	docOrderPos		the position in document order
		 */
		GPathAnnotation(QueriableAnnotation source) {
			this.source = source;
		}
		
		/**	@return	the root QueriableAnnotation this QueriableAnnotation belongs to
		 */
		abstract GPathAnnotation getParent();
		
		/**	prepare for being garbage collected
		 */
		abstract void cleanup();
		
		/** @see java.lang.Object#finalize()
		 */
		protected void finalize() throws Throwable {
			super.finalize();
			this.cleanup();
			this.source = null;
		}
		
		/** @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return this.getAnnotationID().hashCode();
		}

		public Token firstToken() {
			return this.source.firstToken();
		}

		public String firstValue() {
			return this.source.firstValue();
		}

		public String getLeadingWhitespace() {
			return this.source.getLeadingWhitespace();
		}

		public TokenSequence getSubsequence(int start, int size) {
			return this.source.getSubsequence(start, size);
		}

		public Tokenizer getTokenizer() {
			return this.source.getTokenizer();
		}

		public String getWhitespaceAfter(int index) {
			return this.source.getWhitespaceAfter(index);
		}

		public Token lastToken() {
			return this.source.lastToken();
		}

		public String lastValue() {
			return this.source.lastValue();
		}

		public int size() {
			return this.source.size();
		}

		public Token tokenAt(int index) {
			return this.source.tokenAt(index);
		}

		public String valueAt(int index) {
			return this.source.valueAt(index);
		}

		public void clearAttributes() {
			this.source.clearAttributes();
		}

		public void copyAttributes(Attributed source) {
			this.source.copyAttributes(source);
		}

		public String[] getAttributeNames() {
			return this.source.getAttributeNames();
		}

		public Object removeAttribute(String name) {
			return this.source.removeAttribute(name);
		}

		public void setAttribute(String name) {
			this.source.setAttribute(name);
		}

		public Object setAttribute(String name, Object value) {
			return this.source.setAttribute(name, value);
		}

		public char charAt(int index) {
			return this.source.charAt(index);
		}

		public int length() {
			return this.source.length();
		}

		public CharSequence subSequence(int start, int end) {
			return this.source.subSequence(start, end);
		}

		public String[] getAnnotationTypes() {
			return this.source.getAnnotationTypes();
		}

		public String changeTypeTo(String newType) {
			return this.source.changeTypeTo(newType);
		}

		public String getAnnotationID() {
			return this.source.getAnnotationID();
		}

		public String getDocumentProperty(String propertyName) {
			return this.source.getDocumentProperty(propertyName);
		}

		public String getDocumentProperty(String propertyName, String defaultValue) {
			return this.source.getDocumentProperty(propertyName, defaultValue);
		}

		public String[] getDocumentPropertyNames() {
			return this.source.getDocumentPropertyNames();
		}

		public String getAnnotationNestingOrder() {
			return this.source.getAnnotationNestingOrder();
		}

		public int getEndIndex() {
			return this.source.getEndIndex();
		}

		public int getStartIndex() {
			return this.source.getStartIndex();
		}

		public String getType() {
			return this.source.getType();
		}

		public String getValue() {
			return this.source.getValue();
		}

		public String toXML() {
			return this.source.toXML();
		}

		public int getEndOffset() {
			return this.source.getEndOffset();
		}

		public int getStartOffset() {
			return this.source.getStartOffset();
		}
	}
	
	private static class GPathDocument extends GPathAnnotation {
		
		void cachePathResult(String path, GPathAnnotationSet pathResult) {
			this.pathResultCache.put(path, pathResult);
		}
		
		GPathAnnotationSet getPathResult(String path) {
			return ((GPathAnnotationSet) this.pathResultCache.get(path));
		}
		
		private HashMap pathResultCache = new HashMap();
		
		/**	Constructor
		 * @param	source	the Tokens of this Document's text
		 */
		GPathDocument(QueriableAnnotation source) {
			super(source);
		}
		
		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#cleanup()
		 */
		void cleanup() {}
		
		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#getParent()
		 */
		GPathAnnotation getParent() {
			return this;
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAnnotations()
		 */
		public QueriableAnnotation[] getAnnotations() {
			return this.getAnnotations(null);
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAnnotations(java.lang.String)
		 */
		public QueriableAnnotation[] getAnnotations(String type) {
			QueriableAnnotation[] annotations = this.source.getAnnotations(type);
			
			//	add Document itself if no other root Anotation given
			if ((type == null) && ((annotations.length == 0) || (annotations[0].size() != this.size()))) {
				ArrayList aList = new ArrayList();
				aList.add(this);
				for (int p = 0; p < annotations.length; p++) aList.add(annotations[p]);
				annotations = ((QueriableAnnotation[]) aList.toArray(new QueriableAnnotation[aList.size()]));
			}
			
			for (int a = 0; a < annotations.length; a++)
				annotations[a] = new GPathAnnotationView(annotations[a], this, this);
			return annotations;
		}
		
		/** @see de.uka.ipd.idaho.gamta.QueriableAnnotation#getAbsoluteStartOffset()
		 */
		public int getAbsoluteStartOffset() {
			return 0;
		}

		/** @see de.uka.ipd.idaho.gamta.Annotation#getStartIndex()
		 */
		public int getStartIndex() {
			return 0; // this needs to return 0 so the document has a root, no matter to what hierarchy the source belongs
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAbsoluteStartIndex()
		 */
		public int getAbsoluteStartIndex() {
			return 0;
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#size()
		 */
		public int size() {
			return this.source.size();
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getEndIndex()
		 */
		public int getEndIndex() {
			return this.size();
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAttribute(java.lang.String)
		 */
		public Object getAttribute(String name) {
			return this.getAttribute(name, null);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAttribute(java.lang.String, java.lang.String)
		 */
		public Object getAttribute(String name, Object def) {
			if (Annotation.START_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getStartIndex());
			else if (GPath.ABSOLUTE_START_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getAbsoluteStartIndex());
			else if (Annotation.SIZE_ATTRIBUTE.equals(name)) return ("" + this.size());
			else if (Annotation.END_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getEndIndex());
			else if (Annotation.ANNOTATION_VALUE_ATTRIBUTE.equals(name)) return this.getValue();
			else if (Annotation.ANNOTATION_ID_ATTRIBUTE.equals(name)) return this.getAnnotationID();
			else return this.source.getAttribute(name, def);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#hasAttribute(java.lang.String)
		 */
		public boolean hasAttribute(String name) {
			return (this.source.hasAttribute(name) 
					|| Annotation.START_INDEX_ATTRIBUTE.equals(name) 
					|| GPath.ABSOLUTE_START_INDEX_ATTRIBUTE.equals(name) 
					|| Annotation.SIZE_ATTRIBUTE.equals(name) 
					|| Annotation.END_INDEX_ATTRIBUTE.equals(name)
					|| Annotation.ANNOTATION_VALUE_ATTRIBUTE.equals(name)
					|| Annotation.ANNOTATION_ID_ATTRIBUTE.equals(name)
					);
		}
		
		/** @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if (o == null) return -1;
			else if (o == this) return 0;
			else if (o instanceof GPathAnnotation) {
				GPathAnnotation qa = ((GPathAnnotation) o);
				if (qa.getAbsoluteStartIndex() != 0) return -1;
				if (qa.size() < this.size()) return -1;
				if (!this.getType().equals(qa.getType())) return -1;
				return 0;
			}
			//	the document is always the outmost tag in the XML representation and thus has to be first in any sequence
			else return -1;
		}
		
		/** @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object o) {
			return (this.compareTo(o) == 0);
		}
	}
	
	/**	private implementation of the Annotation interface
	 */
	private static class GPathAnnotationView extends GPathAnnotation {
		
		private GPathAnnotation parent;
		private GPathDocument doc;
		
		/**	Constructor
		 * @param	data	the Annotation this Annotation is a view for
		 * @param	parent	the Annotation this Annotation was retrieved from
		 */
		GPathAnnotationView(QueriableAnnotation data, GPathAnnotation parent, GPathDocument doc) {
			super(data);
			this.parent = parent;
			this.doc = doc;
		}
		
		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#cleanup()
		 */
		void cleanup() {
			this.source = null;
			this.parent = null;
			this.doc = null;
		}
		
		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#getParent()
		 */
		GPathAnnotation getParent() {
			return this.parent;
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getStartIndex()
		 */
		public int getAbsoluteStartIndex() {
			return this.source.getAbsoluteStartIndex() - ((this.doc == null) ? 0 : this.doc.source.getAbsoluteStartIndex());
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getEndIndex()
		 */
		public int getEndIndex() {
			return (this.getStartIndex() + this.size());
		}
		
		/** @see de.uka.ipd.idaho.gamta.QueriableAnnotation#getAbsoluteStartOffset()
		 */
		public int getAbsoluteStartOffset() {
			return this.source.getAbsoluteStartOffset() - ((this.doc == null) ? 0 : this.doc.source.getAbsoluteStartOffset());
		}

		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAnnotations()
		 */
		public QueriableAnnotation[] getAnnotations() {
			return this.getAnnotations(null);
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAnnotations(java.lang.String)
		 */
		public QueriableAnnotation[] getAnnotations(String type) {
			QueriableAnnotation[] annotations = this.source.getAnnotations(type);
			for (int a = 0; a < annotations.length; a++)
				annotations[a] = new GPathAnnotationView(annotations[a], this, this.doc);
			return annotations;
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAttribute(java.lang.String)
		 */
		public Object getAttribute(String name) {
			return this.getAttribute(name, null);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAttribute(java.lang.String, java.lang.String)
		 */
		public Object getAttribute(String name, Object def) {
			if (Annotation.START_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getStartIndex());
			else if (Annotation.SIZE_ATTRIBUTE.equals(name)) return ("" + this.size());
			else if (Annotation.END_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getEndIndex());
			else if (Annotation.ANNOTATION_VALUE_ATTRIBUTE.equals(name)) return this.getValue();
			else if (Annotation.ANNOTATION_ID_ATTRIBUTE.equals(name)) return this.getAnnotationID();
			else return this.source.getAttribute(name, def);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#hasAttribute(java.lang.String)
		 */
		public boolean hasAttribute(String name) {
			return this.source.hasAttribute(name);
		}
		
		/** @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if (o == null) return -1;
			if (o instanceof GPathAttributeAnnotation) return -1;
			return this.source.compareTo(o);
		}
		
		/** @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object o) {
			return (this.compareTo(o) == 0);
		}
	}
	
	private static class GPathAttributeAnnotation extends GPathAnnotation {
		
		private String type;
		private TokenSequence value;
		
		/**	Constructor
		 * @param	source		the Annotation this Annotation is an attribute of
		 * @param	type		the name of the Annotation attribute
		 * @param	value		the attribute value
		 */
		GPathAttributeAnnotation(QueriableAnnotation source, String type, TokenSequence value) {
			super(source);
			this.type = type;
			this.value = value;
		}
		
		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#cleanup()
		 */
		void cleanup() {}
		
		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#getParent()
		 */
		GPathAnnotation getParent() {
			return ((GPathAnnotation) this.source);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getStartIndex()
		 */
		public int getStartIndex() {
			return 0;
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAbsoluteStartIndex()
		 */
		public int getAbsoluteStartIndex() {
			return 0;
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#size()
		 */
		public int size() {
			return this.value.size();
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getEndIndex()
		 */
		public int getEndIndex() {
			return this.value.size();
		}
		
		/** @see de.uka.ipd.idaho.gamta.CharSpan#getStartOffset()
		 */
		public int getStartOffset() {
			return 0;
		}

		 /** @see de.uka.ipd.idaho.gamta.QueriableAnnotation#getAbsoluteStartOffset()
		 */
		public int getAbsoluteStartOffset() {
			return 0;
		}

		/** @see de.uka.ipd.idaho.gamta.CharSpan#getEndOffset()
		 */
		public int getEndOffset() {
			return this.value.length();
		}

		/** @see java.lang.CharSequence#charAt(int)
		 */
		public char charAt(int index) {
			return this.value.charAt(index);
		}

		/** @see java.lang.CharSequence#subSequence(int, int)
		 */
		public CharSequence subSequence(int start, int end) {
			return this.value.subSequence(start, end);
		}

		/** @see de.uka.ipd.idaho.gamta.Annotation#getType()
		 */
		public String getType() {
			return this.type;
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#changeTypeTo(java.lang.String)
		 */
		public String changeTypeTo(String newType) {
			return this.type;
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAnnotationID()
		 */
		public String getAnnotationID() {
			return (this.source.getAnnotationID() + "." + this.type);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getValue()
		 */
		public String getValue() {
			return this.value.toString();
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#toXML()
		 */
		public String toXML() {
			String type = (this.source.getType() + "." + this.type);
			return ("<" + type + ">" + AnnotationUtils.escapeForXml(this.getValue()) + "</" + type + ">");
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAnnotations()
		 */
		public QueriableAnnotation[] getAnnotations() {
			return new QueriableAnnotation[0];
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAnnotations(java.lang.String)
		 */
		public QueriableAnnotation[] getAnnotations(String type) {
			return new QueriableAnnotation[0];
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAnnotationTypes()
		 */
		public String[] getAnnotationTypes() {
			return new String[0];
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAttribute(java.lang.String)
		 */
		public Object getAttribute(String name) {
			return null;
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAttribute(java.lang.String, java.lang.String)
		 */
		public Object getAttribute(String name, Object def) {
			return def;
		}
			
		/** @see de.uka.ipd.idaho.gamta.Annotation#hasAttribute(java.lang.String)
		 */
		public boolean hasAttribute(String name) {
			return false;
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAttributeNames()
		 */
		public String[] getAttributeNames() {
			return new String[0];
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#tokenAt(int)
		 */
		public Token tokenAt(int index) {
			return this.value.tokenAt(index);
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#firstToken()
		 */
		public Token firstToken() {
			return this.value.firstToken();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#lastToken()
		 */
		public Token lastToken() {
			return this.value.lastToken();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#valueAt(int)
		 */
		public String valueAt(int index) {
			return this.value.valueAt(index);
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#getWhitespaceAfter(int)
		 */
		public String getWhitespaceAfter(int index) {
			return this.value.getWhitespaceAfter(index);
		}

		/** @see de.uka.ipd.idaho.gamta.TokenSequence#firstValue()
		 */
		public String firstValue() {
			return this.value.firstValue();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#lastValue()
		 */
		public String lastValue() {
			return this.value.lastValue();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#getLeadingWhitespace()
		 */
		public String getLeadingWhitespace() {
			return this.value.getLeadingWhitespace();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#length()
		 */
		public int length() {
			return this.value.length();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#getTokenizer()
		 */
		public Tokenizer getTokenizer() {
			return this.value.getTokenizer();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#getSubsequence(int, int)
		 */
		public TokenSequence getSubsequence(int start, int size) {
			return this.value.getSubsequence(start, size);
		}
		
		/** @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if (o == null) return -1;
			if (o instanceof GPathAttributeAnnotation) {
				GPathAttributeAnnotation aa = ((GPathAttributeAnnotation) o);
				int c = aa.source.compareTo(this.source);
				if (c != 0) c = this.type.compareTo(aa.type);
				return c;
			} else return 1;
		}
		
		/** @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object o) {
			return (this.compareTo(o) == 0);
		}
	}
	
	private static class GPathTokenAnnotation extends GPathAnnotation {
		
		private int index;
		private Token token;
		
		/**	Constructor
		 * @param	source		the Annotation this Annotation is a Token of
		 * @param	index		the index of this Token in the source Annotation
		 */
		GPathTokenAnnotation(QueriableAnnotation source, int index) {
			super(source);
			this.index = index;
			this.token = source.tokenAt(this.index);
		}
		
		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#cleanup()
		 */
		void cleanup() {}
		
		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#getParent()
		 */
		GPathAnnotation getParent() {
			return ((GPathAnnotation) this.source);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getStartIndex()
		 */
		public int getStartIndex() {
			return this.index;
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAbsoluteStartIndex()
		 */
		public int getAbsoluteStartIndex() {
			return (this.source.getAbsoluteStartIndex() + this.getStartIndex());
		}
		
		public int getAbsoluteStartOffset() {
			return (this.source.getAbsoluteStartOffset() + this.getStartOffset());
		}

		/** @see de.uka.ipd.idaho.gamta.TokenSequence#size()
		 */
		public int size() {
			return 1;
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getEndIndex()
		 */
		public int getEndIndex() {
			return (this.getStartIndex() + 1);
		}
		
		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#getEndOffset()
		 */
		public int getEndOffset() {
			return (this.getStartOffset() + this.length());
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#getStartOffset()
		 */
		public int getStartOffset() {
			return this.token.getStartOffset();
		}

		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#charAt(int)
		 */
		public char charAt(int index) {
			return this.token.charAt(index);
		}

		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#subSequence(int, int)
		 */
		public CharSequence subSequence(int start, int end) {
			return this.token.subSequence(start, end);
		}

		/** @see de.uka.ipd.idaho.gamta.Annotation#getType()
		 */
		public String getType() {
			return Token.TOKEN_ANNOTATION_TYPE;
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#changeTypeTo(java.lang.String)
		 */
		public String changeTypeTo(String newType) {
			return Token.TOKEN_ANNOTATION_TYPE;
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAnnotationID()
		 */
		public String getAnnotationID() {
			return (Token.TOKEN_ANNOTATION_TYPE + "@" + this.getAbsoluteStartIndex());
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getValue()
		 */
		public String getValue() {
			return this.source.valueAt(this.index);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#toXML()
		 */
		public String toXML() {
			return ("<" + Token.TOKEN_ANNOTATION_TYPE + ">" + AnnotationUtils.escapeForXml(this.getValue()) + "</" + Token.TOKEN_ANNOTATION_TYPE + ">");
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAnnotations()
		 */
		public QueriableAnnotation[] getAnnotations() {
			return new QueriableAnnotation[0];
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAnnotations(java.lang.String)
		 */
		public QueriableAnnotation[] getAnnotations(String type) {
			return new QueriableAnnotation[0];
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAnnotationTypes()
		 */
		public String[] getAnnotationTypes() {
			return new String[0];
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#setAttribute(java.lang.String, java.lang.String)
		 */
		public Object setAttribute(String name, Object value) {
			if (Annotation.START_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getStartIndex());
			else if (GPath.ABSOLUTE_START_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getAbsoluteStartIndex());
			else if (Annotation.SIZE_ATTRIBUTE.equals(name)) return ("" + this.size());
			else if (Annotation.END_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getEndIndex());
			else if (Token.TOKEN_VALUE_ATTRIBUTE.equals(name)) return this.getValue();
			else if (Annotation.ANNOTATION_ID_ATTRIBUTE.equals(name)) return this.getAnnotationID();
			else if (Token.PARAGRAPH_END_ATTRIBUTE.equals(name)) return this.token.setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
			else return this.token.setAttribute(name, value);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAttribute(java.lang.String)
		 */
		public Object getAttribute(String name) {
			return this.getAttribute(name, null);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAttribute(java.lang.String, java.lang.String)
		 */
		public Object getAttribute(String name, Object def) {
			if (Annotation.START_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getStartIndex());
			else if (GPath.ABSOLUTE_START_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getAbsoluteStartIndex());
			else if (Annotation.SIZE_ATTRIBUTE.equals(name)) return ("" + this.size());
			else if (Annotation.END_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getEndIndex());
			else if (Token.TOKEN_VALUE_ATTRIBUTE.equals(name)) return this.getValue();
			else if (Annotation.ANNOTATION_ID_ATTRIBUTE.equals(name)) return this.getAnnotationID();
			else if (Token.PARAGRAPH_END_ATTRIBUTE.equals(name)) return (this.token.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE) ? GPathBoolean.TRUE : def);
			else return this.token.getAttribute(name, def);
		}
			
		/** @see de.uka.ipd.idaho.gamta.Annotation#hasAttribute(java.lang.String)
		 */
		public boolean hasAttribute(String name) {
			return (this.token.hasAttribute(name) 
					|| Annotation.START_INDEX_ATTRIBUTE.equals(name) 
					|| GPath.ABSOLUTE_START_INDEX_ATTRIBUTE.equals(name) 
					|| Annotation.SIZE_ATTRIBUTE.equals(name) 
					|| Annotation.END_INDEX_ATTRIBUTE.equals(name)
					|| Token.TOKEN_VALUE_ATTRIBUTE.equals(name)
					|| Annotation.ANNOTATION_ID_ATTRIBUTE.equals(name)
					|| (Token.PARAGRAPH_END_ATTRIBUTE.equals(name) && this.token.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE))
					);
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#tokenAt(int)
		 */
		public Token tokenAt(int index) {
			if (index == 0) return this.token;
			throw new IndexOutOfBoundsException("" + index);
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#firstToken()
		 */
		public Token firstToken() {
			return this.token;
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#lastToken()
		 */
		public Token lastToken() {
			return this.token;
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#valueAt(int)
		 */
		public String valueAt(int index) {
			if (index == 0) return this.token.getValue();
			throw new IndexOutOfBoundsException("" + index);
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#firstValue()
		 */
		public String firstValue() {
			return this.token.getValue();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#lastValue()
		 */
		public String lastValue() {
			return this.token.getValue();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#getLeadingWhitespace()
		 */
		public String getLeadingWhitespace() {
			return "";
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#length()
		 */
		public int length() {
			return this.token.length();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#getTokenizer()
		 */
		public Tokenizer getTokenizer() {
			return this.token.getTokenizer();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#getSubsequence(int, int)
		 */
		public TokenSequence getSubsequence(int start, int size) {
			return new PlainTokenSequence(this, this.getTokenizer());
		}
		
		/** @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if (o == null) return -1;
			if (o instanceof GPathAttributeAnnotation) return -1;
			if (o instanceof GPathAnnotation) {
				GPathAnnotation qa = ((GPathAnnotation) o);
				int c = (this.getAbsoluteStartIndex() - qa.getAbsoluteStartIndex());
				if (c == 0) c = (qa.size() - this.size());
				if (c == 0) c = (Token.TOKEN_ANNOTATION_TYPE.equals(qa.getType()) ? 0 : 1);
				return c;
			} else return 1;
		}
		
		/** @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object o) {
			return (this.compareTo(o) == 0);
		}
	}
	
	/**
	 * external representation of a token that is the result of a GPath query
	 * 
	 * @author sautter
	 */
	private static class TokenAnnotation implements QueriableAnnotation {
		
		private QueriableAnnotation source;
		private int index;
		
		/**	Constructor
		 * @param	source		the Annotation this Annotation represents a Token of
		 * @param	index		the index of this Token in the source Annotation
		 */
		TokenAnnotation(QueriableAnnotation source, int index) {
			this.index = index;
			this.source = source;
			
			if (source instanceof MutableAnnotation) {
				final MutableAnnotation mSource = ((MutableAnnotation) this.source);
				mSource.addTokenSequenceListener(new TokenSequenceListener() {
					public void tokenSequenceChanged(TokenSequenceEvent change) {
						if (change.index > TokenAnnotation.this.index) return;
						
						else if ((change.index + change.removed.size()) < TokenAnnotation.this.index)
							TokenAnnotation.this.index += (change.inserted.size() - change.removed.size());
						
						else {
							mSource.removeTokenSequenceListener(this);
							TokenAnnotation.this.index = -1;
						}
					}
				});
			}
		}
		
		public int hashCode() {
			return (Token.TOKEN_ANNOTATION_TYPE + "@" + this.getAbsoluteStartIndex()).hashCode();
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getStartIndex()
		 */
		public int getStartIndex() {
			return this.index;
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAbsoluteStartIndex()
		 */
		public int getAbsoluteStartIndex() {
			return (this.source.getAbsoluteStartIndex() + this.getStartIndex());
		}
		
		public int getAbsoluteStartOffset() {
			return (this.source.getAbsoluteStartOffset() + this.getStartOffset());
		}

		/** @see de.uka.ipd.idaho.gamta.TokenSequence#size()
		 */
		public int size() {
			return 1;
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getEndIndex()
		 */
		public int getEndIndex() {
			return (this.getStartIndex() + 1);
		}
		
		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#getEndOffset()
		 */
		public int getEndOffset() {
			return (this.getStartOffset() + this.length());
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#getStartOffset()
		 */
		public int getStartOffset() {
			return this.source.tokenAt(this.index).getStartOffset();
		}

		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#charAt(int)
		 */
		public char charAt(int index) {
			return this.source.tokenAt(this.index).charAt(index);
		}

		/** @see de.uka.ipd.idaho.gamta.util.gPath.GPathEngine.GPathAnnotation#subSequence(int, int)
		 */
		public CharSequence subSequence(int start, int end) {
			return this.source.tokenAt(this.index).subSequence(start, end);
		}

		/** @see de.uka.ipd.idaho.gamta.Annotation#getType()
		 */
		public String getType() {
			return Token.TOKEN_ANNOTATION_TYPE;
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#changeTypeTo(java.lang.String)
		 */
		public String changeTypeTo(String newType) {
			return Token.TOKEN_ANNOTATION_TYPE;
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAnnotationID()
		 */
		public String getAnnotationID() {
			return (Token.TOKEN_ANNOTATION_TYPE + "@" + this.getAbsoluteStartIndex());
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getValue()
		 */
		public String getValue() {
			return this.source.valueAt(this.index);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#toXML()
		 */
		public String toXML() {
			return ("<" + Token.TOKEN_ANNOTATION_TYPE + ">" + AnnotationUtils.escapeForXml(this.getValue()) + "</" + Token.TOKEN_ANNOTATION_TYPE + ">");
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAnnotations()
		 */
		public QueriableAnnotation[] getAnnotations() {
			return new QueriableAnnotation[0];
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAnnotations(java.lang.String)
		 */
		public QueriableAnnotation[] getAnnotations(String type) {
			return new QueriableAnnotation[0];
		}
		
		/** @see de.uka.ipd.idaho.gamta.MutableAnnotation#getAnnotationTypes()
		 */
		public String[] getAnnotationTypes() {
			return new String[0];
		}
		
		/** @see de.uka.ipd.idaho.gamta.QueriableAnnotation#getAnnotationNestingOrder()
		 */
		public String getAnnotationNestingOrder() {
			return this.source.getAnnotationNestingOrder();
		}
		
		/** @see de.uka.ipd.idaho.gamta.Attributed#setAttribute(java.lang.String)
		 */
		public void setAttribute(String name) {
			if (Annotation.START_INDEX_ATTRIBUTE.equals(name)) {}
			else if (GPath.ABSOLUTE_START_INDEX_ATTRIBUTE.equals(name)) {}
			else if (Annotation.SIZE_ATTRIBUTE.equals(name)) {}
			else if (Annotation.END_INDEX_ATTRIBUTE.equals(name)) {}
			else if (Annotation.ANNOTATION_VALUE_ATTRIBUTE.equals(name)) {}
			else if (Annotation.ANNOTATION_ID_ATTRIBUTE.equals(name)) {}
			else if (Token.PARAGRAPH_END_ATTRIBUTE.equals(name)) {}
			else this.source.tokenAt(this.index).setAttribute(name);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#setAttribute(java.lang.String, java.lang.String)
		 */
		public Object setAttribute(String name, Object value) {
			if (Annotation.START_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getStartIndex());
			else if (GPath.ABSOLUTE_START_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getAbsoluteStartIndex());
			else if (Annotation.SIZE_ATTRIBUTE.equals(name)) return ("" + this.size());
			else if (Annotation.END_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getEndIndex());
			else if (Annotation.ANNOTATION_VALUE_ATTRIBUTE.equals(name)) return this.getValue();
			else if (Annotation.ANNOTATION_ID_ATTRIBUTE.equals(name)) return this.getAnnotationID();
			else if (Token.PARAGRAPH_END_ATTRIBUTE.equals(name)) return this.source.tokenAt(this.index).setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
			else return this.source.tokenAt(this.index).setAttribute(name, value);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAttribute(java.lang.String)
		 */
		public Object getAttribute(String name) {
			if (Token.PARAGRAPH_END_ATTRIBUTE.equals(name)) return (this.source.tokenAt(this.index).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE) ? GPathBoolean.TRUE : GPathBoolean.FALSE);
			else return this.getAttribute(name, null);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getAttribute(java.lang.String, java.lang.String)
		 */
		public Object getAttribute(String name, Object def) {
			if (Annotation.START_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getStartIndex());
			else if (GPath.ABSOLUTE_START_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getAbsoluteStartIndex());
			else if (Annotation.SIZE_ATTRIBUTE.equals(name)) return ("" + this.size());
			else if (Annotation.END_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getEndIndex());
			else if (Annotation.ANNOTATION_VALUE_ATTRIBUTE.equals(name)) return this.getValue();
			else if (Annotation.ANNOTATION_ID_ATTRIBUTE.equals(name)) return this.getAnnotationID();
			else if (Token.PARAGRAPH_END_ATTRIBUTE.equals(name)) return (this.source.tokenAt(this.index).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE) ? GPathBoolean.TRUE : def);
			else return this.source.tokenAt(this.index).getAttribute(name, def);
		}
			
		/** @see de.uka.ipd.idaho.gamta.Annotation#hasAttribute(java.lang.String)
		 */
		public boolean hasAttribute(String name) {
			return (this.source.tokenAt(this.index).hasAttribute(name) 
					|| Annotation.START_INDEX_ATTRIBUTE.equals(name) 
					|| GPath.ABSOLUTE_START_INDEX_ATTRIBUTE.equals(name) 
					|| Annotation.SIZE_ATTRIBUTE.equals(name) 
					|| Annotation.END_INDEX_ATTRIBUTE.equals(name)
					|| Annotation.ANNOTATION_VALUE_ATTRIBUTE.equals(name)
					|| Annotation.ANNOTATION_ID_ATTRIBUTE.equals(name)
					|| (Token.PARAGRAPH_END_ATTRIBUTE.equals(name) && this.source.tokenAt(this.index).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE))
					);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Attributed#clearAttributes()
		 */
		public void clearAttributes() {
			this.source.tokenAt(this.index).clearAttributes();
		}

		/** @see de.uka.ipd.idaho.gamta.Attributed#copyAttributes(de.uka.ipd.idaho.gamta.Attributed)
		 */
		public void copyAttributes(Attributed source) {
			this.source.tokenAt(this.index).copyAttributes(source);
		}

		/** @see de.uka.ipd.idaho.gamta.Attributed#getAttributeNames()
		 */
		public String[] getAttributeNames() {
			return this.source.tokenAt(this.index).getAttributeNames();
		}

		/** @see de.uka.ipd.idaho.gamta.Attributed#removeAttribute(java.lang.String)
		 */
		public Object removeAttribute(String name) {
			if (Annotation.START_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getStartIndex());
			else if (GPath.ABSOLUTE_START_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getAbsoluteStartIndex());
			else if (Annotation.SIZE_ATTRIBUTE.equals(name)) return ("" + this.size());
			else if (Annotation.END_INDEX_ATTRIBUTE.equals(name)) return ("" + this.getEndIndex());
			else if (Annotation.ANNOTATION_VALUE_ATTRIBUTE.equals(name)) return this.getValue();
			else if (Annotation.ANNOTATION_ID_ATTRIBUTE.equals(name)) return this.getAnnotationID();
			else return this.source.tokenAt(this.index).removeAttribute(name);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Annotation#getDocumentProperty(java.lang.String)
		 */
		public String getDocumentProperty(String propertyName) {
			return this.source.getDocumentProperty(propertyName);
		}

		/** @see de.uka.ipd.idaho.gamta.Annotation#getDocumentProperty(java.lang.String, java.lang.String)
		 */
		public String getDocumentProperty(String propertyName, String defaultValue) {
			return this.source.getDocumentProperty(propertyName, defaultValue);
		}

		/** @see de.uka.ipd.idaho.gamta.Annotation#getDocumentPropertyNames()
		 */
		public String[] getDocumentPropertyNames() {
			return this.source.getDocumentPropertyNames();
		}

		/** @see de.uka.ipd.idaho.gamta.TokenSequence#tokenAt(int)
		 */
		public Token tokenAt(int index) {
			if (index == 0) return this.source.tokenAt(this.index);
			throw new IndexOutOfBoundsException("" + index);
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#firstToken()
		 */
		public Token firstToken() {
			return this.source.tokenAt(this.index);
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#lastToken()
		 */
		public Token lastToken() {
			return this.source.tokenAt(this.index);
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#valueAt(int)
		 */
		public String valueAt(int index) {
			if (index == 0) return this.source.tokenAt(this.index).getValue();
			throw new IndexOutOfBoundsException("" + index);
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#getWhitespaceAfter(int)
		 */
		public String getWhitespaceAfter(int index) {
			if (index == 0) return "";
			throw new IndexOutOfBoundsException("" + index);
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#firstValue()
		 */
		public String firstValue() {
			return this.source.tokenAt(this.index).getValue();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#lastValue()
		 */
		public String lastValue() {
			return this.source.tokenAt(this.index).getValue();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#getLeadingWhitespace()
		 */
		public String getLeadingWhitespace() {
			return "";
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#length()
		 */
		public int length() {
			return this.source.tokenAt(this.index).length();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#getTokenizer()
		 */
		public Tokenizer getTokenizer() {
			return this.source.tokenAt(this.index).getTokenizer();
		}
		
		/** @see de.uka.ipd.idaho.gamta.TokenSequence#getSubsequence(int, int)
		 */
		public TokenSequence getSubsequence(int start, int size) {
			return new PlainTokenSequence(this, this.getTokenizer());
		}
		
		/** @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if (o == null) return -1;
			if (o instanceof GPathAttributeAnnotation) return -1;
			if (o instanceof Annotation) {
				int c = AnnotationUtils.compare(this, ((Annotation) o));
				return ((c == 0) ? 1 : c);
			}
			else return 1;
		}
		
		/** @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object o) {
			return (this.compareTo(o) == 0);
		}
	}
}