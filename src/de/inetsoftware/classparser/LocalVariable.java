/*
   Copyright 2011 - 2017 Volker Berlin (i-net software)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/
package de.inetsoftware.classparser;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * @author Volker Berlin
 */
public class LocalVariable {

    private int start_pc;

    private int length;

    private int name_index;

    private int descriptor_index;

    private int index;

    private boolean declared;

    protected LocalVariable() {
        // nothing
    }

    /**
     * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.13
     * http://docs.oracle.com/javase/specs/jvms/se5.0/html/ClassFile.doc.html#5956
     *
     * @param input
     *            the stream of the class
     * @throws IOException
     */
    LocalVariable( DataInputStream input ) throws IOException {
        start_pc = input.readUnsignedShort();
        length = input.readUnsignedShort();
        name_index = input.readUnsignedShort();
        descriptor_index = input.readUnsignedShort();
        index = input.readUnsignedShort();
    }

    /**
     * Get the index in the local variable table.
     * @return the index.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get the name of the variable
     * @param constantPool ConstantPool of the current class
     */
    public String getName( ConstantPool constantPool ) {
        return (String)constantPool.get( name_index );
    }

    public int getDescriptorIdx() {
        return descriptor_index;
    }

    /**
     * Was the declaration printed?
     * @return true if already declared
     */
    public boolean isDeclared() {
        return declared;
    }

    /**
     * Mark this variable as declared.
     */
    public void setDeclared() {
        declared = true;
    }
}
