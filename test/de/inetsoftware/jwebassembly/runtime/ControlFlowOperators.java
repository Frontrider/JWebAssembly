/*
 * Copyright 2018 Volker Berlin (i-net software)
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
package de.inetsoftware.jwebassembly.runtime;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.runners.Parameterized.Parameters;
import org.webassembly.annotation.Export;

import de.inetsoftware.jwebassembly.ScriptEngine;
import de.inetsoftware.jwebassembly.WasmRule;

public class ControlFlowOperators extends AbstractBaseTest {

    @ClassRule
    public static WasmRule rule = new WasmRule( TestClass.class );

    public ControlFlowOperators( ScriptEngine script, String method, Object[] params ) {
        super( rule, script, method, params );
    }

    @Parameters( name = "{0}-{1}" )
    public static Collection<Object[]> data() {
        ArrayList<Object[]> list = new ArrayList<>();
        for( ScriptEngine[] val : ScriptEngine.testParams() ) {
            ScriptEngine script = val[0];
            addParam( list, script, "ifeq" );
            addParam( list, script, "ifne" );
            addParam( list, script, "iflt" );
            addParam( list, script, "ifMultiple" );
            addParam( list, script, "ifCompare" );
            addParam( list, script, "switchDirect" );
            addParam( list, script, "endlessLoop" );
            addParam( list, script, "forLoop" );
        }
        return list;
    }

    static class TestClass {

        @Export
        static int ifeq() {
            int condition = 0;
            if( condition != 0 ) {
                return 13;
            } else {
                return 76;
            }
        }

        @Export
        static int ifne() {
            int condition = 3;
            if( condition == 0 ) {
                return 13;
            } else {
                return 76;
            }
        }

        @Export
        static int iflt() {
            int condition = 3;
            if( condition >= 0 ) {
                condition = 13;
            } else {
                condition = 76;
            }
            return condition;
        }

        @Export
        static int ifMultiple() {
            int condition = 3;
            if( condition <= 0 ) {
                if( condition < 0 ) {
                    condition = 13;
                }
            } else {
                if( condition > 0 ) {
                    condition++;
                } else {
                    condition--;
                }
            }
            if( condition > 2 ) {
                condition++;
            } else {
                condition = 0;
            }
            if( condition >= 2 ) {
                condition++;
            } else {
                condition = 0;
            }
            if( condition <= 123 ) {
                condition++;
            } else {
                condition = 0;
            }
            if( condition < 123 ) {
                condition++;
            } else {
                condition = 0;
            }
            if( condition != 123 ) {
                condition++;
            } else {
                condition = 0;
            }
            return condition;
        }

        @Export
        static int ifCompare() {
            double condition = 3.0;
            int result;
            if( condition >= 3.5 ) {
                result = 13;
            } else {
                result = 76;
            }
            return result;
        }

        @Export
        static int switchDirect() {
            return tableSwitch(10) + (tableSwitch( 9 ) * 10) + (lookupSwitch(Integer.MAX_VALUE) * 100) + (lookupSwitch(0) * 1000);
        }

        private static int tableSwitch( int a ) {
            int b;
            switch(a){
                case 10:
                    b = 1;
                    break;
                default:
                    b = 9;
            }
            return b;
        }

        private static int lookupSwitch( int a ) {
            int b;
            switch(a){
                case 1:
                    b = 1;
                    break;
                case 1000:
                case 1001:
                    if( a == 1000 ) {
                        b = 2;
                        break;
                    } else {
                        b = 0;
                    }
                    //$FALL-THROUGH$
                case Integer.MAX_VALUE:
                    b = 3;
                    break;
                default:
                    b = 9;
            }
            return b;
        }

        @Export
        static int endlessLoop() {
            int a = 0;
            int b = 0;
            do {
                if( a < 10 ) {
                    b++;
                } else {
                    return a;
                }
                a++;
            } while( true );
        }

        @Export
        static int forLoop() {
            int a = 0;
            //            for( int i=0; i<10;i++) {
            //                a += i;
            //            }
            return a;
        }
    }
}
