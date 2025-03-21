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
import java.util.Arrays;
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
			statements.add(declaration());
		}
		return statements;
	}

	// expression -> equality
	private Expr expression() {
		return assignment();
	}

	private Stmt declaration() {
		try {
			if (match(TokenType.VAR)) {
				return varDeclaration();
			}
			return statement();
		} catch (ParseError error) {
			synchronize();
			return null;
		}
	}

	// statement -> exprStmt | forStmt | ifStmt | printStmt | whileStmt | block ;
	private Stmt statement() {
		if (match(TokenType.FOR)) {
			return forStatement();
		}
		if (match(TokenType.IF)) {
			return ifStatement();
		}
		if (match(TokenType.PRINT)) {
			return printStatement();
		}
		if (match(TokenType.WHILE)) {
			return whileStatement();
		}
		if (match(TokenType.LEFT_BRACE)) {
			return new Stmt.Block(block());
		}
		return expressionStatement();
	}

	// forStmt -> "for" "(" ( varDecl | exprStmt | ";") expression? ";" expression?
	// ")" statement ;
	private Stmt forStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");
		Stmt initializer;
		if (match(TokenType.SEMICOLON)) {
			initializer = null;
		} else if (match(TokenType.VAR)) {
			initializer = varDeclaration();
		} else {
			initializer = expressionStatement();
		}

		Expr condition = null;
		if (!check(TokenType.SEMICOLON)) {
			condition = expression();
		}
		consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

		Expr increment = null;
		if (!check(TokenType.RIGHT_PAREN)) {
			increment = expression();
		}
		consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");
		Stmt body = statement();

		if (increment != null) {
			body = new Stmt.Block(
					Arrays.asList(
							body,
							new Stmt.Expression(increment)));
		}

		if (condition == null) {
			condition = new Expr.Literal(true);
		}

		body = new Stmt.While(condition, body);

		if (initializer != null) {
			body = new Stmt.Block(Arrays.asList(initializer, body));
		}

		return body;
	}

	// ifStmt -> "if" "(" expression ")" stateet ( "else" statement )? ;
	private Stmt ifStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

		Stmt thenBranch = statement();
		Stmt elseBranch = null;
		if (match(TokenType.ELSE)) {
			elseBranch = statement();
		}
		return new Stmt.If(condition, thenBranch, elseBranch);
	}

	private Stmt printStatement() {
		Expr value = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after value.");
		return new Stmt.Print(value);
	}

	private Stmt varDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

		Expr initializer = null;
		if (match(TokenType.EQUAL)) {
			initializer = expression();
		}
		consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
		return new Stmt.Var(name, initializer);
	}

	// whileStmt -> "while" "(" expression ")" statement;
	private Stmt whileStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");
		Stmt body = statement();
		return new Stmt.While(condition, body);
	}

	private Stmt expressionStatement() {
		Expr expr = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after value.");
		return new Stmt.Expression(expr);
	}

	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<>();
		while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}
		consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
		return statements;
	}

	// assignment -> IDENTIFIER "=" assignment | logic_or ;
	private Expr assignment() {
		Expr expr = or();

		if (match(TokenType.EQUAL)) {
			Token equals = previous();
			Expr value = assignment();
			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable) expr).name;
				return new Expr.Assign(name, value);
			}
			error(equals, "Invalid assignment target.");
		}
		return expr;
	}

	// logic_or -> logic_and ( "or" logic_and )*;
	private Expr or() {
		Expr expr = and();
		while (match(TokenType.OR)) {
			Token operator = previous();
			Expr right = and();
			expr = new Expr.Logical(expr, operator, right);
		}
		return expr;
	}

	// logic_and -> equality ( "and" equality )* ;
	private Expr and() {
		Expr expr = equality();
		while (match(TokenType.AND)) {
			Token operator = previous();
			Expr right = equality();
			expr = new Expr.Logical(expr, operator, right);
		}
		return expr;
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
		if (match(TokenType.IDENTIFIER)) {
			return new Expr.Variable(previous());
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
