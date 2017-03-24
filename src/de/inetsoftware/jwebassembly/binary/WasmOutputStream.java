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
package de.inetsoftware.jwebassembly.binary;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnegative;

/**
 * @author Volker Berlin
 */
class WasmOutputStream extends FilterOutputStream {

    /**
     * Create a in memory stream.
     */
    WasmOutputStream() {
        super( new ByteArrayOutputStream() );
    }

    /**
     * Create a wrapped stream.
     * 
     * @param output
     *            the target of data
     */
    WasmOutputStream( OutputStream output ) {
        super( output );
    }

    /**
     * Write a integer little endian (ever 4 bytes)
     * 
     * @param value
     *            the value
     * @throws IOException
     *             if an I/O error occurs.
     */
    void writeInt32( int value ) throws IOException {
        write( (value >>> 0) & 0xFF );
        write( (value >>> 8) & 0xFF );
        write( (value >>> 16) & 0xFF );
        write( (value >>> 24) & 0xFF );
    }

    /**
     * Write an unsigned integer.
     * 
     * @param value
     *            the value
     * @throws IOException
     *             if an I/O error occurs.
     */
    void writeVaruint32( @Nonnegative int value ) throws IOException {
        do {
            int b = value & 0x7F; // low 7 bits
            value >>= 7;
            if( value != 0 ) { /* more bytes to come */
                b |= 0x80;
            }
            write( b );
        } while( value != 0 );
    }

    /**
     * Write an integer value.
     * 
     * @param value
     *            the value
     * @throws IOException
     *             if an I/O error occurs.
     */
    void writeVarint32( int value ) throws IOException {
        while( true ) {
            int b = value & 0x7F;
            value >>= 7;

            /* sign bit of byte is second high order bit (0x40) */
            if( (value == 0 && (b & 0x40) == 0) || (value == -1 && (b & 0x40) != 0) ) {
                write( b );
                return;
            } else {
                write( b | 0x80 );
            }
        }
    }

    /**
     * Write a section with header and data.
     * 
     * @param type
     *            the name of the section
     * @param data
     *            the data of the section
     * @param name
     *            the name, must be set if the id == 0
     * @throws IOException
     *             if any I/O error occur
     */
    void writeSection( SectionType type, WasmOutputStream data, String name ) throws IOException {
        ByteArrayOutputStream baos = (ByteArrayOutputStream)data.out;
        int size = baos.size();
        if( size == 0 ) {
            return;
        }
        writeVaruint32( type.ordinal() );
        writeVaruint32( size );
        if( type == SectionType.Custom ) {
            byte[] bytes = name.getBytes( StandardCharsets.ISO_8859_1 );
            writeVaruint32( bytes.length );
            write( bytes );
        }
        baos.writeTo( this );
    }

    /**
     * Write the data of this stream to the output. Work only for in memory stream.
     * 
     * @param output
     *            the target
     * @throws IOException
     *             if any I/O error occur
     */
    void writeTo( OutputStream output ) throws IOException {
        ByteArrayOutputStream baos = (ByteArrayOutputStream)out;
        baos.writeTo( output );
    }
}
