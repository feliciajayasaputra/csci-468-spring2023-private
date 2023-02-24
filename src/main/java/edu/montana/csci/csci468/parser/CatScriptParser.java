package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.util.LinkedList;
import java.util.List;

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

    private Statement parseProgramStatement() {
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
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
                return parseFunctionCall(identifierToken.getStringValue());

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

    private Expression parseFunctionCall(String name){
        if (tokens.match(LEFT_PAREN)) {
            Token token = tokens.consumeToken();
            List<Expression> exprs = new LinkedList<>();
            if (!tokens.match(RIGHT_PAREN)) {
                do {
                    Expression expr = parseExpression();
                    exprs.add(expr);
                } while (tokens.matchAndConsume(COMMA));
            }
            FunctionCallExpression functionCallExpression = new FunctionCallExpression(name, exprs);
            require(RIGHT_PAREN, functionCallExpression, ErrorType.UNTERMINATED_ARG_LIST);
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
