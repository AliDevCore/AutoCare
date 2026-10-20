package ir.alidevcore.autocare.ui.navigation

sealed class Screen(val route: String) {
    object Vehicles : Screen("vehicles")
    object Dashboard : Screen("dashboard/{vehicleId}") {
        fun createRoute(vehicleId: Long) = "dashboard/$vehicleId"
    }
    object Items : Screen("items/{vehicleId}") {
        fun createRoute(vehicleId: Long) = "items/$vehicleId"
    }
    object AddRecord : Screen("add_record/{vehicleId}?itemId={itemId}&recordId={recordId}&planId={planId}") {
        fun createRoute(vehicleId: Long, itemId: Long? = null, recordId: Long? = null, planId: Long? = null): String {
            val q = mutableListOf<String>()
            if (itemId != null) q.add("itemId=$itemId")
            if (recordId != null) q.add("recordId=$recordId")
            if (planId != null) q.add("planId=$planId")
            return "add_record/$vehicleId" + if (q.isNotEmpty()) "?${q.joinToString("&")}" else ""
        }
    }
    object AddPlan : Screen("add_plan/{vehicleId}?itemId={itemId}&planId={planId}") {
        fun createRoute(vehicleId: Long, itemId: Long? = null, planId: Long? = null): String {
            val q = mutableListOf<String>()
            if (itemId != null) q.add("itemId=$itemId")
            if (planId != null) q.add("planId=$planId")
            return "add_plan/$vehicleId" + if (q.isNotEmpty()) "?${q.joinToString("&")}" else ""
        }
    }
    object RecordsHistory : Screen("records_history/{vehicleId}?itemId={itemId}") {
        fun createRoute(vehicleId: Long, itemId: Long? = null): String {
            return "records_history/$vehicleId" + if (itemId != null) "?itemId=$itemId" else ""
        }
    }
    object ExpensesReport : Screen("expenses/{vehicleId}") {
        fun createRoute(vehicleId: Long) = "expenses/$vehicleId"
    }
    object ExportData : Screen("export/{vehicleId}") {
        fun createRoute(vehicleId: Long) = "export/$vehicleId"
    }
    object BulkRecord : Screen("bulk_record/{vehicleId}?itemId={itemId}") {
        fun createRoute(vehicleId: Long, itemId: Long? = null): String {
            return "bulk_record/$vehicleId" + if (itemId != null) "?itemId=$itemId" else ""
        }
    }
}
