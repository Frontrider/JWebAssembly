/*
 * Copyright 2017 Volker Berlin (i-net software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.inetsoftware.jwebassembly;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.junit.rules.TemporaryFolder;

/**
 * A Junit Rule that compile the given classes and compare the results from node and Java.
 * 
 * @author Volker Berlin
 */
public class WasmNodeRule extends TemporaryFolder {

    private final Class<?>[] classes;

    private File             wasmFile;

    private File             scriptFile;

    /**
     * Compile the given classes to a Wasm and save it to a file.
     * 
     * @param classes
     *            list of classes to compile
     */
    public WasmNodeRule( Class<?>... classes ) {
        if( classes == null || classes.length == 0 ) {
            throw new IllegalArgumentException( "You need to set minimum one test class" );
        }
        this.classes = classes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void before() throws Throwable {
        super.before();
        try {
            wasmFile = newFile( "test.wasm" );
            JWebAssembly wasm = new JWebAssembly();
            for( Class<?> clazz : classes ) {
                URL url = clazz.getResource( '/' + clazz.getName().replace( '.', '/' ) + ".class" );
                wasm.addFile( url );
            }
            wasm.compileToBinary( wasmFile );

            scriptFile = newFile( "test.js" );
            URL scriptUrl = getClass().getResource( "nodetest.js" );
            String expected = readStream( scriptUrl.openStream() );
            expected = expected.replace( "{test.wasm}", wasmFile.getAbsolutePath().replace( '\\', '/' ) );
            try (FileOutputStream scriptStream = new FileOutputStream( scriptFile )) {
                scriptStream.write( expected.getBytes( StandardCharsets.UTF_8 ) );
            }
        } catch( Exception ex ) {
            throwException( ex );
        }
    }

    /**
     * Run a test single test. It run the method in Java and call it via node in the WenAssembly. If the result are
     * different it fire an error.
     * 
     * @param methodName
     *            the method name of the test.
     * @param params
     *            the parameters for the method
     */
    public void test( String methodName, Object... params ) {
        try {
            Class<?>[] types = new Class[params.length];
            for( int i = 0; i < types.length; i++ ) {
                Class<?> type = params[i].getClass();
                switch( type.getName() ) {
                    case "java.lang.Integer":
                        type = int.class;
                        break;
                }
                types[i] = type;
            }
            Method method = null;
            for( int i = 0; i < classes.length; i++ ) {
                try {
                    Class<?> clazz = classes[i];
                    method = clazz.getDeclaredMethod( methodName, types );
                } catch( NoSuchMethodException ex ) {
                    if( i == classes.length - 1 ) {
                        throw ex;
                    }
                }
            }
            method.setAccessible( true );
            Object expected = method.invoke( null, params );

            String command = System.getProperty( "node.dir" );
            command = command == null ? "node" : command + "/node";
            ProcessBuilder processBuilder =
                            new ProcessBuilder( command, scriptFile.getAbsolutePath(), methodName );
            for( int i = 0; i < params.length; i++ ) {
                processBuilder.command().add( String.valueOf( params[i] ) );

            }
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            String result = readStream( process.getInputStream() );
            if( exitCode != 0 ) {
                String errorMessage = readStream( process.getErrorStream() );
                assertEquals( errorMessage, 0, exitCode );
            }
            assertEquals( String.valueOf( expected ), result );
        } catch( Exception ex ) {
            throwException( ex );
        }
    }

    /**
     * Reads a stream into a String.
     * 
     * @param input
     *            the InputStream
     * @return the string
     */
    public static String readStream( InputStream input ) {
        try (Scanner scanner = new Scanner( input ).useDelimiter( "\\A" ) ) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    /**
     * Throw any exception independent of signatures
     * 
     * @param exception
     *            the exception
     * @throws T
     *             a generic helper
     */
    public static <T extends Throwable> void throwException( Throwable exception ) throws T {
        throw (T)exception;
    }
}
