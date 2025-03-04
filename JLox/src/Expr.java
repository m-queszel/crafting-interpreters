public abstract class Expr {
	public static class Binary extends Expr {
		Binary(Expr leftIn, Token operatorIn, Expr rightIn) {
			this.left = leftIn;
			this.operator = operatorIn;
			this.right = rightIn;
		}

		final Expr left;
		final Token operator;
		final Expr right;
	}
}
