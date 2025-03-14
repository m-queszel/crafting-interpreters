import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
	private static final Interpreter interpreter = new Interpreter();
	public static boolean hadError = false;
	public static boolean hadRuntimeError = false;

	public static void main(String[] args) throws IOException {
		if (args.length > 1) {
			System.out.println("Usage: jlox [script]");
			System.exit(64);
		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
	}

	private static void runFile(String filePath) throws IOException {
		// Reads all bytes from the file at filePath and puts it into a byte array
		byte[] bytes = Files.readAllBytes(Paths.get(filePath));
		// Converts byte array into a string using the default charset, then calls run
		run(new String(bytes, Charset.defaultCharset()));
		if (hadError) {
			System.exit(65);
		}
		if (hadRuntimeError) {
			System.exit(70);
		}
	}

	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
		for (;;) {
			System.out.print("> ");
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			run(line);
			hadError = false;
		}
	}

	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens);
		Expr expression = parser.parse();
		if (hadError) {
			return;
		}
		interpreter.interpret(expression);
	}

	private static void report(int line, String where, String msg) {
		System.err.println("[line " + line + "] Error" + where + ": " + msg);
		hadError = true;
	}

	public static void error(int line, String msg) {
		report(line, "", msg);
	}

	public static void runtimeError(RuntimeError error) {
		System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
		hadRuntimeError = true;
	}

	public static void error(Token token, String message) {
		if (token.type == TokenType.EOF) {
			report(token.line, " at end", message);
		} else {
			report(token.line, " at '" + token.lexeme + "'", message);
		}
	}
}
