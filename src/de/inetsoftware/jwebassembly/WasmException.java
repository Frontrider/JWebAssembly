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

/**
 * If there any error occur on converting a class file to a WebAssembly module.
 * 
 * @author Volker Berlin
 *
 */
public class WasmException extends Exception {

    private int lineNumber;

    /**
     * Create a new instance.
     * 
     * @param message
     *            the error message
     * @param lineNumber
     *            the line number in Java Code
     */
    public WasmException( String message, int lineNumber ) {
        super( message );
        this.lineNumber = lineNumber;
    }

    /**
     * Create a new instance with a cause.
     * 
     * @param cause
     *            the cause
     */
    private WasmException( Throwable cause ) {
        super( cause );
        lineNumber = -1;
    }

    /**
     * Create a wrapped exception needed.
     * 
     * @param cause
     *            the wrapped cause
     * @param lineNumber
     *            the line number in Java Code
     * @return a new instance
     */
    public static WasmException create( Throwable cause, int lineNumber ) {
        WasmException wasmEx = create( cause );
        if( wasmEx.lineNumber < 0 ) {
            wasmEx.lineNumber = lineNumber;
        }
        return wasmEx;
    }

    /**
     * Create a wrapped exception needed.
     * 
     * @param cause
     *            the wrapped cause
     * @return a new instance
     */
    public static WasmException create( Throwable cause ) {
        if( cause instanceof WasmException ) {
            return (WasmException)cause;
        }
        return new WasmException( cause );
    }

    /**
     * Get the line number in Java code on which the error occurred.
     * 
     * @return the line number or -1
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String str = super.toString();
        if( lineNumber > 0 ) {
            str += " at line: " + lineNumber;
        }
        return str;
    }
}
