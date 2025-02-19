public class Token {
	final TokenType type;
	final String lexeme;
	final Object literal;
	final int line;

	public Token(TokenType typeIn, String lexemeIn, Object literalIn, int lineIn) {
		this.type = typeIn;
		this.lexeme = lexemeIn;
		this.literal = literalIn;
		this.line = lineIn;
	}

	@Override
	public String toString() {
		return type + " " + lexeme + " " + literal;
	}
}
