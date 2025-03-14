/*
 * CFG for our parser
 * ---------------------------------------------------------
 * expression	-> equality ;
 * equality	-> comparison (("!=" | "==") comparison)* ;
 * comparison	-> term ((">" | ">=" | "<" | "<=") term)* ;
 * term		-> factor (("-" | "+") factor)* ;
 * factor	-> unary (("/" | "*") unary)* ;
 * unary	-> ("!" | "-") unary | primary ;
 * primary	-> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
 */

import java.util.List;
import java.util.ArrayList;

public class Parser {
	private static class ParseError extends RuntimeException {
	}

	private final List<Token> tokens;
	private int current = 0;

	public Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>();
		while (!isAtEnd()) {
			statements.add(statement());
		}
		return statements;
	}

	// expression -> equality
	private Expr expression() {
		return equality();
	}

	private Stmt statement() {
		if (match(TokenType.PRINT)) {
			return printStatement();
		}
		return expressionStatement();
	}

	private Stmt printStatement() {
		Expr value = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after value.");
		return new Stmt.Print(value);
	}

	private Stmt expressionStatement() {
		Expr expr = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after value.");
		return new Stmt.Expression(expr);
	}

	// equality -> comparison (("!=" | "==") comparison)*
	private Expr equality() {
		Expr expr = comparison();
		while (match(TokenType.NOT_EQUAL, TokenType.EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	// comparison -> term ((">" | ">=" | "<" | "<=") term)*
	private Expr comparison() {
		Expr expr = term();
		while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	// term -> factor (("-" | "+") factor)*
	private Expr term() {
		Expr expr = factor();
		while (match(TokenType.MINUS, TokenType.PLUS)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	// factor -> unary (("/" | "*") unary)*
	private Expr factor() {
		Expr expr = unary();
		while (match(TokenType.SLASH, TokenType.STAR)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	// unary -> ("!" | "-") unary | primary
	private Expr unary() {
		if (match(TokenType.NOT, TokenType.MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}
		return primary();
	}

	// primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
	private Expr primary() {
		if (match(TokenType.FALSE)) {
			return new Expr.Literal(false);
		}
		if (match(TokenType.TRUE)) {
			return new Expr.Literal(true);
		}
		if (match(TokenType.NIL)) {
			return new Expr.Literal(null);
		}
		if (match(TokenType.NUMBER, TokenType.STRING)) {
			return new Expr.Literal(previous().literal);
		}
		if (match(TokenType.LEFT_PAREN)) {
			Expr expr = expression();
			consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}
		throw error(peek(), "Expect expression.");
	}

	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}
		return false;
	}

	private Token consume(TokenType type, String message) {
		if (check(type)) {
			return advance();
		}
		throw error(peek(), message);
	}

	private boolean check(TokenType type) {
		if (isAtEnd()) {
			return false;
		}
		return peek().type == type;
	}

	private Token advance() {
		if (!isAtEnd()) {
			current++;
		}
		return previous();
	}

	private boolean isAtEnd() {
		return peek().type == TokenType.EOF;
	}

	private Token peek() {
		return tokens.get(current);
	}

	private Token previous() {
		return tokens.get(current - 1);
	}

	private ParseError error(Token token, String message) {
		Lox.error(token, message);
		return new ParseError();
	}

	private void synchronize() {
		advance();
		while (!isAtEnd()) {
			if (previous().type == TokenType.SEMICOLON) {
				return;
			}
			switch (peek().type) {
				case TokenType.CLASS:
				case TokenType.FOR:
				case TokenType.FUN:
				case TokenType.IF:
				case TokenType.PRINT:
				case TokenType.RETURN:
				case TokenType.VAR:
				case TokenType.WHILE:
					return;
			}
			advance();
		}
	}

}
