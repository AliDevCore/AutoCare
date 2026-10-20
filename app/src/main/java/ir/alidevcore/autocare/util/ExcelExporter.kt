package ir.alidevcore.autocare.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import ir.alidevcore.autocare.data.model.MaintenanceItem
import ir.alidevcore.autocare.data.model.MaintenanceRecord
import ir.alidevcore.autocare.data.model.PlannedService
import ir.alidevcore.autocare.data.model.Vehicle
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object ExcelExporter {

    data class VehicleExportData(
        val vehicle: Vehicle,
        val items: List<MaintenanceItem>,
        val records: List<Pair<MaintenanceRecord, String>>, // record + itemName
        val plans: List<Pair<PlannedService, String>>       // plan + itemName
    )

    /**
     * Exports a single vehicle to an XLSX workbook.
     */
    fun exportVehicleToExcel(
        context: Context,
        data: VehicleExportData
    ): File {
        val workbook = XSSFWorkbook()

        val headerStyle = createHeaderStyle(workbook)
        val dataStyle = createDataStyle(workbook)
        val boldStyle = createBoldStyle(workbook)

        // 1. Vehicle info sheet
        val infoSheet = workbook.createSheet("مشخصات خودرو")
        infoSheet.isRightToLeft = true
        createVehicleInfoSheet(infoSheet, data.vehicle, headerStyle, dataStyle, boldStyle)

        // 2. Records sheet
        val recordsSheet = workbook.createSheet("سوابق تعمیرات و سرویس‌ها")
        recordsSheet.isRightToLeft = true
        createRecordsSheet(recordsSheet, data.records, headerStyle, dataStyle)

        // 3. Planned services sheet
        val plansSheet = workbook.createSheet("برنامه‌های آینده")
        plansSheet.isRightToLeft = true
        createPlansSheet(plansSheet, data.plans, headerStyle, dataStyle)

        // 4. Maintenance items sheet
        val itemsSheet = workbook.createSheet("اقلام و فواصل سرویس")
        itemsSheet.isRightToLeft = true
        createItemsSheet(itemsSheet, data.items, headerStyle, dataStyle)

        // Generate file name with Persian date
        val (year, month, day) = JalaliDateUtil.getCurrentJalaliDate()
        val dateStr = String.format("%04d%02d%02d", year, month, day)
        val safeVehicleName = data.vehicle.name.replace("[^a-zA-Z0-9آ-ی]".toRegex(), "_")
        val fileName = "AutoCare_${safeVehicleName}_$dateStr.xlsx"

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        FileOutputStream(file).use { out ->
            workbook.write(out)
        }
        workbook.close()

        return file
    }

    /**
     * Exports multiple vehicles to a single multi-sheet XLSX file.
     * Contains summary sheet, and for each vehicle: records sheet, planned services sheet, and maintenance items sheet.
     */
    fun exportAllVehiclesToExcel(
        context: Context,
        allData: List<VehicleExportData>
    ): File {
        val workbook = XSSFWorkbook()

        val headerStyle = createHeaderStyle(workbook)
        val dataStyle = createDataStyle(workbook)

        // 1. Summary sheet for all vehicles
        val summarySheet = workbook.createSheet("خلاصه خودروها")
        summarySheet.isRightToLeft = true
        val summaryHeaders = listOf("شناسه", "نام خودرو", "کیلومتر فعلی", "سال ساخت", "تعداد اقلام", "تعداد سوابق", "تعداد برنامه‌ها")
        val summaryHeaderRow = summarySheet.createRow(0)
        summaryHeaders.forEachIndexed { i, h ->
            val cell = summaryHeaderRow.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        var rowNum = 1
        allData.forEach { vd ->
            val row = summarySheet.createRow(rowNum++)
            row.createCell(0).setCellValue(vd.vehicle.id.toDouble())
            row.createCell(1).setCellValue(vd.vehicle.name)
            row.createCell(2).setCellValue(vd.vehicle.currentKm.toDouble())
            row.createCell(3).setCellValue(vd.vehicle.year?.toString() ?: "-")
            row.createCell(4).setCellValue(vd.items.size.toDouble())
            row.createCell(5).setCellValue(vd.records.size.toDouble())
            row.createCell(6).setCellValue(vd.plans.size.toDouble())
            for (c in 0..6) row.getCell(c).cellStyle = dataStyle
        }
        for (i in summaryHeaders.indices) summarySheet.autoSizeColumn(i)

        // 2. For each vehicle, create dedicated sheets for records, plans, and items
        allData.forEachIndexed { idx, vd ->
            val prefix = "${idx + 1}"
            val safeName = vd.vehicle.name.replace("[^a-zA-Z0-9آ-ی]".toRegex(), "_").take(10)

            // Vehicle Records Sheet
            val recSheetName = "${prefix}_سوابق_$safeName".take(31)
            val recordsSheet = workbook.createSheet(recSheetName)
            recordsSheet.isRightToLeft = true
            createRecordsSheet(recordsSheet, vd.records, headerStyle, dataStyle)

            // Vehicle Planned Services Sheet
            val planSheetName = "${prefix}_برنامه_$safeName".take(31)
            val plansSheet = workbook.createSheet(planSheetName)
            plansSheet.isRightToLeft = true
            createPlansSheet(plansSheet, vd.plans, headerStyle, dataStyle)

            // Vehicle Maintenance Items Sheet
            val itemSheetName = "${prefix}_اقلام_$safeName".take(31)
            val itemsSheet = workbook.createSheet(itemSheetName)
            itemsSheet.isRightToLeft = true
            createItemsSheet(itemsSheet, vd.items, headerStyle, dataStyle)
        }

        val (year, month, day) = JalaliDateUtil.getCurrentJalaliDate()
        val dateStr = String.format("%04d%02d%02d", year, month, day)
        val fileName = "AutoCare_AllVehicles_$dateStr.xlsx"

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        FileOutputStream(file).use { out ->
            workbook.write(out)
        }
        workbook.close()

        return file
    }

    /**
     * Escapes a value for safe CSV export conforming to RFC 4180
     * and prevents spreadsheet formula injection (=, +, -, @).
     */
    private fun escapeCsvCell(raw: String): String {
        var s = raw
        // Prevent spreadsheet formula injection (=, +, -, @)
        if (s.startsWith("=") || s.startsWith("+") || s.startsWith("-") || s.startsWith("@")) {
            s = "'$s"
        }
        // Escape internal double quotes by doubling them, and wrap the whole cell in quotes
        return "\"${s.replace("\"", "\"\"")}\""
    }

    /**
     * Exports a vehicle to CSV as a lightweight alternative
     */
    fun exportVehicleToCsv(
        context: Context,
        data: VehicleExportData
    ): File {
        val (year, month, day) = JalaliDateUtil.getCurrentJalaliDate()
        val dateStr = String.format("%04d%02d%02d", year, month, day)
        val safeVehicleName = data.vehicle.name.replace("[^a-zA-Z0-9آ-ی]".toRegex(), "_")
        val fileName = "AutoCare_${safeVehicleName}_$dateStr.csv"

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM so Excel opens Persian text properly
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write("نام خودرو,${escapeCsvCell(data.vehicle.name)}\n")
                writer.write("کیلومتر فعلی,${data.vehicle.currentKm}\n")
                writer.write("سال ساخت,${data.vehicle.year ?: "-"}\n\n")

                writer.write("ردیف,نام قطعه/سرویس,تاریخ شمسی,کیلومتر,هزینه (تومان),توضیحات,قطعات مصرفی,تعمیرگاه\n")
                data.records.forEachIndexed { index, (record, itemName) ->
                    val shamsiDate = record.date?.let { JalaliDateUtil.timestampToJalaliString(it) } ?: "نامشخص"
                    val cost = record.cost?.toString() ?: ""
                    writer.write(
                        "${index + 1}," +
                        "${escapeCsvCell(itemName)}," +
                        "$shamsiDate," +
                        "${record.km}," +
                        "$cost," +
                        "${escapeCsvCell(record.description)}," +
                        "${escapeCsvCell(record.parts)}," +
                        "${escapeCsvCell(record.workshop)}\n"
                    )
                }
            }
        }
        return file
    }

    /**
     * Launches the system share sheet for the exported file
     */
    fun shareExportedFile(context: Context, file: File, mimeType: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری گزارش خودرو"))
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private fun createVehicleInfoSheet(
        sheet: Sheet,
        vehicle: Vehicle,
        headerStyle: XSSFCellStyle,
        dataStyle: XSSFCellStyle,
        boldStyle: XSSFCellStyle
    ) {
        val titleRow = sheet.createRow(0)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("مشخصات خودرو")
        titleCell.cellStyle = boldStyle

        val fields = listOf(
            "شناسه خودرو" to vehicle.id.toString(),
            "نام خودرو" to vehicle.name,
            "کیلومتر فعلی" to NumberUtil.formatKm(vehicle.currentKm),
            "سال ساخت" to (vehicle.year?.toString() ?: "ثبت نشده"),
            "تاریخ ایجاد پرونده" to JalaliDateUtil.timestampToJalaliString(vehicle.createdAt)
        )

        fields.forEachIndexed { index, (label, value) ->
            val row = sheet.createRow(index + 2)
            val c0 = row.createCell(0)
            c0.setCellValue(label)
            c0.cellStyle = boldStyle

            val c1 = row.createCell(1)
            c1.setCellValue(value)
            c1.cellStyle = dataStyle
        }

        sheet.autoSizeColumn(0)
        sheet.autoSizeColumn(1)
    }

    private fun createRecordsSheet(
        sheet: Sheet,
        records: List<Pair<MaintenanceRecord, String>>,
        headerStyle: XSSFCellStyle,
        dataStyle: XSSFCellStyle
    ) {
        val headers = listOf("ردیف", "نام سرویس / قطعه", "تاریخ انجام (شمسی)", "کیلومتر", "هزینه (تومان)", "توضیحات", "قطعات و مواد مصرفی", "تعمیرگاه")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        records.forEachIndexed { index, (record, itemName) ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue((index + 1).toDouble())
            row.createCell(1).setCellValue(itemName)
            row.createCell(2).setCellValue(record.date?.let { JalaliDateUtil.timestampToJalaliString(it) } ?: "نامشخص")
            row.createCell(3).setCellValue(record.km.toDouble())
            row.createCell(4).setCellValue(record.cost?.toDouble() ?: 0.0)
            row.createCell(5).setCellValue(record.description)
            row.createCell(6).setCellValue(record.parts)
            row.createCell(7).setCellValue(record.workshop)
            for (c in 0..7) row.getCell(c).cellStyle = dataStyle
        }

        for (i in headers.indices) sheet.autoSizeColumn(i)
    }

    private fun createPlansSheet(
        sheet: Sheet,
        plans: List<Pair<PlannedService, String>>,
        headerStyle: XSSFCellStyle,
        dataStyle: XSSFCellStyle
    ) {
        val headers = listOf("ردیف", "نام سرویس", "کیلومتر هدف", "تاریخ هدف (شمسی)", "وضعیت", "توضیحات")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        plans.forEachIndexed { index, (plan, itemName) ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue((index + 1).toDouble())
            row.createCell(1).setCellValue(itemName)
            row.createCell(2).setCellValue(plan.targetKm?.toDouble() ?: 0.0)
            row.createCell(3).setCellValue(plan.targetDate?.let { JalaliDateUtil.timestampToJalaliString(it) } ?: "-")
            row.createCell(4).setCellValue(when (plan.status.name) {
                "PENDING" -> "در انتظار انجام"
                "DONE" -> "انجام شده"
                "CANCELLED" -> "لغو شده"
                else -> plan.status.name
            })
            row.createCell(5).setCellValue(plan.notes)
            for (c in 0..5) row.getCell(c).cellStyle = dataStyle
        }

        for (i in headers.indices) sheet.autoSizeColumn(i)
    }

    private fun createItemsSheet(
        sheet: Sheet,
        items: List<MaintenanceItem>,
        headerStyle: XSSFCellStyle,
        dataStyle: XSSFCellStyle
    ) {
        val headers = listOf("ردیف", "نام قطعه / سرویس", "دسته‌بندی", "فاصله کیلومتری", "فاصله زمانی (ماه)", "وضعیت", "توضیحات")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        items.forEachIndexed { index, item ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue((index + 1).toDouble())
            row.createCell(1).setCellValue(item.name)
            row.createCell(2).setCellValue(item.category)
            row.createCell(3).setCellValue(item.intervalKm?.toDouble() ?: 0.0)
            row.createCell(4).setCellValue(item.intervalMonths?.toDouble() ?: 0.0)
            row.createCell(5).setCellValue(if (item.isActive) "فعال" else "غیرفعال")
            row.createCell(6).setCellValue(item.description)
            for (c in 0..6) row.getCell(c).cellStyle = dataStyle
        }

        for (i in headers.indices) sheet.autoSizeColumn(i)
    }

    private fun createHeaderStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont().apply {
            bold = true
            color = IndexedColors.WHITE.index
        }
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.DARK_BLUE.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER
        setBorders(style)
        return style
    }

    private fun createDataStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val style = workbook.createCellStyle()
        style.alignment = HorizontalAlignment.RIGHT
        style.verticalAlignment = VerticalAlignment.CENTER
        setBorders(style)
        return style
    }

    private fun createBoldStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont().apply {
            bold = true
        }
        style.setFont(font)
        style.alignment = HorizontalAlignment.RIGHT
        return style
    }

    private fun setBorders(style: XSSFCellStyle) {
        style.borderTop = BorderStyle.THIN
        style.borderBottom = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
    }
}
