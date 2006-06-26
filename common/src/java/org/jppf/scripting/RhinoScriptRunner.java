/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.scripting;

import java.util.*;
import org.mozilla.javascript.*;

/**
 * Script runner wrapper around a Rhino script engine.
 * @author Laurent Cohen
 */
public class RhinoScriptRunner implements ScriptRunner
{
	/**
	 * Contains information about the scripts execution environment.
	 */
	private Context context = null;
	/**
	 * Contains information about global variables.
	 */
	private Scriptable scope = null;
	/**
	 * USed to collect error messages.
	 */
	private ErrorHandler errorHandler = new ErrorHandler();

	/**
	 * Initialize the Rhino environment.
	 */
	public RhinoScriptRunner()
	{
	}

	/**
	 * Evaluate the script specified as input and get the evaluation result.
	 * @param script a string containing the script to evaluate.
	 * @param variables a mapping of objects to add the scope of the script.
	 * @return the result of the evaluation as an object.
	 * @throws JPPFScriptingException if an error occurs while evaluating the script.
	 * @see org.jppf.scripting.ScriptRunner#evaluate(java.lang.String)
	 */
	public Object evaluate(String script, Map<String, Object> variables) throws JPPFScriptingException
	{
		init();
		errorHandler.errors.clear();
		Object result = null;
		for (String name: variables.keySet())
		{
			Object wrapped = Context.javaToJS(variables.get(name), scope);
			ScriptableObject.putProperty(scope, name, wrapped);
		}
		try
		{
			result = context.evaluateString(scope, script, "script", 1, null);
		}
		catch(EvaluatorException e)
		{
		}
		finally
		{
			for (String name: variables.keySet())
				ScriptableObject.deleteProperty(scope, name);
			cleanup();
		}
		if (!errorHandler.errors.isEmpty())
		{
			StringBuilder sb = new StringBuilder();
			for (String s: errorHandler.errors)
			{
				if (sb.length() > 0) sb.append("\n");
				sb.append(s);
			}
			sb.insert(0, "Errors occurred while executing the script:\n");
			throw new JPPFScriptingException(sb.toString());
		}
		return result;
	}

	/**
	 * Initialize the execution environment.
	 * @see org.jppf.scripting.ScriptRunner#init()
	 */
	public void init()
	{
		context = Context.enter();
		//scope = context.initStandardObjects(null);
		scope = new ImporterTopLevel(context);
		context.setErrorReporter(errorHandler);
	}

	/**
	 * Perform cleanup after we're done using this script runner.
	 * @see org.jppf.scripting.ScriptRunner#cleanup()
	 */
	public void cleanup()
	{
		Context.exit();
	}

	/**
	 * Error handler for this script runner.
	 */
	public class ErrorHandler implements ErrorReporter
	{
		/**
		 * Errors thrown during execution or translation of a script.
		 */
		public List<String> errors = new ArrayList<String>();

		/**
     * Report an error.
     * If execution has not yet begun, the JavaScript engine is free to
     * find additional errors rather than terminating the translation.
     * It will not execute a script that had errors, however.
     * @param message a String describing the error
     * @param sourceName a String describing the JavaScript source
     * where the error occured; typically a filename or URL
     * @param line the line number associated with the error
     * @param lineSource the text of the line (may be null)
     * @param lineOffset the offset into lineSource where problem was detected
		 * @see org.mozilla.javascript.ErrorReporter#error(java.lang.String, java.lang.String, int, java.lang.String, int)
		 */
		public void error(String message, String sourceName, int line, String lineSource, int lineOffset)
		{
			errors.add(makeErrorString(message, sourceName, line, lineSource, lineOffset));
		}

		/**
     * Report a warning.
     * @param message a String describing the warning
     * @param sourceName a String describing the JavaScript source
     * where the warning occured; typically a filename or URL
     * @param line the line number associated with the warning
     * @param lineSource the text of the line (may be null)
     * @param lineOffset the offset into lineSource where problem was detected
		 * @see org.mozilla.javascript.ErrorReporter#warning(java.lang.String, java.lang.String, int, java.lang.String, int)
		 */
		public void warning(String message, String sourceName, int line, String lineSource, int lineOffset)
		{
		}
	
		/**
     * Creates an EvaluatorException that may be thrown.
     * runtimeErrors, unlike errors, will always terminate the current script.
     * @param message a String describing the error
     * @param sourceName a String describing the JavaScript source
     * where the error occured; typically a filename or URL
     * @param line the line number associated with the error
     * @param lineSource the text of the line (may be null)
     * @param lineOffset the offset into lineSource where problem was detected
     * @return an EvaluatorException that will be thrown.
		 * @see org.mozilla.javascript.ErrorReporter#runtimeError(java.lang.String, java.lang.String, int, java.lang.String, int)
		 */
		public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset)
		{
			String s = makeErrorString(message, sourceName, line, lineSource, lineOffset);
			errors.add(s);
			return new EvaluatorException(s);
		}

		/**
		 * Generate a single string form the parametrs of an error or warning.
     * @param message a String describing the warning
     * @param sourceName a String describing the JavaScript source
     * where the warning occured; typically a filename or URL
     * @param line the line number associated with the warning
     * @param lineSource the text of the line (may be null)
     * @param lineOffset the offset into lineSource where problem was detected
		 * @return a string containing the information about the error.
		 */
		private String makeErrorString(String message, String sourceName, int line, String lineSource, int lineOffset)
		{
			StringBuilder sb = new StringBuilder();
			sb.append(message).append(" at ").append(line).append(":").append(lineOffset).append(":\n");
			sb.append("Source = ").append(lineSource);
			return sb.toString();
		}
	}
}
