package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = null;
        try {
            expression = parseExpression();
        } catch(RuntimeException re) {
            // ignore :)
        }
        if (expression == null || tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement(){

        Statement functionDefStatement = parseFunctionDefinitionStatement();
        if (functionDefStatement != null){
            return functionDefStatement;
        }
        return parseStatement();
    }

    private FunctionDefinitionStatement parseFunctionDefinitionStatement(){
        if(tokens.match(FUNCTION)) {
            Token start = tokens.consumeToken();
            FunctionDefinitionStatement funcDef = new FunctionDefinitionStatement();
            funcDef.setStart(start);

            // require an identifier (name)
            Token functionName = require(IDENTIFIER, funcDef);
            funcDef.setName(functionName.getStringValue());

            // parameters
            //() or (i: int) or (i:int, b:bool)
            // require a (
            require(LEFT_PAREN, funcDef);
            if(!tokens.match(RIGHT_PAREN)){
                do {
                    Token parameterName = require(IDENTIFIER, funcDef);
                    TypeLiteral typeLiteral = null;
                    if (tokens.matchAndConsume(COLON)){
                        typeLiteral = parseTypeLiteral();
                    }
                    funcDef.addParameter(parameterName.getStringValue(), typeLiteral);
                } while(tokens.matchAndConsume(COMMA) && tokens.hasMoreTokens());
            }

            // require )
            require(RIGHT_PAREN, funcDef);

            //function foo() : int {
            TypeLiteral returnType = null;
            if (tokens.matchAndConsume(COLON)){
                //parse return type
                returnType = parseTypeLiteral();
            }
            funcDef.setType(returnType);

            // require {
            require(LEFT_BRACE, funcDef);
            LinkedList<Statement> statements = new LinkedList<>();
            currentFunctionDefinition = funcDef; // so that we know it is inside the function body and can have a return statement
            while(!tokens.match(RIGHT_BRACE) && tokens.hasMoreTokens()){
                statements.add(parseStatement());
            }
            currentFunctionDefinition = null;
            require(RIGHT_BRACE, funcDef);
            funcDef.setBody(statements);
            return funcDef;
        }

        else{
            return null;
        }


    }

    private TypeLiteral parseTypeLiteral(){
        if (tokens.match("int")){
            TypeLiteral typeLiteral = new TypeLiteral();
            typeLiteral.setType(CatscriptType.INT);
            typeLiteral.setToken(tokens.consumeToken());
            return typeLiteral;
        } else if (tokens.match("string")){
            TypeLiteral typeLiteral = new TypeLiteral();
            typeLiteral.setType(CatscriptType.STRING);
            typeLiteral.setToken(tokens.consumeToken());
            return typeLiteral;
        } else if (tokens.match("bool")){
            TypeLiteral typeLiteral = new TypeLiteral();
            typeLiteral.setType(CatscriptType.BOOLEAN);
            typeLiteral.setToken(tokens.consumeToken());
            return typeLiteral;
        } else if (tokens.match("object")){
            TypeLiteral typeLiteral = new TypeLiteral();
            typeLiteral.setType(CatscriptType.OBJECT);
            typeLiteral.setToken(tokens.consumeToken());
            return typeLiteral;
        } else if (tokens.match("list")){
            TypeLiteral typeLiteral = new TypeLiteral();
            typeLiteral.setStart(tokens.consumeToken());
            if (tokens.matchAndConsume(LESS)){
                typeLiteral.setType(CatscriptType.getListType(parseTypeLiteral().getType()));
                Token greater = require(GREATER, typeLiteral);
                typeLiteral.setEnd(greater);
            }
            else{
                typeLiteral.setType(CatscriptType.getListType(CatscriptType.OBJECT));
                typeLiteral.setEnd(typeLiteral.getStart());
            }

            return typeLiteral;
        }
        return null;
    }
    /*
    private Expression parseTypeExpression(){
        TypeLiteral typeLiteral = new TypeLiteral();
        // recursive call here to deal with lists
        typeLiteral.setType();
        return typeLiteral;
    }*/
    private Statement parseStatement() {
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }

        Statement forStmt = parseForStatement();
        if (forStmt != null) {
            return forStmt;
        }

        Statement ifStmt = parseIfStatement();
        if (ifStmt != null) {
            return ifStmt;
        }

        Statement varStmt = parseVarStatement();
        if (varStmt != null) {
            return varStmt;
        }

        Statement assignmentOrFuncCall = parseAssignmentOrFunctionCallStatement();
        if (assignmentOrFuncCall != null){
            return assignmentOrFuncCall;
        }

        if (currentFunctionDefinition != null){  // as long as there is a function definition, we are allowed to parse the return statement
            Statement returnStmt = parseReturnStatement();
            if (returnStmt != null){
                return returnStmt;
            }
        }



        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parseAssignmentOrFunctionCallStatement() {
        if (tokens.match(IDENTIFIER)) {
            Token start = tokens.consumeToken();
            if (tokens.match(EQUAL)) {
                tokens.consumeToken();
                final AssignmentStatement assignmentStatement = new AssignmentStatement();
                assignmentStatement.setStart(start);
                assignmentStatement.setVariableName(start.getStringValue());
                assignmentStatement.setExpression(parseExpression());
                assignmentStatement.setEnd(tokens.lastToken());
                return assignmentStatement;
            } else if (tokens.match(LEFT_PAREN)) {
                FunctionCallStatement functionCallStatement = parseFunctionCallStatement(start);
                return functionCallStatement;


            }
        } return null;
    }

        /*if (tokens.match(IDENTIFIER)){
            Token id = tokens.consumeToken();
            if (tokens.match(LEFT_PAREN)){
                return parseFunctionCallStatement(id);
            } else{
                return parseAssignmentStatement(id);
            }

        }
        return null;*/


    private Statement parseAssignmentStatement(Token id){
        if (tokens.match(IDENTIFIER)){
            AssignmentStatement assignmentStatement = new AssignmentStatement();
            assignmentStatement.setStart(tokens.consumeToken());

            require(EQUAL, assignmentStatement);
            assignmentStatement.setExpression(parseExpression());

            return assignmentStatement;
        }
        else {
            return null;
        }

    }

    private FunctionCallStatement parseFunctionCallStatement(Token id){
        FunctionCallExpression e = parseFunctionCall(id);
        return new FunctionCallStatement(e);
    }

    private Statement parseForStatement(){
        if (tokens.match(FOR)){
            //off we go and parse the for loop
            ForStatement forStatement = new ForStatement();
            forStatement.setStart(tokens.consumeToken());

            // require (
            require(LEFT_PAREN, forStatement);

            // require an identifier
            Token name = require(IDENTIFIER, forStatement);
            forStatement.setVariableName(name.getStringValue());

            // require 'in' for (x in ...)
            require(IN, forStatement);
            forStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, forStatement);

            // require {
            LinkedList<Statement> statements = new LinkedList<>();
            require(LEFT_BRACE, forStatement);
            while (!tokens.match(RIGHT_BRACE) && tokens.hasMoreTokens()){
                //Statement statement = parseStatement();
                statements.add(parseStatement());

            }
            require(RIGHT_BRACE, forStatement);
            forStatement.setBody(statements);
            return forStatement;
            // forStatement.setVariableName(??);
            //forStatement.setExpression(parseExpression);

        }
        else{
            return null;
        }


    }


    // NEED HELP
    private Statement parseIfStatement(){
        if (tokens.match(IF)){
            IfStatement ifStatement = new IfStatement();
            ifStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, ifStatement);
            ifStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, ifStatement);

            LinkedList<Statement> statements = new LinkedList<>();
            require(LEFT_BRACE, ifStatement);
            while (tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACE)){
                statements.add(parseStatement());
            }
            ifStatement.setEnd(require(RIGHT_BRACE, ifStatement));

            LinkedList<Statement> elseStatements = new LinkedList<>();
            if (tokens.matchAndConsume(ELSE)){
                if (tokens.match(IF)){
                    Statement ifstatement = parseIfStatement();
                    elseStatements.add(ifstatement);
                    ifStatement.setEnd(ifstatement.getEnd());
                }
                else{
                    require(LEFT_BRACE, ifStatement);
                    while (tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACE)){
                        elseStatements.add(parseStatement());
                    }
                    ifStatement.setEnd(require(RIGHT_BRACE, ifStatement));
                }
            }

            ifStatement.setTrueStatements(statements);
            ifStatement.setElseStatements(elseStatements);


            return ifStatement;

        }

        else{
            return null;
        }
    }

    private Statement parseVarStatement(){
        if (tokens.match(VAR)){
            VariableStatement variableStatement = new VariableStatement();
            variableStatement.setStart(tokens.consumeToken());
            Token name = require(IDENTIFIER, variableStatement);
            variableStatement.setVariableName(name.getStringValue());
            TypeLiteral returnType = null;
            if (tokens.matchAndConsume(COLON)){
                //parse return type
                returnType = parseTypeLiteral();
                variableStatement.setExplicitType(returnType.getType());
            }
            //variableStatement.set;  How to set the type literal??
            require(EQUAL, variableStatement);
            variableStatement.setExpression(parseExpression());
            return variableStatement;

        }

        else{
            return null;
        }
    }
    private Statement parseReturnStatement() {
        if (tokens.match(RETURN)) {
            ReturnStatement returnStatement = new ReturnStatement();
            returnStatement.setStart(tokens.consumeToken());
            returnStatement.setFunctionDefinition(currentFunctionDefinition);


            if (!tokens.match(RIGHT_BRACE)) {
                // we need to parse the return statement
                returnStatement.setExpression(parseExpression());

            }
            return returnStatement;
        } else {
            return null;
        }

    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }


    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {

        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression(){
        Expression expression = parseComparisonExpression();
        while (tokens.match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseComparisonExpression();
            EqualityExpression equalityExpression = new EqualityExpression(operator, expression, rightHandSide);
            equalityExpression.setStart(expression.getStart());
            equalityExpression.setEnd(rightHandSide.getEnd());
            expression = equalityExpression;
        }
        return expression;
    }

    private Expression parseComparisonExpression(){
        Expression expression = parseAdditiveExpression();
        while (tokens.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseAdditiveExpression();
            ComparisonExpression comparisonExpression = new ComparisonExpression(operator, expression, rightHandSide);
            comparisonExpression.setStart(expression.getStart());
            comparisonExpression.setEnd(rightHandSide.getEnd());
            expression = comparisonExpression;
        }
        return expression;
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
        return expression;
    }

    private Expression parseFactorExpression(){
        Expression expression = parseUnaryExpression();
        while (tokens.match(SLASH, STAR)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rightHandSide);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(rightHandSide.getEnd());
            expression = factorExpression;
        }
        return expression;
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        } else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else if (tokens.match(STRING)){
            Token stringToken = tokens.consumeToken();
            StringLiteralExpression stringExpression = new StringLiteralExpression(stringToken.getStringValue());
            stringExpression.setToken(stringToken);
            return stringExpression;

        } else if (tokens.match(IDENTIFIER)){
            Token identifierToken = tokens.consumeToken();
            if (tokens.match(LEFT_PAREN)){
                //FunctionCallExpression functionCall = parseFunctionCall(identifierToken.getStringValue());
                return parseFunctionCall(identifierToken);

            } else{
                IdentifierExpression identifierExpression = new IdentifierExpression(identifierToken.getStringValue());
                identifierExpression.setToken(identifierToken);
                return identifierExpression;
            }


        } else if (tokens.match(TRUE)){
            Token trueToken = tokens.consumeToken();
            String trueString = trueToken.getStringValue();
            boolean trueValue = Boolean.parseBoolean(trueString);
            BooleanLiteralExpression trueExpression = new BooleanLiteralExpression(trueValue);
            trueExpression.setToken(trueToken);
            return trueExpression;

        } else if (tokens.match(FALSE)){
            Token falseToken = tokens.consumeToken();
            String falseString = falseToken.getStringValue();
            boolean falseValue = Boolean.parseBoolean(falseString);
            BooleanLiteralExpression falseExpression = new BooleanLiteralExpression(falseValue);
            falseExpression.setToken(falseToken);
            return falseExpression;

        } else if (tokens.match(LEFT_PAREN)){
            Token leftParen = tokens.consumeToken();
            final Expression lhs = parseExpression();
            ParenthesizedExpression paranthesizeExpression  = new ParenthesizedExpression(lhs);
            paranthesizeExpression.setStart(paranthesizeExpression.getStart());
            paranthesizeExpression.setEnd(lhs.getEnd());
            require(RIGHT_PAREN, paranthesizeExpression);
            return paranthesizeExpression;


        } else if (tokens.match(LEFT_BRACKET)){
            return parseListLiteralExpression();


        }else if (tokens.match(NULL)){
            Token nullToken = tokens.consumeToken();
            NullLiteralExpression nullExpression = new NullLiteralExpression();
            //stringExpression.setToken(stringToken);
            return nullExpression;
        }else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }

    private FunctionCallExpression parseFunctionCall(Token name){
        if (tokens.match(LEFT_PAREN)) {
            Token token = tokens.consumeToken();
            List<Expression> exprs = new LinkedList<>();
            if (!tokens.match(RIGHT_PAREN)) {
                do {
                    Expression expr = parseExpression();
                    exprs.add(expr);
                } while (tokens.matchAndConsume(COMMA));
            }
            FunctionCallExpression functionCallExpression = new FunctionCallExpression(name.getStringValue(), exprs);
            functionCallExpression.setStart(name);
            Token right = require(RIGHT_PAREN, functionCallExpression, ErrorType.UNTERMINATED_ARG_LIST);
            functionCallExpression.setEnd(right);
            return functionCallExpression;
        }  else {
            return null;
        }

    }
    private Expression parseListLiteralExpression(){
        if (tokens.match(LEFT_BRACKET)) {
            Token token = tokens.consumeToken();
            List<Expression> exprs = new LinkedList<>();
            if (!tokens.match(RIGHT_BRACKET)) {
                do {
                    Expression expr = parseExpression();
                    exprs.add(expr);
                } while (tokens.matchAndConsume(COMMA));
            }
            ListLiteralExpression listLiteralExpression = new ListLiteralExpression(exprs);
            require(RIGHT_BRACKET, listLiteralExpression, ErrorType.UNTERMINATED_LIST);
            return  listLiteralExpression;
        } else {
            return null;
        }
    }

    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}
