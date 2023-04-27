package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenType;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.util.Objects;

public class EqualityExpression extends Expression {

    private final Token operator;
    private final Expression leftHandSide;
    private final Expression rightHandSide;

    public EqualityExpression(Token operator, Expression leftHandSide, Expression rightHandSide) {
        this.leftHandSide = addChild(leftHandSide);
        this.rightHandSide = addChild(rightHandSide);
        this.operator = operator;
    }

    public Expression getLeftHandSide() {
        return leftHandSide;
    }

    public Expression getRightHandSide() {
        return rightHandSide;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + operator.getStringValue() + "]";
    }

    public boolean isEqual() {
        return operator.getType().equals(TokenType.EQUAL_EQUAL);
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        leftHandSide.validate(symbolTable);
        rightHandSide.validate(symbolTable);
    }

    @Override
    public CatscriptType getType() {
        return CatscriptType.BOOLEAN;
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        Object lhsValue = leftHandSide.evaluate(runtime);
        Object rhsValue = rightHandSide.evaluate(runtime);
        if (isEqual()){
            return Objects.equals(lhsValue, rhsValue);
        }
        else {
            return !(Objects.equals(lhsValue, rhsValue));
        }
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        //compile lhs
        getLeftHandSide().compile(code);
        //box lhs
        box(code, getLeftHandSide().getType());
        //compile rhs
        getRightHandSide().compile(code);
        //box rhs
        box(code, getRightHandSide().getType());
        code.addMethodInstruction(Opcodes.INVOKESTATIC,
                ByteCodeGenerator.internalNameFor(Objects.class),
                "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
        // invoke static Objects.equals() method
        if (!isEqual()){
            Label setTrue = new Label();
            Label endLabel = new Label();
            code.addJumpInstruction(Opcodes.IFEQ, setTrue);  // if it is 0 which means it's false
            code.pushConstantOntoStack(false);   // go here if it's 0
            code.addJumpInstruction(Opcodes.GOTO, endLabel); // then here to end label
            code.addLabel(setTrue);  // if it's 1 which is true
            code.pushConstantOntoStack(true);   // go here
            code.addLabel(endLabel);  // end the label

        }




    }


}
