import java.util.List;

public abstract class Stmt {
	interface Visitor<R> {
		R visitBlockStmt(Block stmt);

		R visitExpressionStmt(Expression stmt);

		R visitIfStmt(If stmt);

		R visitVarStmt(Var stmt);

		R visitPrintStmt(Print stmt);

		R visitWhileStmt(While stmt);
	}

	public static class Block extends Stmt {
		Block(List<Stmt> statements) {
			this.statements = statements;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBlockStmt(this);
		}

		final List<Stmt> statements;
	}

	public static class Expression extends Stmt {
		Expression(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}

		final Expr expression;
	}

	public static class If extends Stmt {
		If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitIfStmt(this);
		}

		final Expr condition;
		final Stmt thenBranch;
		final Stmt elseBranch;
	}

	public static class Var extends Stmt {
		Var(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVarStmt(this);
		}

		final Token name;
		final Expr initializer;
	}

	public static class Print extends Stmt {
		Print(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintStmt(this);
		}

		final Expr expression;
	}

	public static class While extends Stmt {
		While(Expr condition, Stmt body) {
			this.condition = condition;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitWhileStmt(this);
		}

		final Expr condition;
		final Stmt body;
	}

	abstract <R> R accept(Visitor<R> visitor);
}
