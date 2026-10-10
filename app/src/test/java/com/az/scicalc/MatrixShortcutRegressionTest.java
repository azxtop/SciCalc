package com.az.scicalc;
import org.junit.Test;
import static org.junit.Assert.*;
public class MatrixShortcutRegressionTest {
 private final MathEngine engine=new MathEngine();
 @Test public void diagonalAndLinearCommandsEvaluate(){assertEquals("{1,4}",engine.evaluate("Diagonal({{1,2},{3,4}})",false));assertEquals("{{1,0},{0,2}}",engine.evaluate("DiagonalMatrix({1,2})",false).replaceAll("\\s",""));assertEquals("{3,4}",engine.evaluate("LeastSquares({{1,0},{0,1}},{3,4})",false));assertEquals("{3,4}",engine.evaluate("LinearSolve({{1,0},{0,1}},{3,4})",false));}
 @Test public void decompositionCommandsReturnUsableResults(){assertEquals("1",engine.evaluate("Length(NullSpace({{1,1},{1,1}}))",false));assertEquals(1,engine.value("Part(PseudoInverse({{1,0},{0,1}}),1,1)"),1e-12);assertEquals("3",engine.evaluate("Length(LUDecomposition({{1,0},{0,1}}))",false));assertEquals("3",engine.evaluate("Length(SingularValueDecomposition({{1,0},{0,1}}))",false));}
}
