public class Interpreter implements Expr.Visitor<Object> {
	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		return expr.value;
	}

	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		Object right = evaluate(expr.right);
		switch (expr.operator.type) {
			case TokenType.NOT:
				return !isTruthy(right);
			case TokenType.MINUS:
				checkNumberOperand(expr.operator, right);
				return -(double) right;
		}
		return null;
	}

	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double) {
			return;
		}
		throw new RuntimeError(operator, "Operand must be a number.");
	}

	private void checkNumberOperands(Token operator, Object left, Object right) {
		if (left instanceof Double && right instanceof Double) {
			return;
		}
		throw new RuntimeError(operator, "Operands must be numbers.");
	}

	// Follows Ruby's rule: false and nil -> falsey; everything else -> truthy.
	private boolean isTruthy(Object object) {
		if (object == null) {
			return false;
		}
		if (object instanceof Boolean) {
			return (boolean) object;
		}
		return true;
	}

	private boolean isEqual(Object obj1, Object obj2) {
		if (obj1 == null && obj2 == null) {
			return true;
		}
		if (obj1 == null) {
			return false;
		}
		return obj1.equals(obj2);
	}

	private String stringify(Object object) {
		if (object == null) {
			return "nil";
		}
		if (object instanceof Double) {
			String text = object.toString();
			if (text.endsWith(".0")) {
				text = text.substring(0, text.length() - 2);
			}
			return text;
		}
		return object.toString();
	}

	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
	}

	public Object evaluate(Expr expr) {
		return expr.accept(this);
	}

	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);
		switch (expr.operator.type) {
			case TokenType.GREATER:
				checkNumberOperands(expr.operator, left, right);
				return (double) left > (double) right;
			case TokenType.GREATER_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double) left >= (double) right;
			case TokenType.LESS:
				checkNumberOperands(expr.operator, left, right);
				return (double) left < (double) right;
			case TokenType.LESS_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double) left <= (double) right;
			case TokenType.MINUS:
				checkNumberOperands(expr.operator, left, right);
				return (double) left - (double) right;
			case TokenType.NOT_EQUAL:
				return !isEqual(left, right);
			case TokenType.EQUAL_EQUAL:
				return isEqual(left, right);
			// handle concatenation or arithmetic addition.
			case TokenType.PLUS:
				if (left instanceof Double && right instanceof Double) {
					return (double) left + (double) right;
				}
				if (left instanceof String && right instanceof String) {
					return (String) left + (String) right;
				}
				if ((left instanceof String && right instanceof Double)
						|| (left instanceof Double && right instanceof String)) {
					return stringify(left) + stringify(right);
				}
				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
			case TokenType.SLASH:
				checkNumberOperands(expr.operator, left, right);
				if ((double) right == 0) {
					throw new RuntimeError(expr.operator, "Cannot divide by zero.");
				}
				return (double) left / (double) right;
			case TokenType.STAR:
				checkNumberOperands(expr.operator, left, right);
				return (double) left * (double) right;
		}
		return null;
	}

	public void interpret(Expr expression) {
		try {
			Object value = evaluate(expression);
			System.out.println(stringify(value));
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
	}

}
