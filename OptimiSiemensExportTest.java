package com.vsoft.siemens;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsoft.optimi.model.LogCutTask;
import com.vsoft.optimi.model.solution.LogSolution;
import com.vsoft.siemens.model.Optimi3DSiemensExportData;
import lombok.val;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Sergii Volchenko
 */
public class OptimiSiemensExportTest {

  private static final double[] beforeX = {0, 54.583,-40,41,93.886};
  private static final double[] beforeY = {0, 89,47,47,47, 9999};
  private static final double[] afterX = {0, 54.583,-40,41,93.886,-80.5,0.5,81.5,103.378,-60,61,103.378};
  private static final double[] afterY = {0, 89,47,47,47,-22,20,20,20,-22,-22,-22};

  private static final double[] noTurnoverX = {0, 66.08,-60,61,97.58,-60,-86,-86,-60,61,98.38,61,87,95.836};
  private static final double[] noTurnoverY = {0, 81,39,39,39,37,-3,-45,-45,-3,37,-45,-45,-45};

  public Optimi3DSiemensExportData prepareConvertData(String taskFileName, String solutionFileName) throws Exception{
    ObjectMapper mapper = new ObjectMapper();
    String taskStr = readRes(taskFileName);
    String solutionStr = readRes(solutionFileName);
    LogCutTask task = mapper.readValue(taskStr, LogCutTask.class);
    LogSolution solution = mapper.readValue(solutionStr, LogSolution.class);
    Optimi3DSiemensTransformer transformer = new Optimi3DSiemensTransformer();
    return transformer.transform(task, solution);
  }

  @Test
  public void testExport() throws Exception {
    Optimi3DSiemensExportData result = prepareConvertData("task.json", "solution.json");
    assertArrayEquals(beforeX, result.getBeforeTurnoverCutsX(), 0.01);
    assertArrayEquals(beforeY, result.getBeforeTurnoverCutsY(), 0.01);
    assertArrayEquals(afterX, result.getAfterTurnoverCutsX(), 0.01);
    assertArrayEquals(afterY, result.getAfterTurnoverCutsY(), 0.01);
  }

  @Test
  public void testExportWithTransformation() throws Exception {
    Optimi3DSiemensExportData result = prepareConvertData("taskTransformed.json", "solutionTransformed.json");
    assertArrayEquals(beforeX, result.getBeforeTurnoverCutsX(), 0.01);
    assertArrayEquals(beforeY, result.getBeforeTurnoverCutsY(), 0.01);
    assertArrayEquals(afterX, result.getAfterTurnoverCutsX(), 0.01);
    assertArrayEquals(afterY, result.getAfterTurnoverCutsY(), 0.01);
  }

  @Test
  public void testExportWithNoTurnover() throws Exception {
    Optimi3DSiemensExportData result = prepareConvertData("taskNoTurnover.json", "solutionNoTurnover.json");

    assertArrayEquals(noTurnoverX, result.getBeforeTurnoverCutsX(), 0.01);
    assertArrayEquals(noTurnoverY, result.getBeforeTurnoverCutsY(), 0.01);
    assert(result.getAfterTurnoverCutsX().length == 0);
    assert(result.getAfterTurnoverCutsY().length == 0);
  }

  @Test
  public void test628() throws Exception {
    Optimi3DSiemensExportData result = prepareConvertData("task_628_smrek11(1).json", "solution_628_smrek11(1).json");
    val data = new ResultSiemensData("dataplc_smrek11(1).txt.json");

    assertArrayEquals(data.getBeforeX(), result.getBeforeTurnoverCutsX(), 0.06);
    assertArrayEquals(data.getBeforeY(), result.getBeforeTurnoverCutsY(), 0.06);
    assertArrayEquals(data.getAfterX(), result.getAfterTurnoverCutsX(), 0.06);
    assertArrayEquals(data.getAfterY(), result.getAfterTurnoverCutsY(), 0.06);
  }

  private String readRes(String fileName) throws Exception {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    try (InputStream is = classLoader.getResourceAsStream(fileName)) {
      if (Objects.isNull(is))
        return null;
      try (InputStreamReader isr = new InputStreamReader(is);
          BufferedReader reader = new BufferedReader(isr)) {
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
      }
    }
  }

}
