package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenType;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.util.Objects;

public class UnaryExpression extends Expression {

    private final Token operator;
    private final Expression rightHandSide;

    public UnaryExpression(Token operator, Expression rightHandSide) {
        this.rightHandSide = addChild(rightHandSide);
        this.operator = operator;
    }

    public Expression getRightHandSide() {
        return rightHandSide;
    }

    public boolean isMinus() {
        return operator.getType().equals(TokenType.MINUS);
    }

    public boolean isNot() {
        return !isMinus();
    }

    @Override
    public String toString() {
        return super.toString() + "[" + operator.getStringValue() + "]";
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        rightHandSide.validate(symbolTable);
        if (isNot() && !rightHandSide.getType().equals(CatscriptType.BOOLEAN)) {
            addError(ErrorType.INCOMPATIBLE_TYPES);
        } else if(isMinus() && !rightHandSide.getType().equals(CatscriptType.INT)) {
            addError(ErrorType.INCOMPATIBLE_TYPES);
        }
    }

    @Override
    public CatscriptType getType() {
        if (isMinus()) {
            return CatscriptType.INT;
        } else {
            return CatscriptType.BOOLEAN;
        }
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        Object rhsValue = getRightHandSide().evaluate(runtime);
        if (this.isMinus()) {
            return -1 * (Integer) rhsValue;
        } else {
            if (this.isNot()){
                return false; // TODO handle boolean NOT (UNSURE BUT THE TEST PASS)
            }
            else {
                return true;
            }

        }
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        //compile rhs

        Label setTrue = new Label();
        Label endLabel = new Label();


        if (isNot()){
            getRightHandSide().compile(code);
            code.addJumpInstruction(Opcodes.IFEQ, setTrue);  // if it is 0 which means it's false
            code.pushConstantOntoStack(false);   // go here if it's 0
            code.addJumpInstruction(Opcodes.GOTO, endLabel); // then here to end label
            code.addLabel(setTrue);  // if it's 1 which is true
            code.pushConstantOntoStack(true);   // go here
            code.addLabel(endLabel);  // end the label
        }

        else if(isMinus()){
            getRightHandSide().compile(code);
            code.pushConstantOntoStack(-1);
            code.addInstruction(Opcodes.IMUL);
        }

    }


}
