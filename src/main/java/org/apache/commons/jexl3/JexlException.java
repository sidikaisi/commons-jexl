/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import org.apache.commons.jexl3.internal.Debugger;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.ParseException;
import org.apache.commons.jexl3.parser.TokenMgrError;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Wraps any error that might occur during interpretation of a script or expression.
 * @since 2.0
 */
public class JexlException extends RuntimeException {
    /** The point of origin for this exception. */
    private final transient JexlNode mark;
    /** The debug info. */
    private final transient JexlInfo info;
    /** A marker to use in NPEs stating a null operand error. */
    public static final String NULL_OPERAND = "jexl.null";
    /** Minimum number of characters around exception location. */
    private static final int MIN_EXCHARLOC = 10;
    /** Maximum number of characters around exception location. */
    private static final int MAX_EXCHARLOC = 15;

    /**
     * Creates a new JexlException.
     * @param node the node causing the error
     * @param msg the error message
     */
    public JexlException(JexlNode node, String msg) {
        this(node, msg, null);
    }

    /**
     * Creates a new JexlException.
     * @param node the node causing the error
     * @param msg the error message
     * @param cause the exception causing the error
     */
    public JexlException(JexlNode node, String msg, Throwable cause) {
        this(node != null ? node.jexlInfo() : null, msg, cause);
    }

    /**
     * Creates a new JexlException.
     * @param jinfo the debugging information associated
     * @param msg the error message
     * @param cause the exception causing the error
     */
    public JexlException(JexlInfo jinfo, String msg, Throwable cause) {
        super(msg, unwrap(cause));
        mark = null;
        info = jinfo;
    }

    /**
     * Unwraps the cause of a throwable due to reflection. 
     * @param xthrow the throwable
     * @return the cause
     */
    private static Throwable unwrap(Throwable xthrow) {
        if (xthrow instanceof InvocationTargetException) {
            return ((InvocationTargetException) xthrow).getTargetException();
        } else if (xthrow instanceof UndeclaredThrowableException) {
            return ((UndeclaredThrowableException) xthrow).getUndeclaredThrowable();
        } else {
            return xthrow;
        }
    }

    /**
     * Accesses detailed message.
     * @return  the message
     */
    protected String detailedMessage() {
        return super.getMessage();
    }

    /**
     * Formats an error message from the parser.
     * @param prefix the prefix to the message
     * @param expr the expression in error
     * @return the formatted message
     */
    protected String parserError(String prefix, String expr) {
        int begin = info.getColumn();
        int end = begin + MIN_EXCHARLOC;
        begin -= MIN_EXCHARLOC;
        if (begin < 0) {
            end += MIN_EXCHARLOC;
            begin = 0;
        }
        int length = expr.length();
        if (length < MAX_EXCHARLOC) {
            return prefix + " error in '" + expr + "'";
        } else {
            return prefix + " error near '... "
                    + expr.substring(begin, end > length ? length : end) + " ...'";
        }
    }

    /**
     * Thrown when tokenization fails.
     * @since 3.0
     */
    public static class Tokenization extends JexlException {
        /**
         * Creates a new Tokenization exception instance.
         * @param info the location info
         * @param expr the expression
         * @param cause the javacc cause
         */
        public Tokenization(JexlInfo info, CharSequence expr, TokenMgrError cause) {
            super(merge(info, cause), expr.toString(), cause);
        }

        /**
         * Merge the node info and the cause info to obtain best possible location.
         * @param info the node
         * @param cause the cause
         * @return the info to use
         */
        private static JexlInfo merge(JexlInfo info, TokenMgrError cause) {
            JexlInfo dbgn = info != null ? info : null;
            if (cause == null) {
                return dbgn;
            } else if (dbgn == null) {
                return new JexlInfo("", cause.getLine(), cause.getColumn());
            } else {
                return new JexlInfo(dbgn.getName(), cause.getLine(), cause.getColumn());
            }
        }

        /**
         * @return the expression
         */
        public String getExpression() {
            return super.detailedMessage();
        }

        @Override
        protected String detailedMessage() {
            return parserError("tokenization", getExpression());
        }
    }

    /**
     * Thrown when parsing fails.
     * @since 3.0
     */
    public static class Parsing extends JexlException {
        /**
         * Creates a new Variable exception instance.
         * @param info the location information
         * @param expr the offending source
         * @param cause the javacc cause
         */
        public Parsing(JexlInfo info, CharSequence expr, ParseException cause) {
            super(merge(info, cause), expr.toString(), cause);
        }

        /**
         * Merge the node info and the cause info to obtain best possible location.
         * @param info the location information
         * @param cause the cause
         * @return the info to use
         */
        private static JexlInfo merge(JexlInfo info, ParseException cause) {
            JexlInfo dbgn = info != null ? info : null;
            if (cause == null) {
                return dbgn;
            } else if (dbgn == null) {
                return new JexlInfo("", cause.getLine(), cause.getColumn());
            } else {
                return new JexlInfo(dbgn.getName(), cause.getLine(), cause.getColumn());
            }
        }

        /**
         * @return the expression
         */
        public String getExpression() {
            return super.detailedMessage();
        }

        @Override
        protected String detailedMessage() {
            return parserError("parsing", getExpression());
        }
    }

    /**
     * Thrown when a variable is unknown.
     * @since 3.0
     */
    public static class Variable extends JexlException {
        /**
         * Creates a new Variable exception instance.
         * @param node the offending ASTnode
         * @param var the unknown variable
         * @param cause the exception causing the error
         */
        public Variable(JexlNode node, String var) {
            super(node, var, null);
        }

        /**
         * @return the variable name
         */
        public String getVariable() {
            return super.detailedMessage();
        }

        @Override
        protected String detailedMessage() {
            return "undefined variable " + getVariable();
        }
    }

    /**
     * Thrown when a property is unknown.
     * @since 3.0
     */
    public static class Property extends JexlException {
        /**
         * Creates a new Property exception instance.
         * @param node the offending ASTnode
         * @param var the unknown variable
         */
        public Property(JexlNode node, String var) {
            this(node, var, null);
        }

        /**
         * Creates a new Property exception instance.
         * @param node the offending ASTnode
         * @param var the unknown variable
         * @param cause the exception causing the error
         */
        public Property(JexlNode node, String var, Throwable cause) {
            super(node, var, cause);
        }

        /**
         * @return the property name
         */
        public String getProperty() {
            return super.detailedMessage();
        }

        @Override
        protected String detailedMessage() {
            return "inaccessible or unknown property " + getProperty();
        }
    }

    /**
     * Thrown when a method or ctor is unknown, ambiguous or inaccessible.
     * @since 3.0
     */
    public static class Method extends JexlException {
        /**
         * Creates a new Method exception instance.
         * @param node the offending ASTnode
         * @param name the unknown method
         * @param cause the exception causing the error
         */
        public Method(JexlNode node, String name, Throwable cause) {
            super(node, name, cause);
        }

        /**
         * Creates a new Method exception instance.
         * @param info the location information
         * @param name the unknown method
         * @param cause the exception causing the error
         */
        public Method(JexlInfo info, String name, Throwable cause) {
            super(info, name, cause);
        }

        /**
         * @return the method name
         */
        public String getMethod() {
            return super.detailedMessage();
        }

        @Override
        protected String detailedMessage() {
            return "unknown, ambiguous or inaccessible method " + getMethod();
        }
    }

    /**
     * Thrown to return a value.
     * @since 3.0
     */
    public static class Return extends JexlException {
        /** The returned value. */
        private final Object result;

        /**
         * Creates a new instance of Return.
         * @param node the return node
         * @param msg the message
         * @param value the returned value
         */
        public Return(JexlNode node, String msg, Object value) {
            super(node, msg, null);
            this.result = value;
        }

        /**
         * @return the returned value
         */
        public Object getValue() {
            return result;
        }
    }

    /**
     * Thrown to cancel a script execution.
     * @since 3.0
     */
    public static class Cancel extends JexlException {
        /**
         * Creates a new instance of Cancel.
         * @param node the node where the interruption was detected
         */
        public Cancel(JexlNode node) {
            super(node, "execution cancelled", null);
        }
    }

    /**
     * Gets information about the cause of this error.
     * <p>
     * The returned string represents the outermost expression in error.
     * The info parameter, an int[2] optionally provided by the caller, will be filled with the begin/end offset
     * characters of the precise error's trigger.
     * </p>
     * @param offsets character offset interval of the precise node triggering the error
     * @return a string representation of the offending expression, the empty string if it could not be determined
     */
    public String getInfo(int[] offsets) {
        Debugger dbg = new Debugger();
        if (dbg.debug(mark)) {
            if (offsets != null && offsets.length >= 2) {
                offsets[0] = dbg.start();
                offsets[1] = dbg.end();
            }
            return dbg.data();
        }
        return "";
    }

    /**
     * Detailed info message about this error.
     * Format is "debug![begin,end]: string \n msg" where:
     * - debug is the debugging information if it exists (@link JexlEngine.setDebug)
     * - begin, end are character offsets in the string for the precise location of the error
     * - string is the string representation of the offending expression
     * - msg is the actual explanation message for this error
     * @return this error as a string
     */
    @Override
    public String getMessage() {
        Debugger dbg = new Debugger();
        StringBuilder msg = new StringBuilder();
        if (info != null) {
            msg.append(info.toString());
        }
        if (dbg.debug(mark)) {
            msg.append("![");
            msg.append(dbg.start());
            msg.append(",");
            msg.append(dbg.end());
            msg.append("]: '");
            msg.append(dbg.data());
            msg.append("'");
        }
        msg.append(' ');
        msg.append(detailedMessage());
        Throwable cause = getCause();
        if (cause != null && (Object) NULL_OPERAND == cause.getMessage()) {
            msg.append(" caused by null operand");
        }
        return msg.toString();
    }
}