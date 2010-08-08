/*
 * YUI Compressor
 * Author: Russell Francis (russell.francis@gmail.com)
 * Copyright (c) 2007, Yahoo! Inc. All rights reserved.
 * Code licensed under the BSD License:
 *     http://developer.yahoo.net/yui/license.txt
 */
package com.iwouldrather.yuicompressor.anttask;

import org.apache.tools.ant.Project;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

/**
 * A basic error reporter implementation for reporting errors to an ant project.
 * 
 * @author Russell Francis (russell.francis@gmail.com)
 * @version $Id$
 */
public class AntErrorReporter implements ErrorReporter {    
    //==========================================================================
    //  CLASS
    //==========================================================================
    
    //==========================================================================
    //  INSTANCE
    //==========================================================================    
    private Project project = null;
    
    //--------------------------------------------------------------------------
    /**
     * Construct a new AntErrorReporter instance.
     * @param project The ant project which we should report errors to.
     */
    public AntErrorReporter( Project project ) {
        if (project == null) {
            throw new IllegalArgumentException( "The parameter project must be non-null." );
        }
        this.project = project;
    }
    
    //--------------------------------------------------------------------------
    public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
        if (line < 0) {
            this.project.log("[WARNING] " + message);
        } else {
            this.project.log( line + ':' + lineOffset + ':' + message);
        }
    }

    //--------------------------------------------------------------------------
    public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
        if (line < 0) {
            this.project.log("[ERROR] " + message);
        } else {
            this.project.log( line + ':' + lineOffset + ':' + message);
        }
    }
    
    //--------------------------------------------------------------------------
    public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
        error(message, sourceName, line, lineSource, lineOffset);
        return new EvaluatorException(message);
    }
}
