package com.gasmonsoft.fuelboxcontrol.domain.calibracion

import android.content.Context
import android.net.Uri
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import javax.inject.Inject

class ExcelCalibrationReader @Inject constructor() {

    operator fun invoke(context: Context, uri: Uri): List<Pair<String, String>> {
        val tempFile = copyUriToCacheFile(context, uri)

        return try {
            WorkbookFactory.create(tempFile).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                val formatter = DataFormatter()
                val evaluator = workbook.creationHelper.createFormulaEvaluator()

                buildList {
                    for (rowIndex in sheet.firstRowNum..sheet.lastRowNum) {
                        val row = sheet.getRow(rowIndex) ?: continue

                        val medida = formatter
                            .formatCellValue(row.getCell(0), evaluator)
                            .trim()

                        val litros = formatter
                            .formatCellValue(row.getCell(1), evaluator)
                            .trim()

                        if (medida.isBlank() && litros.isBlank()) continue

                        if (
                            rowIndex == sheet.firstRowNum &&
                            medida.equals("Medidas", ignoreCase = true) &&
                            litros.equals("Litros", ignoreCase = true)
                        ) {
                            continue
                        }

                        if (medida.isNotBlank() && litros.isNotBlank()) {
                            add(medida to litros)
                        }
                    }
                }
            }
        } finally {
            tempFile.delete()
        }
    }

    private fun copyUriToCacheFile(context: Context, uri: Uri): File {
        val tempFile = File.createTempFile("calibration_", ".tmp", context.cacheDir)

        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("No se pudo abrir el archivo seleccionado")

        return tempFile
    }
}