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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import de.inetsoftware.jwebassembly.WasmException;
import de.inetsoftware.jwebassembly.module.ModuleWriter;
import de.inetsoftware.jwebassembly.module.NumericOperator;
import de.inetsoftware.jwebassembly.module.ValueType;
import de.inetsoftware.jwebassembly.module.ValueTypeConvertion;

/**
 * Module Writer for binary format. http://webassembly.org/docs/binary-encoding/
 * 
 * @author Volker Berlin
 */
public class BinaryModuleWriter extends ModuleWriter implements InstructionOpcodes {

    private static final byte[]   WASM_BINARY_MAGIC   = { 0, 'a', 's', 'm' };

    private static final int      WASM_BINARY_VERSION = 1;

    private WasmOutputStream      wasm;

    private WasmOutputStream      codeStream          = new WasmOutputStream();

    private WasmOutputStream      functionsStream     = new WasmOutputStream();

    private List<FunctionType>    functionTypes       = new ArrayList<>();

    private Map<String, Function> functions           = new LinkedHashMap<>();

    private Map<String, String>   exports             = new LinkedHashMap<>();

    private Function              function;

    private FunctionType          functionType;

    /**
     * Create new instance.
     * 
     * @param output
     *            the target for the module data.
     * @throws IOException
     *             if any I/O error occur
     */
    public BinaryModuleWriter( OutputStream output ) throws IOException {
        wasm = new WasmOutputStream( output );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        wasm.write( WASM_BINARY_MAGIC );
        wasm.writeInt32( WASM_BINARY_VERSION );

        writeTypeSection();
        writeFunctionSection();
        writeExportSection();
        writeCodeSection();

        wasm.close();
    }

    /**
     * Write the type section to the output. This section contains the signatures of the functions.
     * 
     * @throws IOException
     *             if any I/O error occur
     */
    private void writeTypeSection() throws IOException {
        int count = functionTypes.size();
        if( count > 0 ) {
            WasmOutputStream stream = new WasmOutputStream();
            stream.writeVaruint32( count );
            for( FunctionType type : functionTypes ) {
                stream.writeVarint( ValueType.func.getCode() );
                stream.writeVaruint32( type.params.size() );
                for( ValueType valueType : type.params ) {
                    stream.writeVarint( valueType.getCode() );
                }
                if( type.result == null ) {
                    stream.writeVaruint32( 0 );
                } else {
                    stream.writeVaruint32( 1 );
                    stream.writeVarint( type.result.getCode() );
                }
            }
            wasm.writeSection( SectionType.Type, stream, null );
        }
    }

    /**
     * Write the function section to the output. This section contains a mapping from the function index to the type signature index.
     * 
     * @throws IOException
     *             if any I/O error occur
     */
    private void writeFunctionSection() throws IOException {
        int count = functions.size();
        if( count > 0 ) {
            WasmOutputStream stream = new WasmOutputStream();
            stream.writeVaruint32( count );
            for( Function func : functions.values() ) {
                stream.writeVaruint32( func.typeId );
            }
            wasm.writeSection( SectionType.Function, stream, null );
        }
    }

    /**
     * Write the export section to the output. This section contains a mapping from the external index to the type signature index.
     * 
     * @throws IOException
     *             if any I/O error occur
     */
    private void writeExportSection() throws IOException {
        int count = exports.size();
        if( count > 0 ) {
            WasmOutputStream stream = new WasmOutputStream();
            stream.writeVaruint32( count );
            for( Map.Entry<String,String> entry : exports.entrySet() ) {
                String exportName = entry.getKey();
                byte[] bytes = exportName.getBytes( StandardCharsets.UTF_8 );
                stream.writeVaruint32( bytes.length );
                stream.write( bytes );
                stream.writeVaruint32( ExternalKind.Function.ordinal() );
                int id = functions.get( entry.getValue() ).id;
                stream.writeVaruint32( id );
            }
            wasm.writeSection( SectionType.Export, stream, null );
        }
    }

    /**
     * Write the code section to the output. This section contains the byte code.
     * 
     * @throws IOException
     *             if any I/O error occur
     */
    private void writeCodeSection() throws IOException {
        int size = functions.size();
        if( size == 0 ) {
            return;
        }
        WasmOutputStream stream = new WasmOutputStream();
        stream.writeVaruint32( size );
        functionsStream.writeTo( stream );
        wasm.writeSection( SectionType.Code, stream, null );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeExport( String methodName, String exportName ) throws IOException {
        exports.put( exportName, methodName );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeMethodStart( String name ) throws IOException {
        function = new Function();
        function.id = functions.size();
        functions.put( name, function );
        functionType = new FunctionType();
        codeStream.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeMethodParam( String kind, ValueType valueType ) throws IOException {
        switch( kind ) {
            case "param":
                functionType.params.add( valueType );
                return;
            case "return":
                functionType.result = valueType;
                return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeMethodFinish( List<ValueType> locals ) throws IOException {
        // TODO optimize and search for duplicates
        function.typeId = functionTypes.size();
        functionTypes.add( functionType );

        
        WasmOutputStream localsStream = new WasmOutputStream();
        localsStream.writeVaruint32( locals.size() );
        for( ValueType valueType : locals ) {
            localsStream.writeVaruint32( 1 ); // TODO optimize, write the count of same types.
            localsStream.writeVarint( valueType.getCode() );
        }
        functionsStream.writeVaruint32( localsStream.size() + codeStream.size() + 1 );
        localsStream.writeTo( functionsStream );
        codeStream.writeTo( functionsStream );
        functionsStream.write( END );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeConstInt( int value ) throws IOException {
        codeStream.write( I32_CONST );
        codeStream.writeVarint( value );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeConstLong( long value ) throws IOException {
        codeStream.write( I64_CONST );
        codeStream.writeVarint( value );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeConstFloat( float value ) throws IOException {
        codeStream.write( F32_CONST );
        codeStream.writeFloat( value );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeConstDouble( double value ) throws IOException {
        codeStream.write( F64_CONST );
        codeStream.writeDouble( value );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeLoad( int idx ) throws IOException {
        codeStream.write( GET_LOCAL );
        codeStream.writeVaruint32( idx );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeStore( int idx ) throws IOException {
        codeStream.write( SET_LOCAL );
        codeStream.writeVaruint32( idx );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeNumericOperator( NumericOperator numOp, @Nullable ValueType valueType ) throws IOException {
        int op = 0;
        switch( numOp ) {
            case add:
                switch( valueType ) {
                    case i32:
                        op = I32_ADD;
                        break;
                    case i64:
                        op = I64_ADD;
                        break;
                    case f32:
                        op = F32_ADD;
                        break;
                    case f64:
                        op = F64_ADD;
                        break;
                }
                break;
            case sub:
                switch( valueType ) {
                    case i32:
                        op = I32_SUB;
                        break;
                    case i64:
                        op = I64_SUB;
                        break;
                    case f32:
                        op = F32_SUB;
                        break;
                    case f64:
                        op = F64_SUB;
                        break;
                }
                break;
            case mul:
                switch( valueType ) {
                    case i32:
                        op = I32_MUL;
                        break;
                    case i64:
                        op = I64_MUL;
                        break;
                    case f32:
                        op = F32_MUL;
                        break;
                    case f64:
                        op = F64_MUL;
                        break;
                }
                break;
            case div:
                switch( valueType ) {
                    case i32:
                        op = I32_DIV_S;
                        break;
                    case i64:
                        op = I64_DIV_S;
                        break;
                    case f32:
                        op = F32_DIV;
                        break;
                    case f64:
                        op = F64_DIV;
                        break;
                }
                break;
            case rem:
                switch( valueType ) {
                    case i32:
                        op = I32_REM_S;
                        break;
                    case i64:
                        op = I64_REM_S;
                        break;
                }
                break;
        }
        if( op == 0 ) {
            throw new Error();
        }
        codeStream.write( op );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeCast( ValueTypeConvertion cast ) throws IOException {
        int op;
        switch( cast ) {
            case l2i:
                op = I32_WRAP_I64;
                break;
            default:
                throw new Error( "Unknown cast: " + cast );
        }
        codeStream.write( op );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeReturn() throws IOException {
        codeStream.write( RETURN );
    }
}
