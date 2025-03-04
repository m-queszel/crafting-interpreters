import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: generate_ast <output directory>");
			System.exit(64);
		}
		String outputDir = args[0];
		try {
			defineAst(outputDir, "Expr", Arrays.asList(
					"Binary	: Expr left, Token operator, Expr right",
					"Grouping	: Expr expression",
					"Literal	: Object value",
					"Unary	: Token operator, Expr right"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void defineAst(String outputDirIn, String baseNameIn, List<String> typesIn) throws IOException {
		String path = outputDirIn + "/" + baseNameIn + ".java";
		PrintWriter writer = new PrintWriter(path, "UTF-8");
		writer.println("import java.util.List;");
		writer.println();
		writer.println("public abstract class " + baseNameIn + " {");
		for (String type : typesIn) {
			String className = type.split(":")[0].trim();
			String fields = type.split(":")[1].trim();
			defineType(writer, baseNameIn, className, fields);
		}
		writer.println("}");
		writer.close();
	}

	public static void defineType(PrintWriter writerIn, String baseNameIn, String classNameIn, String fieldListIn) {
		writerIn.println(" public static class " + classNameIn + " extends " + baseNameIn + " {");
		writerIn.println("	" + classNameIn + "(" + fieldListIn + ") {");
		String[] fields = fieldListIn.split(", ");
		for (String field : fields) {
			String name = field.split(" ")[1];
			writerIn.println("	this." + name + " = " + name + ";");
		}
		writerIn.println("	}");

		// Fields
		writerIn.println();
		for (String field : fields) {
			writerIn.println("	final " + field + ";");
		}
		writerIn.println("	}");
	}
}
