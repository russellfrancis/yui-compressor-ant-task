/*
 * YUI Compressor
 * Author: Russell Francis (russ@metro-six.com)
 * Copyright (c) 2007, Yahoo! Inc. All rights reserved.
 * Code licensed under the BSD License:
 *     http://developer.yahoo.net/yui/license.txt
 */
package com.metrosix.yuicompressor.anttask;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * This class implements an ant task which can be used to process sets of
 * javascript and css files to make them smaller.  It will remove unneeded spaces,
 * and can also obfuscate variable names which result in smaller code.  This
 * allows us to shrink the size of downloaded css and js files from our applications
 * web-server.
 * 
 * @author Russell Francis (russ@metro-six.com)
 */
public class YuiCompressorTask extends Task {
    
    // Javascript options.
    private boolean munge = true;
    private boolean warn = false;
    private boolean preserveAllSemiColons = false;
    private boolean preserveStringLiterals = false;    
    
    private int maxColumnWidth = -1;
    private File destDir = null;
    private List fileSets = new ArrayList();
    private String type = null;    
 
    /**
     * Construct a new YuiCompressorTask to process files using the YUI javascript
     * and css compression tool.
     */
    public YuiCompressorTask() {        
        super();
    }
    
    /**
     * Add a fileset which should be processed by this task.
     * @param fileSet The FileSet object which we should process.
     */
    public void addFileset(FileSet fileSet) {
        if (fileSet == null) {
            throw new IllegalArgumentException( "The parameter fileSet must be non-null." );
        }
        this.fileSets.add(fileSet);
    }
    
    /**
     * The destination directory we should use when copying
     * @param destDir  The destination directory we should use when copying a 
     * FileSet.
     */
    public void setTodir( File destDir ) {
        this.destDir = destDir;
    }
    
    /**
     * Set the type of compression to apply to the files within the fileset, we
     * can either compress "js" or "css" files.  We usually don't need to set this
     * as the type will be guessed based on the extension of the file.  If you
     * use a different extension for .css files or .js files then you will have
     * to instruct the task which method to use here.
     * @param type The type of compression to apply either "js" or "css".
     */
    public void setType( String type ) {
        if (type == null) {
            throw new IllegalArgumentException( "The parameter type must be non-null." );            
        }
        if (type.compareToIgnoreCase("js") != 0 && type.compareToIgnoreCase("css") != 0) {
            throw new IllegalArgumentException( "The parameter type must be either \"js\" or \"css\"." );
        }
        this.type = type;
    }
    
    /**
     * If this is enabled, we will change variable names to shorter values and 
     * otherwise obfuscate the code.  Otherwise, we will just minimize the source
     * which is a less aggressive optimization.  The default is true and it should
     * not damage the functionality of the original file.  The default is true.
     * @param munge Whether we should obfuscate the source or not.
     */
    public void setMunge( boolean munge ) {
        this.munge = munge;
    }
    
    /**
     * Whether or not we should warn about bad javascript practices and other
     * style things that we don't like.  The default is false.
     * @param warn Whether or not to warn about potentially bad javascript code.
     */
    public void setWarn( boolean warn ) {
        this.warn = warn;
    }
    
    /**
     * Whether we should preserve all semicolons, even those which are not needed.
     * The default is false.
     * @param preserveAllSemiColons Whether or not to preserve all semi-colons or
     * whether we can delete them if they are not needed.
     */
    public void setPreserveAllSemiColons( boolean preserveAllSemiColons ) {
        this.preserveAllSemiColons = preserveAllSemiColons;
    }

    /**
     * Whether or not to merge concatenated string literals.  The default is false.
     * @param preserveStringLiterals If true, we will not modify string literals,
     * if false, we will join them into larger string literals for performance and
     * space savings.
     */
    public void setPreserveStringLiterals( boolean preserveStringLiterals ) {
        this.preserveStringLiterals = preserveStringLiterals;
    }

    /**
     * The compressor will squish many lines of code onto a single line.  If you
     * wish to not have lines longer than a certain length, you may set that here.
     * 
     * @param maxColumnWidth The maximum length of a line which we should export.
     */
    public void setMaxColumnWidth( int maxColumnWidth ) {
        this.maxColumnWidth = maxColumnWidth;
        if (this.maxColumnWidth < 0) {
            this.maxColumnWidth = -1;
        }
    }
   
    /**
     * Perform the compression of the files from the fileset.
     */
    @Override
    public void execute() {
        try {            
            // handle filesets.
            if (destDir != null) {
                for( int i = 0; i < fileSets.size(); ++i ) {
                    FileSet fileSet = (FileSet) fileSets.get(i);
                    File fromDir = fileSet.getDir(this.getProject());                    
                    DirectoryScanner directoryScanner = fileSet.getDirectoryScanner(this.getProject());
                    String[] srcFiles = directoryScanner.getIncludedFiles();
                    for (int j = 0; j < srcFiles.length; ++j) {
                        File srcFile = new File( fromDir + File.separator + srcFiles[j] );
                        File destFile = new File( destDir + File.separator + srcFiles[j] );
                        compressFile( srcFile, destFile );
                    }
                }
            }
        }
        catch( IOException e ) {
            getProject().log( e.getMessage() );
        }
    }

    /**
     * Given a srcFile and destFile, perform the necessary compression either
     * css or js saving the compressed srcFile to destFile.
     * @param srcFile The source file to compress.
     * @param destFile The destination file.
     * @throws java.io.IOException If we have trouble writing the file for any
     * reason.
     */
    private void compressFile( File srcFile, File destFile ) throws IOException {
        // handle individual files.
        if (srcFile != null && srcFile.exists() && destFile != null) {
            // ensure destFile exists.
            if (!destFile.getParentFile().mkdirs()) {
                throw new IOException("Unable to create destination location '" + 
                        destFile.getParentFile().getAbsolutePath() + "'.");
            }
            if( (type != null && type.compareTo("css") == 0)    ||
                (type == null && srcFile.getName().endsWith("css") ) )
            {
                this.compressCss(srcFile, destFile);
            }
            else if(    (type != null && type.compareTo("js") == 0) ||
                        (type == null && srcFile.getName().endsWith("js") ) )
            {
                // javascript compression.
                this.compressJs( srcFile, destFile );
            } else {
                // unsupported.
                this.getProject().log( "Unable to determine the type of compression to apply to '" + 
                        srcFile.getName() + "'." );
            }
        }        
    }
    
    /**
     * Compress the given source file as if it was a .css file.
     * @param srcFile The source css file we wish to compress.
     * @param destFile The destination file to write the compressed css file to.
     * @throws java.io.IOException If there is some error reading or writing 
     * the files.
     */
    private void compressCss( File srcFile, File destFile ) throws IOException {
        Reader reader = new FileReader( srcFile );
        try {
            Writer writer = new FileWriter( destFile );
            try {
                CssCompressor cssCompressor = new CssCompressor( reader );
                cssCompressor.compress(writer, maxColumnWidth);
            } finally {
                writer.close();
            }
        }
        finally {
            reader.close();
        }
    }
    
    /**
     * Compress the given source file as though it was a javascript file.
     * @param srcFile The source file to compress as a javascript file.
     * @param destFile The destination file to write the compressed file to.
     * @throws java.io.IOException If there is an error reading or writing from
     * or to either of the files.
     */
    private void compressJs(File srcFile, File destFile) throws IOException {
        Reader reader = new FileReader( srcFile );
        try {
            Writer writer = new FileWriter( destFile );
            try {
                JavaScriptCompressor compressor = new JavaScriptCompressor(reader, new AntErrorReporter( this.getProject() ) );
                compressor.compress( writer, maxColumnWidth, munge, warn, preserveAllSemiColons, preserveStringLiterals );
            } finally {
                writer.close();
            }
        }
        finally {
            reader.close();
        }
    }
} 
